package cs151.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Business rules for goals. Part of the subsystem hidden behind the {@link GoalPlanner} facade. All storage goes
 * through the {@link DAL} it was given (the strategy), so it works with any storage.
 *
 * <p>Every method either returns the saved/loaded object or throws a {@link ValidationException} (or subclass) whose
 * message can be shown to the user directly.
 */
public class GoalService {
    private final DAL dal;

    /** @precondition dal is not null */
    public GoalService(DAL dal) {
        this.dal = dal;
    }

    /**
     * Creates and saves a goal. A null status defaults to "Not Started". The creation date is automatic.
     *
     * @precondition name is not blank and unique, category and target date are given, the target date is today or
     *               later, progress is 0-100 (100 if Completed)
     * @postcondition the goal is stored and has its generated id
     * @throws ValidationException      if a precondition is not met
     * @throws DuplicateEntityException if the name is already used
     */
    public Goal createGoal(String name, String description, GoalCategory category, LocalDate targetDate,
                           GoalStatus status, int progress) {
        Goal goal = new Goal(name, description, category, targetDate, status == null ? GoalStatus.NOT_STARTED : status,
                progress);
        requireUniqueName(goal);
        requireTargetDateNotPast(goal.getTargetDate());
        dal.insertGoal(goal);
        return goal;
    }

    /** Creates a goal with status "Not Started" and progress 0. */
    public Goal createGoal(String name, String description, GoalCategory category, LocalDate targetDate) {
        return createGoal(name, description, category, targetDate, GoalStatus.NOT_STARTED, 0);
    }

    /**
     * Updates a goal. The "today or later" rule only applies if the target date is being changed, so an
     * existing goal whose date has passed can still be edited. The target date may not move before the
     * latest milestone target date.
     *
     * @throws NotFoundException if the goal does not exist
     */
    public Goal updateGoal(long goalId, String name, String description, GoalCategory category,
                           LocalDate targetDate, GoalStatus status, int progress) {
        return updateGoal(goalId, name, description, category, targetDate, status, progress,
                getGoal(goalId).getCreationDate());
    }

    /**
     * Updates a goal including its creation date (v0.8: the auto-generated date can be edited). The creation
     * date is required and cannot be in the future.
     *
     * @throws NotFoundException if the goal does not exist
     */
    public Goal updateGoal(long goalId, String name, String description, GoalCategory category,
                           LocalDate targetDate, GoalStatus status, int progress, LocalDate creationDate) {
        Goal goal = getGoal(goalId);
        LocalDate oldTargetDate = goal.getTargetDate();
        if (creationDate == null) throw new ValidationException("Creation date is required.");
        if (creationDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Creation date cannot be in the future.");
        }
        goal.setCreationDate(creationDate);
        goal.update(name, description, category, targetDate, status == null ? GoalStatus.NOT_STARTED : status,
                progress);
        requireUniqueName(goal);
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

    /**
     * Deletes the goal and, by cascade, its milestones, tasks and progress entries.
     *
     * @throws NotFoundException if the goal does not exist
     */
    public void deleteGoal(long goalId) {
        if (!dal.deleteGoal(goalId)) throw new NotFoundException("Goal not found.");
    }

    /** @throws NotFoundException if the goal does not exist */
    public Goal getGoal(long goalId) {
        return dal.findGoalById(goalId).orElseThrow(() -> new NotFoundException("Goal not found."));
    }

    /** @postcondition the goals are sorted by target date, then name */
    public List<Goal> getAllGoals() {
        return dal.getAllGoals();
    }

    /**
     * Sets a goal's progress (0-100); a Completed goal must remain at 100.
     *
     * @throws NotFoundException   if the goal does not exist
     * @throws ValidationException if the progress is not allowed
     */
    public Goal updateGoalProgress(long goalId, int progress) {
        Goal goal = getGoal(goalId);
        goal.updateProgress(progress);
        dal.updateGoal(goal);
        return goal;
    }

    /**
     * Sets status to Completed and progress to 100.
     *
     * @throws NotFoundException if the goal does not exist
     */
    public Goal markGoalCompleted(long goalId) {
        Goal goal = getGoal(goalId);
        goal.markCompleted();
        dal.updateGoal(goal);
        return goal;
    }

    private void requireUniqueName(Goal goal) {
        dal.findGoalByName(goal.getName()).ifPresent(existing -> {
            if (existing.getId() != goal.getId()) throw new DuplicateEntityException("Goal name already exists.");
        });
    }

    private static void requireTargetDateNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Target date must be today or later.");
    }
}
