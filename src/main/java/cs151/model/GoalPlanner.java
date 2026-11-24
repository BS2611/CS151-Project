package cs151.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main backend API for the frontend (the <em>Facade</em> of the model). Validates input, enforces the business rules
 * and delegates all storage to a {@link DAL} (the <em>Strategy</em>), so it works the same with
 * {@link DataManager} (SQLite) or {@link FileSystemDAL} (text file). Every method either returns the saved/loaded
 * object or throws a {@link ValidationException} (or subclass) whose message can be shown to the user directly.
 */
public class GoalPlanner {
    private final DAL dal;

    /** Uses SQLite (the default database file) for everything. */
    public GoalPlanner() {
        this(new DataManager());
    }

    /** Uses the given {@link DAL} for everything (goals, milestones, tasks, progress entries and search). */
    public GoalPlanner(DAL dal) {
        this.dal = dal;
    }

    // ------------------------------------------------------------------ goals
    /** Creates and saves a goal. A null/blank status defaults to "Not Started". Creation date is automatic. */
    public Goal createGoal(String name, String description, String category, LocalDate targetDate,
                           String status, int progress) {
        Goal goal = new Goal(name, description, category, targetDate, defaultIfBlank(status, Goal.STATUS_NOT_STARTED),
                progress);
        validateGoal(goal);
        requireTargetDateNotPast(goal.getTargetDate());
        dal.insertGoal(goal);
        return goal;
    }

    /** Creates a goal with status "Not Started" and progress 0. */
    public Goal createGoal(String name, String description, String category, LocalDate targetDate) {
        return createGoal(name, description, category, targetDate, Goal.STATUS_NOT_STARTED, 0);
    }

    /**
     * Updates a goal. The "today or later" rule only applies if the target date is being changed, so an
     * existing goal whose date has passed can still be edited. The target date may not move before the
     * latest milestone target date.
     */
    public Goal updateGoal(long goalId, String name, String description, String category, LocalDate targetDate,
                           String status, int progress) {
        return updateGoal(goalId, name, description, category, targetDate, status, progress,
                getGoal(goalId).getCreationDate());
    }

    /**
     * Updates a goal including its creation date (v0.8: the auto-generated date can be edited). The creation
     * date is required and cannot be in the future.
     */
    public Goal updateGoal(long goalId, String name, String description, String category, LocalDate targetDate,
                           String status, int progress, LocalDate creationDate) {
        Goal goal = getGoal(goalId);
        LocalDate oldTargetDate = goal.getTargetDate();
        if (creationDate == null) throw new ValidationException("Creation date is required.");
        if (creationDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Creation date cannot be in the future.");
        }
        goal.setCreationDate(creationDate);
        goal.setName(name);
        goal.setDescription(description);
        goal.setCategory(category);
        goal.setTargetDate(targetDate);
        goal.setStatus(defaultIfBlank(status, Goal.STATUS_NOT_STARTED));
        goal.setProgress(progress);
        validateGoal(goal);
        if (!goal.getTargetDate().equals(oldTargetDate)) {
            requireTargetDateNotPast(goal.getTargetDate());
        }
        for (Milestone m : dal.getMilestonesForGoal(goalId)) {
            if (m.getTargetDate().isAfter(goal.getTargetDate())) {
                throw new ValidationException("Goal target date cannot be earlier than a milestone target date.");
            }
        }
        dal.updateGoal(goal);
        return goal;
    }

    /**
     * Counts what deleting the goal will remove along with it (its milestones, their tasks and the progress
     * entries of those tasks), so the delete confirmation can warn the user.
     */
    public GoalDeletionImpact getDeletionImpact(long goalId) {
        int milestones = 0;
        int tasks = 0;
        int progresses = 0;
        for (Milestone m : dal.getMilestonesForGoal(goalId)) {
            milestones++;
            for (Task t : dal.getTasksForMilestone(m.getId())) {
                tasks++;
                progresses += dal.getProgressEntriesForTask(t.getId()).size();
            }
        }
        return new GoalDeletionImpact(milestones, tasks, progresses);
    }

    /** Deletes the goal and, by cascade, its milestones, tasks and progress entries. */
    public void deleteGoal(long goalId) {
        if (!dal.deleteGoal(goalId)) throw new NotFoundException("Goal not found.");
    }

    public Goal getGoal(long goalId) {
        return dal.findGoalById(goalId).orElseThrow(() -> new NotFoundException("Goal not found."));
    }

    public List<Goal> getAllGoals() {
        return dal.getAllGoals();
    }

    /** Sets a goal's progress (0-100); a Completed goal must remain at 100. */
    public Goal updateGoalProgress(long goalId, int progress) {
        Goal goal = getGoal(goalId);
        goal.updateProgress(progress);
        dal.updateGoal(goal);
        return goal;
    }

    /** Sets status to Completed and progress to 100. */
    public Goal markGoalCompleted(long goalId) {
        Goal goal = getGoal(goalId);
        goal.markCompleted();
        dal.updateGoal(goal);
        return goal;
    }

    private void validateGoal(Goal goal) {
        if (goal.getName().isEmpty()) throw new ValidationException("Goal name is required.");
        dal.findGoalByName(goal.getName()).ifPresent(existing -> {
            if (existing.getId() != goal.getId()) throw new DuplicateEntityException("Goal name already exists.");
        });
        if (goal.getCategory() == null || !Goal.CATEGORIES.contains(goal.getCategory())) {
            throw new ValidationException("Category must be one of: " + String.join(", ", Goal.CATEGORIES) + ".");
        }
        if (goal.getTargetDate() == null) throw new ValidationException("Target date is required.");
        if (!Goal.STATUSES.contains(goal.getStatus())) {
            throw new ValidationException("Status must be one of: " + String.join(", ", Goal.STATUSES) + ".");
        }
        if (goal.getProgress() < 0 || goal.getProgress() > 100) {
            throw new ValidationException("Progress must be between 0 and 100.");
        }
        if (Goal.STATUS_COMPLETED.equals(goal.getStatus()) && goal.getProgress() != 100) {
            throw new ValidationException("Completed goals must have 100% progress.");
        }
    }

    private void requireTargetDateNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Target date must be today or later.");
    }

    // ------------------------------------------------------------------ milestones

    /** Creates a milestone under a goal. A null/blank status defaults to "Not Started". */
    public Milestone createMilestone(long goalId, String name, LocalDate targetDate, String status) {
        Goal goal = getGoal(goalId);
        Milestone m = new Milestone(goalId, name, targetDate, defaultIfBlank(status, Milestone.STATUS_NOT_STARTED));
        validateMilestone(m, goal);
        dal.insertMilestone(m);
        return m;
    }

    /** Updates a milestone's name, target date and status; its goal cannot change. */
    public Milestone updateMilestone(long milestoneId, String name, LocalDate targetDate, String status) {
        Milestone m = getMilestone(milestoneId);
        m.setMilestoneName(name);
        m.setTargetDate(targetDate);
        m.setStatus(defaultIfBlank(status, Milestone.STATUS_NOT_STARTED));
        validateMilestone(m, getGoal(m.getGoalId()));
        for (Task t : dal.getTasksForMilestone(milestoneId)) {
            if (t.getDueDate().isAfter(m.getTargetDate())) {
                throw new ValidationException("Milestone target date cannot be earlier than a task due date.");
            }
        }
        dal.updateMilestone(m);
        return m;
    }

    /** Deletes the milestone and, by cascade, its tasks and their progress entries. */
    public void deleteMilestone(long milestoneId) {
        if (!dal.deleteMilestone(milestoneId)) throw new NotFoundException("Milestone not found.");
    }

    public Milestone getMilestone(long milestoneId) {
        return dal.findMilestoneById(milestoneId)
                .orElseThrow(() -> new NotFoundException("Milestone not found."));
    }

    public List<Milestone> getMilestonesForGoal(long goalId) {
        return dal.getMilestonesForGoal(goalId);
    }

    public Milestone markMilestoneCompleted(long milestoneId) {
        Milestone m = getMilestone(milestoneId);
        m.markCompleted();
        dal.updateMilestone(m);
        return m;
    }

    private void validateMilestone(Milestone m, Goal goal) {
        if (m.getMilestoneName().isEmpty()) throw new ValidationException("Milestone name is required.");
        for (Milestone other : dal.getMilestonesForGoal(goal.getId())) {
            if (other.getId() != m.getId() && other.getMilestoneName().equals(m.getMilestoneName())) {
                throw new DuplicateEntityException("Milestone name already exists for this goal.");
            }
        }
        if (m.getTargetDate() == null) throw new ValidationException("Milestone target date is required.");
        if (m.getTargetDate().isAfter(goal.getTargetDate())) {
            throw new ValidationException("Milestone target date cannot exceed Goal target date.");
        }
        if (!Milestone.STATUSES.contains(m.getStatus())) {
            throw new ValidationException("Milestone status must be one of: "
                    + String.join(", ", Milestone.STATUSES) + ".");
        }
    }

    // ------------------------------------------------------------------ tasks

    /** Creates a task under a milestone. Null/blank priority defaults to Medium, status to "To Do". */
    public Task createTask(long milestoneId, String description, LocalDate dueDate, String priority, String status) {
        Milestone milestone = getMilestone(milestoneId);
        Task t = new Task(milestoneId, description, dueDate, defaultIfBlank(priority, Task.DEFAULT_PRIORITY),
                defaultIfBlank(status, Task.STATUS_TO_DO));
        validateTask(t, milestone);
        dal.insertTask(t);
        return t;
    }

    /**
     * Updates a task. Moving status to Completed sets the completion date to today; moving away from
     * Completed clears it.
     */
    public Task updateTask(long taskId, String description, LocalDate dueDate, String priority, String status) {
        Task t = getTask(taskId);
        t.setDescription(description);
        t.setDueDate(dueDate);
        t.setPriority(defaultIfBlank(priority, Task.DEFAULT_PRIORITY));
        t.setStatus(defaultIfBlank(status, Task.STATUS_TO_DO));
        validateTask(t, getMilestone(t.getMilestoneId()));
        dal.updateTask(t);
        return t;
    }

    /** Deletes the task and, by cascade, its progress entries. */
    public void deleteTask(long taskId) {
        if (!dal.deleteTask(taskId)) throw new NotFoundException("Task not found.");
    }

    public Task getTask(long taskId) {
        return dal.findTaskById(taskId).orElseThrow(() -> new NotFoundException("Task not found."));
    }

    public List<Task> getTasksForMilestone(long milestoneId) {
        return dal.getTasksForMilestone(milestoneId);
    }

    public Task markTaskCompleted(long taskId) {
        Task t = getTask(taskId);
        t.markCompleted();
        dal.updateTask(t);
        return t;
    }

    private void validateTask(Task t, Milestone milestone) {
        if (t.getDescription().isEmpty()) throw new ValidationException("Task description is required.");
        for (Task other : dal.getTasksForMilestone(milestone.getId())) {
            if (other.getId() != t.getId() && other.getDescription().equals(t.getDescription())) {
                throw new DuplicateEntityException("Task description already exists for this milestone.");
            }
        }
        if (t.getDueDate() == null) throw new ValidationException("Task due date is required.");
        if (t.getDueDate().isAfter(milestone.getTargetDate())) {
            throw new ValidationException("Task due date cannot exceed Milestone target date.");
        }
        if (!Task.PRIORITIES.contains(t.getPriority())) {
            throw new ValidationException("Priority must be one of: " + String.join(", ", Task.PRIORITIES) + ".");
        }
        if (!Task.STATUSES.contains(t.getStatus())) {
            throw new ValidationException("Task status must be one of: " + String.join(", ", Task.STATUSES) + ".");
        }
    }

    // ------------------------------------------------------------------ progress entries

    /** Adds a note to a task. The entry date is always today and cannot be chosen by the caller. */
    public ProgressEntry addProgressEntry(long taskId, String note) {
        getTask(taskId);
        ProgressEntry entry = new ProgressEntry(taskId, note);
        requireNote(entry);
        dal.insertProgressEntry(entry);
        return entry;
    }

    /**
     * Adds a note with an explicit entry date. Only used for the sample data; the application pages use
     * {@link #addProgressEntry(long, String)}, where the date is always today.
     */
    public ProgressEntry addProgressEntry(long taskId, String note, LocalDate entryDate) {
        getTask(taskId);
        ProgressEntry entry = new ProgressEntry(0, taskId, entryDate, note);
        requireNote(entry);
        dal.insertProgressEntry(entry);
        return entry;
    }

    /** Changes the note of an entry; its date stays as it was. */
    public ProgressEntry updateProgressEntry(long entryId, String note) {
        ProgressEntry entry = dal.findProgressEntryById(entryId)
                .orElseThrow(() -> new NotFoundException("Progress entry not found."));
        entry.setNote(note);
        requireNote(entry);
        dal.updateProgressEntry(entry);
        return entry;
    }

    public void deleteProgressEntry(long entryId) {
        if (!dal.deleteProgressEntry(entryId)) throw new NotFoundException("Progress entry not found.");
    }

    public List<ProgressEntry> getProgressEntriesForTask(long taskId) {
        return dal.getProgressEntriesForTask(taskId);
    }

    private void requireNote(ProgressEntry entry) {
        if (entry.getNote().isEmpty()) throw new ValidationException("Progress note is required.");
    }

    // ------------------------------------------------------------------ search

    /**
     * Global, case-insensitive search. Goals match on name, description, category, status, dates or progress;
     * milestones on name, target date or status; tasks on description, due date, priority or status. A blank query returns no results.
     */
    public SearchResults search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResults(List.of(), List.of(), List.of());
        }

        return new SearchResults(
                dal.searchGoals(query),
                dal.searchMilestones(query),
                dal.searchTasks(query)
        );
    }

    /**
     * Returns every Goal related to the search text.
     * A Goal is returned when the text matches the Goal itself,
     * one of its Milestones, or one of its Tasks.
     * Each Goal appears only once.
     */
    public List<Goal> searchRelatedGoals(String query) {
        SearchResults results = search(query);

        Map<Long, Goal> uniqueGoals = new LinkedHashMap<>();

        for (Goal goal : results.goals()) {
            uniqueGoals.put(goal.getId(), goal);
        }

        for (Milestone milestone : results.milestones()) {
            Goal goal = getGoal(milestone.getGoalId());
            uniqueGoals.put(goal.getId(), goal);
        }

        for (Task task : results.tasks()) {
            Milestone milestone = getMilestone(task.getMilestoneId());
            Goal goal = getGoal(milestone.getGoalId());
            uniqueGoals.put(goal.getId(), goal);
        }

        List<Goal> goals = new ArrayList<>(uniqueGoals.values());

        goals.sort(
                Comparator.comparing(Goal::getTargetDate)
                        .thenComparing(
                                Goal::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return goals;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
