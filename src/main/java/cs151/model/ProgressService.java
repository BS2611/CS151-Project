package cs151.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Business rules for progress entries. Part of the subsystem hidden behind the {@link GoalPlanner} facade; storage
 * goes through the {@link DAL} it was given.
 */
public class ProgressService {
    private final DAL dal;
    private final TaskService tasks;

    /** @precondition neither argument is null */
    public ProgressService(DAL dal, TaskService tasks) {
        this.dal = dal;
        this.tasks = tasks;
    }

    /**
     * Adds a note to a task. The entry date is always today and cannot be chosen by the caller.
     *
     * @precondition the task exists and the note is not blank
     * @postcondition the entry is stored and has its generated id
     * @throws NotFoundException   if the task does not exist
     * @throws ValidationException if the note is blank
     */
    public ProgressEntry addProgressEntry(long taskId, String note) {
        tasks.getTask(taskId);
        ProgressEntry entry = new ProgressEntry(taskId, note);
        dal.insertProgressEntry(entry);
        return entry;
    }

    /**
     * Adds a note with an explicit entry date. Only used for the sample data; the application pages use
     * {@link #addProgressEntry(long, String)}, where the date is always today.
     */
    public ProgressEntry addProgressEntry(long taskId, String note, LocalDate entryDate) {
        tasks.getTask(taskId);
        ProgressEntry entry = new ProgressEntry(0, taskId, entryDate, note);
        dal.insertProgressEntry(entry);
        return entry;
    }

    /**
     * Changes the note of an entry; its date stays as it was.
     *
     * @throws NotFoundException   if the entry does not exist
     * @throws ValidationException if the note is blank
     */
    public ProgressEntry updateProgressEntry(long entryId, String note) {
        ProgressEntry entry = dal.findProgressEntryById(entryId)
                .orElseThrow(() -> new NotFoundException("Progress entry not found."));
        entry.setNote(note);
        dal.updateProgressEntry(entry);
        return entry;
    }

    /** @throws NotFoundException if the entry does not exist */
    public void deleteProgressEntry(long entryId) {
        if (!dal.deleteProgressEntry(entryId)) throw new NotFoundException("Progress entry not found.");
    }

    /** @postcondition the entries are sorted by date, oldest first */
    public List<ProgressEntry> getProgressEntriesForTask(long taskId) {
        return dal.getProgressEntriesForTask(taskId);
    }
}
