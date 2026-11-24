package cs151.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A task that belongs to exactly one Milestone.
 *
 * <p>Class invariants: the description is not empty; due date, priority and status are not null; the completion
 * date is set when the task becomes Completed (to today's date) and cleared when it moves away from Completed.
 * An operation that would break an invariant throws a {@link ValidationException} and leaves the task unchanged.
 */
public class Task {
    private long id;
    private final long milestoneId;
    private String description;
    private LocalDate dueDate;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDate completionDate;

    /**
     * Creates a new task. If the status is Completed the completion date is set to today.
     *
     * @precondition the values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken
     */
    public Task(long milestoneId, String description, LocalDate dueDate, TaskPriority priority, TaskStatus status) {
        this(0, milestoneId, description, dueDate, priority, status,
                status == TaskStatus.COMPLETED ? LocalDate.now() : null);
    }

    /**
     * Full constructor used when loading a stored task.
     *
     * @precondition the values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken
     */
    public Task(long id, long milestoneId, String description, LocalDate dueDate, TaskPriority priority,
                TaskStatus status, LocalDate completionDate) {
        check(description, dueDate, priority, status);
        this.id = id;
        this.milestoneId = milestoneId;
        this.description = description.trim();
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.completionDate = completionDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMilestoneId() { return milestoneId; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getCompletionDate() { return completionDate; }

    /**
     * Changes everything the user can edit in one step and keeps the completion date consistent: moving to
     * Completed sets it to today (unless already Completed), moving away from Completed clears it. The milestone of
     * a task never changes.
     *
     * @precondition the new values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken; the task is then unchanged
     */
    public void update(String description, LocalDate dueDate, TaskPriority priority, TaskStatus status) {
        check(description, dueDate, priority, status);
        boolean wasCompleted = this.status == TaskStatus.COMPLETED;
        boolean nowCompleted = status == TaskStatus.COMPLETED;
        if (nowCompleted && !wasCompleted) {
            completionDate = LocalDate.now();
        } else if (!nowCompleted) {
            completionDate = null;
        }
        this.description = description.trim();
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
    }

    /** @postcondition status is Completed and the completion date is set */
    public void markCompleted() {
        update(description, dueDate, priority, TaskStatus.COMPLETED);
    }

    private static void check(String description, LocalDate dueDate, TaskPriority priority, TaskStatus status) {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Task description is required.");
        }
        if (dueDate == null) throw new ValidationException("Task due date is required.");
        if (priority == null) throw new ValidationException("Priority is required.");
        if (status == null) throw new ValidationException("Task status is required.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task t)) return false;
        return id != 0 ? id == t.id : t.id == 0 && milestoneId == t.milestoneId && description.equals(t.description);
    }

    @Override
    public int hashCode() { return id != 0 ? Long.hashCode(id) : Objects.hash(milestoneId, description); }

    @Override
    public String toString() {
        return "Task{id=" + id + ", milestoneId=" + milestoneId + ", description='" + description + "', dueDate="
                + dueDate + ", priority=" + priority + ", status=" + status + ", completionDate=" + completionDate + "}";
    }
}
