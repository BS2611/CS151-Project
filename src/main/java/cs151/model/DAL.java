package cs151.model;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer: the <em>strategy</em> interface for storing goals, milestones, tasks and progress entries.
 * {@link GoalPlanner} and {@link ReportGenerator} depend only on this interface, so the storage (SQLite in
 * {@link DataManager}, a text file in {@link FileSystemDAL}) can be replaced without changing the business rules.
 *
 * <p>Every implementation follows the same rules:
 * <ul>
 *   <li>an insert sets the generated id on the object and returns it;</li>
 *   <li>a goal name is unique, a milestone name is unique within its goal and a task description is unique
 *       within its milestone ({@link DuplicateEntityException});</li>
 *   <li>inserting a milestone, task or progress entry whose parent does not exist throws
 *       {@link NotFoundException};</li>
 *   <li>deleting a goal also deletes its milestones, tasks and progress entries, deleting a milestone also deletes
 *       its tasks and their progress entries, deleting a task also deletes its progress entries;</li>
 *   <li>update and delete return whether the record existed;</li>
 *   <li>lists are sorted by target/due date and then name/description (progress entries by date, then id).</li>
 * </ul>
 */
public interface DAL {
    // ------------------------------------------------------------------ goals

    /** Stores a new goal, sets its generated id and returns the id. */
    long insertGoal(Goal goal);

    /** Returns all goals, soonest target date first (then by name). */
    List<Goal> getAllGoals();

    /** Saves the changes of an existing goal; returns whether a goal was updated. */
    boolean updateGoal(Goal goal);

    /** Deletes the goal (and its milestones, tasks and progress entries); returns whether it existed. */
    boolean deleteGoal(long goalId);

    Optional<Goal> findGoalById(long goalId);

    /** Finds a goal by its exact (case-sensitive) name, ignoring leading and trailing spaces. */
    Optional<Goal> findGoalByName(String name);

    // ------------------------------------------------------------------ milestones

    long insertMilestone(Milestone milestone);

    boolean updateMilestone(Milestone milestone);

    /** Deletes the milestone (and its tasks and their progress entries); returns whether it existed. */
    boolean deleteMilestone(long milestoneId);

    Optional<Milestone> findMilestoneById(long milestoneId);

    List<Milestone> getMilestonesForGoal(long goalId);

    // ------------------------------------------------------------------ tasks

    long insertTask(Task task);

    boolean updateTask(Task task);

    /** Deletes the task (and its progress entries); returns whether it existed. */
    boolean deleteTask(long taskId);

    Optional<Task> findTaskById(long taskId);

    List<Task> getTasksForMilestone(long milestoneId);

    // ------------------------------------------------------------------ progress entries

    long insertProgressEntry(ProgressEntry entry);

    /** Only the note can change; the entry date is fixed at creation. */
    boolean updateProgressEntry(ProgressEntry entry);

    boolean deleteProgressEntry(long entryId);

    Optional<ProgressEntry> findProgressEntryById(long entryId);

    List<ProgressEntry> getProgressEntriesForTask(long taskId);

    // ------------------------------------------------------------------ search (case-insensitive "contains")

    /** Goals whose name, description, category, status, target date, creation date or progress contains the text. */
    List<Goal> searchGoals(String query);

    /** Milestones whose name, target date or status contains the text. */
    List<Milestone> searchMilestones(String query);

    /** Tasks whose description, due date, priority or status contains the text. */
    List<Task> searchTasks(String query);
}
