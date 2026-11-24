package cs151.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Business rules for tasks. Part of the subsystem hidden behind the {@link GoalPlanner} facade; storage goes
 * through the {@link DAL} it was given.
 */
public class TaskService {
    private final DAL dal;
    private final MilestoneService milestones;

    /** @precondition neither argument is null */
    public TaskService(DAL dal, MilestoneService milestones) {
        this.dal = dal;
        this.milestones = milestones;
    }

    /**
     * Creates a task under a milestone. A null priority defaults to Medium and a null status to "To Do".
     *
     * @precondition the milestone exists; the description is not blank and unique within the milestone; the due date
     *               is given and not after the milestone's target date
     * @postcondition the task is stored and has its generated id
     * @throws NotFoundException        if the milestone does not exist
     * @throws ValidationException      if a precondition is not met
     * @throws DuplicateEntityException if the description is already used in this milestone
     */
    public Task createTask(long milestoneId, String description, LocalDate dueDate, TaskPriority priority,
                           TaskStatus status) {
        Milestone milestone = milestones.getMilestone(milestoneId);
        Task t = new Task(milestoneId, description, dueDate, priority == null ? TaskPriority.MEDIUM : priority,
                status == null ? TaskStatus.TO_DO : status);
        validate(t, milestone);
        dal.insertTask(t);
        return t;
    }

    /**
     * Updates a task. Moving status to Completed sets the completion date to today; moving away from
     * Completed clears it.
     *
     * @throws NotFoundException if the task does not exist
     */
    public Task updateTask(long taskId, String description, LocalDate dueDate, TaskPriority priority,
                           TaskStatus status) {
        Task t = getTask(taskId);
        t.update(description, dueDate, priority == null ? TaskPriority.MEDIUM : priority,
                status == null ? TaskStatus.TO_DO : status);
        validate(t, milestones.getMilestone(t.getMilestoneId()));
        dal.updateTask(t);
        return t;
    }

    /**
     * Deletes the task and, by cascade, its progress entries.
     *
     * @throws NotFoundException if the task does not exist
     */
    public void deleteTask(long taskId) {
        if (!dal.deleteTask(taskId)) throw new NotFoundException("Task not found.");
    }

    /** @throws NotFoundException if the task does not exist */
    public Task getTask(long taskId) {
        return dal.findTaskById(taskId).orElseThrow(() -> new NotFoundException("Task not found."));
    }

    /** @postcondition the tasks are sorted by due date, then description */
    public List<Task> getTasksForMilestone(long milestoneId) {
        return dal.getTasksForMilestone(milestoneId);
    }

    /** @throws NotFoundException if the task does not exist */
    public Task markTaskCompleted(long taskId) {
        Task t = getTask(taskId);
        t.markCompleted();
        dal.updateTask(t);
        return t;
    }

    private void validate(Task t, Milestone milestone) {
        for (Task other : dal.getTasksForMilestone(milestone.getId())) {
            if (other.getId() != t.getId() && other.getDescription().equals(t.getDescription())) {
                throw new DuplicateEntityException("Task description already exists for this milestone.");
            }
        }
        if (t.getDueDate().isAfter(milestone.getTargetDate())) {
            throw new ValidationException("Task due date cannot exceed Milestone target date.");
        }
    }
}
