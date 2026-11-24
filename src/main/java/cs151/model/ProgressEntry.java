package cs151.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A dated note about work done on a Task (work completed, obstacles or next steps).
 *
 * <p>Class invariant: the note is not empty.
 */
public class ProgressEntry {
    private long id;
    private final long taskId;
    private final LocalDate entryDate;
    private String note;

    /** Creates a new entry; the entry date is set to today automatically. */
    public ProgressEntry(long taskId, String note) {
        this(0, taskId, LocalDate.now(), note);
    }

    /** Full constructor used when loading a stored entry. */
    public ProgressEntry(long id, long taskId, LocalDate entryDate, String note) {
        this.id = id;
        this.taskId = taskId;
        this.entryDate = entryDate;
        setNote(note);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTaskId() { return taskId; }
    public LocalDate getEntryDate() { return entryDate; }

    public String getNote() { return note; }
    /**
     * @precondition note is not blank
     * @throws ValidationException if the note is blank; the entry is then unchanged
     */
    public void setNote(String note) {
        String clean = note == null ? "" : note.trim();
        if (clean.isEmpty()) throw new ValidationException("Progress note is required.");
        this.note = clean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgressEntry p)) return false;
        return id != 0 && id == p.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "ProgressEntry{id=" + id + ", taskId=" + taskId + ", entryDate=" + entryDate + ", note='" + note + "'}";
    }
}
