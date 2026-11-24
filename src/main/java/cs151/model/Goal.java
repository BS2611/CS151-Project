package cs151.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A personal goal. The id is an internal persistence detail (0 until the goal is saved).
 *
 * <p>Class invariants (always true, right after construction and after every operation): the name is not empty;
 * category, target date, status and creation date are not null; progress is between 0 and 100; a Completed goal has
 * progress 100. An operation that would break an invariant throws a {@link ValidationException} and leaves the goal
 * unchanged.
 */
public class Goal {
    private long id;
    private String name;
    private String description;
    private GoalCategory category;
    private LocalDate targetDate;
    private GoalStatus status;
    private int progress;
    private LocalDate creationDate;

    /**
     * Creates a new, unsaved goal; the creation date is set to today.
     *
     * @precondition the values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken
     */
    public Goal(String name, String description, GoalCategory category, LocalDate targetDate, GoalStatus status,
                int progress) {
        this(0, name, description, category, targetDate, status, progress, LocalDate.now());
    }

    /**
     * Full constructor used when loading a stored goal.
     *
     * @precondition the values satisfy the class invariants
     * @throws ValidationException if a class invariant would be broken
     */
    public Goal(long id, String name, String description, GoalCategory category, LocalDate targetDate,
                GoalStatus status, int progress, LocalDate creationDate) {
        this.id = id;
        apply(name, description, category, targetDate, status, progress);
        setCreationDate(creationDate);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public GoalCategory getCategory() { return category; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }
    public int getProgress() { return progress; }
    public LocalDate getCreationDate() { return creationDate; }

    /**
     * Changes everything the user can edit in one step, so the goal is never half-updated.
     *
     * @precondition the new values satisfy the class invariants
     * @postcondition the goal holds the new values (the name and description are trimmed)
     * @throws ValidationException if a class invariant would be broken; the goal is then unchanged
     */
    public void update(String name, String description, GoalCategory category, LocalDate targetDate,
                       GoalStatus status, int progress) {
        apply(name, description, category, targetDate, status, progress);
    }

    /**
     * Changes the creation date (v0.8: the Edit Goal page lets the user edit it).
     *
     * @precondition creationDate is not null
     * @throws ValidationException if creationDate is null
     */
    public void setCreationDate(LocalDate creationDate) {
        if (creationDate == null) {
            throw new ValidationException("Creation date is required.");
        }
        this.creationDate = creationDate;
    }

    /**
     * Sets progress (0-100). A Completed goal must stay at 100.
     *
     * @precondition 0 &lt;= progress &lt;= 100, and progress is 100 if the goal is Completed
     * @throws ValidationException if the precondition is not met; the goal is then unchanged
     */
    public void updateProgress(int progress) {
        requireProgress(status, progress);
        this.progress = progress;
    }

    /**
     * Sets status to Completed and progress to 100.
     *
     * @postcondition status is Completed and progress is 100
     */
    public void markCompleted() {
        this.status = GoalStatus.COMPLETED;
        this.progress = 100;
    }

    private void apply(String name, String description, GoalCategory category, LocalDate targetDate,
                       GoalStatus status, int progress) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) throw new ValidationException("Goal name is required.");
        if (category == null) throw new ValidationException("Category is required.");
        if (targetDate == null) throw new ValidationException("Target date is required.");
        if (status == null) throw new ValidationException("Status is required.");
        requireProgress(status, progress);
        this.name = cleanName;
        this.description = description == null ? "" : description.trim();
        this.category = category;
        this.targetDate = targetDate;
        this.status = status;
        this.progress = progress;
    }

    private static void requireProgress(GoalStatus status, int progress) {
        if (progress < 0 || progress > 100) {
            throw new ValidationException("Progress must be between 0 and 100.");
        }
        if (status == GoalStatus.COMPLETED && progress != 100) {
            throw new ValidationException("Completed goals must have 100% progress.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Goal g)) return false;
        return id != 0 ? id == g.id : g.id == 0 && name.equals(g.name);
    }

    @Override
    public int hashCode() { return id != 0 ? Long.hashCode(id) : Objects.hash(name); }

    @Override
    public String toString() {
        return "Goal{id=" + id + ", name='" + name + "', category=" + category + ", targetDate=" + targetDate
                + ", status=" + status + ", progress=" + progress + ", creationDate=" + creationDate + "}";
    }
}
