package cs151.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A milestone that belongs to exactly one Goal.
 *
 * <p>Class invariants: the name is not empty; target date and status are not null. An operation that would break
 * an invariant throws a {@link ValidationException} and leaves the milestone unchanged.
 */
public class Milestone {
    private long id;
    private final long goalId;
    private String milestoneName;
    private LocalDate targetDate;
    private MilestoneStatus status;

    /**
     * @precondition the values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken
     */
    public Milestone(long goalId, String milestoneName, LocalDate targetDate, MilestoneStatus status) {
        this(0, goalId, milestoneName, targetDate, status);
    }

    /**
     * Full constructor used when loading a stored milestone.
     *
     * @precondition the values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken
     */
    public Milestone(long id, long goalId, String milestoneName, LocalDate targetDate, MilestoneStatus status) {
        this.id = id;
        this.goalId = goalId;
        apply(milestoneName, targetDate, status);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getGoalId() { return goalId; }
    public String getMilestoneName() { return milestoneName; }
    public LocalDate getTargetDate() { return targetDate; }
    public MilestoneStatus getStatus() { return status; }

    /**
     * Changes everything the user can edit in one step; the goal of a milestone never changes.
     *
     * @precondition the new values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken; the milestone is then unchanged
     */
    public void update(String milestoneName, LocalDate targetDate, MilestoneStatus status) {
        apply(milestoneName, targetDate, status);
    }

    /** @postcondition status is Completed */
    public void markCompleted() { this.status = MilestoneStatus.COMPLETED; }

    private void apply(String name, LocalDate targetDate, MilestoneStatus status) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) throw new ValidationException("Milestone name is required.");
        if (targetDate == null) throw new ValidationException("Milestone target date is required.");
        if (status == null) throw new ValidationException("Milestone status is required.");
        this.milestoneName = cleanName;
        this.targetDate = targetDate;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Milestone m)) return false;
        return id != 0 ? id == m.id : m.id == 0 && goalId == m.goalId && milestoneName.equals(m.milestoneName);
    }

    @Override
    public int hashCode() { return id != 0 ? Long.hashCode(id) : Objects.hash(goalId, milestoneName); }

    @Override
    public String toString() {
        return "Milestone{id=" + id + ", goalId=" + goalId + ", name='" + milestoneName + "', targetDate="
                + targetDate + ", status=" + status + "}";
    }
}
