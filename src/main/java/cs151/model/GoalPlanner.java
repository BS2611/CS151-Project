package cs151.model;

import java.time.LocalDate;
import java.util.List;

/**
 * The single entry point of the model for the controllers (the <em>Facade</em>). It exposes only the operations the
 * controllers need and hides the classes behind it: {@link GoalService}, {@link MilestoneService},
 * {@link TaskService}, {@link ProgressService} and {@link SearchService}. These services apply the business rules and
 * store everything through one {@link DAL} (the <em>Strategy</em>), so the storage can be replaced without touching
 * the controllers or the rules.
 *
 * <p>Every method either returns the saved/loaded object or throws a {@link ValidationException} (or subclass) whose
 * message can be shown to the user directly. See the service classes for the precondition and postcondition of each
 * operation.
 */
public class GoalPlanner {
    private final GoalService goalService;
    private final MilestoneService milestoneService;
    private final TaskService taskService;
    private final ProgressService progressService;
    private final SearchService searchService;

    /**
     * Builds the services on top of the given storage.
     *
     * @precondition dal is not null
     */
    public GoalPlanner(DAL dal) {
        this.goalService = new GoalService(dal);
        this.milestoneService = new MilestoneService(dal, goalService);
        this.taskService = new TaskService(dal, milestoneService);
        this.progressService = new ProgressService(dal, taskService);
        this.searchService = new SearchService(dal, goalService, milestoneService);
    }

    // ------------------------------------------------------------------ goals

    public Goal createGoal(String name, String description, GoalCategory category, LocalDate targetDate,
                           GoalStatus status, int progress) {
        return goalService.createGoal(name, description, category, targetDate, status, progress);
    }

    public Goal createGoal(String name, String description, GoalCategory category, LocalDate targetDate) {
        return goalService.createGoal(name, description, category, targetDate);
    }

    public Goal updateGoal(long goalId, String name, String description, GoalCategory category,
                           LocalDate targetDate, GoalStatus status, int progress) {
        return goalService.updateGoal(goalId, name, description, category, targetDate, status, progress);
    }

    public Goal updateGoal(long goalId, String name, String description, GoalCategory category,
                           LocalDate targetDate, GoalStatus status, int progress, LocalDate creationDate) {
        return goalService.updateGoal(goalId, name, description, category, targetDate, status, progress,
                creationDate);
    }

    public GoalDeletionImpact getDeletionImpact(long goalId) {
        return goalService.getDeletionImpact(goalId);
    }

    public void deleteGoal(long goalId) {
        goalService.deleteGoal(goalId);
    }

    public Goal getGoal(long goalId) {
        return goalService.getGoal(goalId);
    }

    public List<Goal> getAllGoals() {
        return goalService.getAllGoals();
    }

    public Goal updateGoalProgress(long goalId, int progress) {
        return goalService.updateGoalProgress(goalId, progress);
    }

    public Goal markGoalCompleted(long goalId) {
        return goalService.markGoalCompleted(goalId);
    }

    // ------------------------------------------------------------------ milestones

    public Milestone createMilestone(long goalId, String name, LocalDate targetDate, MilestoneStatus status) {
        return milestoneService.createMilestone(goalId, name, targetDate, status);
    }

    public Milestone updateMilestone(long milestoneId, String name, LocalDate targetDate, MilestoneStatus status) {
        return milestoneService.updateMilestone(milestoneId, name, targetDate, status);
    }

    public void deleteMilestone(long milestoneId) {
        milestoneService.deleteMilestone(milestoneId);
    }

    public Milestone getMilestone(long milestoneId) {
        return milestoneService.getMilestone(milestoneId);
    }

    public List<Milestone> getMilestonesForGoal(long goalId) {
        return milestoneService.getMilestonesForGoal(goalId);
    }

    public Milestone markMilestoneCompleted(long milestoneId) {
        return milestoneService.markMilestoneCompleted(milestoneId);
    }

    // ------------------------------------------------------------------ tasks

    public Task createTask(long milestoneId, String description, LocalDate dueDate, TaskPriority priority,
                           TaskStatus status) {
        return taskService.createTask(milestoneId, description, dueDate, priority, status);
    }

    public Task updateTask(long taskId, String description, LocalDate dueDate, TaskPriority priority,
                           TaskStatus status) {
        return taskService.updateTask(taskId, description, dueDate, priority, status);
    }

    public void deleteTask(long taskId) {
        taskService.deleteTask(taskId);
    }

    public Task getTask(long taskId) {
        return taskService.getTask(taskId);
    }

    public List<Task> getTasksForMilestone(long milestoneId) {
        return taskService.getTasksForMilestone(milestoneId);
    }

    public Task markTaskCompleted(long taskId) {
        return taskService.markTaskCompleted(taskId);
    }

    // ------------------------------------------------------------------ progress entries

    public ProgressEntry addProgressEntry(long taskId, String note) {
        return progressService.addProgressEntry(taskId, note);
    }

    public ProgressEntry addProgressEntry(long taskId, String note, LocalDate entryDate) {
        return progressService.addProgressEntry(taskId, note, entryDate);
    }

    public ProgressEntry updateProgressEntry(long entryId, String note) {
        return progressService.updateProgressEntry(entryId, note);
    }

    public void deleteProgressEntry(long entryId) {
        progressService.deleteProgressEntry(entryId);
    }

    public List<ProgressEntry> getProgressEntriesForTask(long taskId) {
        return progressService.getProgressEntriesForTask(taskId);
    }

    // ------------------------------------------------------------------ search

    public SearchResults search(String query) {
        return searchService.search(query);
    }

    public List<Goal> searchRelatedGoals(String query) {
        return searchService.searchRelatedGoals(query);
    }
}
