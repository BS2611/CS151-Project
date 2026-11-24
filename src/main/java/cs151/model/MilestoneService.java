package cs151.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Business rules for milestones. Part of the subsystem hidden behind the {@link GoalPlanner} facade; storage goes
 * through the {@link DAL} it was given.
 */
public class MilestoneService {
    private final DAL dal;
    private final GoalService goals;

    /** @precondition neither argument is null */
    public MilestoneService(DAL dal, GoalService goals) {
        this.dal = dal;
        this.goals = goals;
    }

    /**
     * Creates a milestone under a goal. A null status defaults to "Not Started".
     *
     * @precondition the goal exists; the name is not blank and unique within the goal; the target date is given and
     *               not after the goal's target date
     * @postcondition the milestone is stored and has its generated id
     * @throws NotFoundException        if the goal does not exist
     * @throws ValidationException      if a precondition is not met
     * @throws DuplicateEntityException if the name is already used in this goal
     */
    public Milestone createMilestone(long goalId, String name, LocalDate targetDate, MilestoneStatus status) {
        Goal goal = goals.getGoal(goalId);
        Milestone m = new Milestone(goalId, name, targetDate, status == null ? MilestoneStatus.NOT_STARTED : status);
        validate(m, goal);
        dal.insertMilestone(m);
        return m;
    }

    /**
     * Updates a milestone's name, target date and status; its goal cannot change. The target date may not move
     * before the latest task due date.
     *
     * @throws NotFoundException if the milestone does not exist
     */
    public Milestone updateMilestone(long milestoneId, String name, LocalDate targetDate, MilestoneStatus status) {
        Milestone m = getMilestone(milestoneId);
        m.update(name, targetDate, status == null ? MilestoneStatus.NOT_STARTED : status);
        validate(m, goals.getGoal(m.getGoalId()));
        for (Task t : dal.getTasksForMilestone(milestoneId)) {
            if (t.getDueDate().isAfter(m.getTargetDate())) {
                throw new ValidationException("Milestone target date cannot be earlier than a task due date.");
            }
        }
        dal.updateMilestone(m);
        return m;
    }

    /**
     * Deletes the milestone and, by cascade, its tasks and their progress entries.
     *
     * @throws NotFoundException if the milestone does not exist
     */
    public void deleteMilestone(long milestoneId) {
        if (!dal.deleteMilestone(milestoneId)) throw new NotFoundException("Milestone not found.");
    }

    /** @throws NotFoundException if the milestone does not exist */
    public Milestone getMilestone(long milestoneId) {
        return dal.findMilestoneById(milestoneId).orElseThrow(() -> new NotFoundException("Milestone not found."));
    }

    /** @postcondition the milestones are sorted by target date, then name */
    public List<Milestone> getMilestonesForGoal(long goalId) {
        return dal.getMilestonesForGoal(goalId);
    }

    /** @throws NotFoundException if the milestone does not exist */
    public Milestone markMilestoneCompleted(long milestoneId) {
        Milestone m = getMilestone(milestoneId);
        m.markCompleted();
        dal.updateMilestone(m);
        return m;
    }

    private void validate(Milestone m, Goal goal) {
        for (Milestone other : dal.getMilestonesForGoal(goal.getId())) {
            if (other.getId() != m.getId() && other.getMilestoneName().equals(m.getMilestoneName())) {
                throw new DuplicateEntityException("Milestone name already exists for this goal.");
            }
        }
        if (m.getTargetDate().isAfter(goal.getTargetDate())) {
            throw new ValidationException("Milestone target date cannot exceed Goal target date.");
        }
    }
}
