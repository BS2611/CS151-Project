package cs151.backend;

import cs151.backend.exception.DuplicateEntityException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MilestoneTaskProgressTest extends BackendTestBase {

    // ---------------- milestones

    @Test
    void validMilestoneCreated() {
        Goal g = newGoal("G");
        Milestone m = planner.createMilestone(g.getId(), "  M1 ", today.plusDays(5), null);
        assertEquals("M1", planner.getMilestone(m.getId()).getMilestoneName());
        assertEquals("Not Started", m.getStatus());
        assertEquals(1, planner.getMilestonesForGoal(g.getId()).size());
    }

    @Test
    void blankMilestoneNameRejected() {
        Goal g = newGoal("G");
        assertThrows(ValidationException.class, () -> planner.createMilestone(g.getId(), " ", today, null));
    }

    @Test
    void duplicateMilestoneNameInSameGoalRejected() {
        Goal g = newGoal("G");
        newMilestone(g, "M");
        assertThrows(DuplicateEntityException.class, () -> planner.createMilestone(g.getId(), " M", today, null));
    }

    @Test
    void sameMilestoneNameInDifferentGoalsAllowed() {
        newMilestone(newGoal("G1"), "M");
        assertDoesNotThrow(() -> newMilestone(newGoal("G2"), "M"));
    }

    /** v0.6: a goal's milestones are listed with the soonest target date on top. */
    @Test
    void milestonesListedSoonestTargetDateFirst() {
        Goal g = newGoal("G");
        planner.createMilestone(g.getId(), "Early", today.plusDays(5), null);
        planner.createMilestone(g.getId(), "Late", today.plusDays(20), null);
        planner.createMilestone(g.getId(), "Middle", today.plusDays(10), null);

        List<String> names = planner.getMilestonesForGoal(g.getId()).stream()
                .map(Milestone::getMilestoneName).toList();
        assertEquals(List.of("Early", "Middle", "Late"), names);
    }

    /** v0.5: a saved milestone is linked to its goal and only listed under that goal. */
    @Test
    void milestoneIsOnlyListedUnderItsOwnGoal() {
        Goal a = newGoal("A");
        Goal b = newGoal("B");
        Milestone m = newMilestone(a, "M");

        assertEquals(a.getId(), m.getGoalId());
        assertEquals(1, planner.getMilestonesForGoal(a.getId()).size());
        assertTrue(planner.getMilestonesForGoal(b.getId()).isEmpty());
    }

    @Test
    void milestoneAfterGoalTargetDateRejected() {
        Goal g = newGoal("G");
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createMilestone(g.getId(), "Late", g.getTargetDate().plusDays(1), null));
        assertEquals("Milestone target date cannot exceed Goal target date.", e.getMessage());
    }

    @Test
    void milestoneForMissingGoalRejected() {
        assertThrows(ValidationException.class, () -> planner.createMilestone(4242, "M", today, null));
    }

    @Test
    void markMilestoneCompleted() {
        Milestone m = newMilestone(newGoal("G"), "M");
        assertEquals("Completed", planner.markMilestoneCompleted(m.getId()).getStatus());
        assertEquals("Completed", planner.getMilestone(m.getId()).getStatus());
    }

    // ---------------- tasks

    @Test
    void validTaskCreatedWithDefaults() {
        Milestone m = newMilestone(newGoal("G"), "M");
        Task t = planner.createTask(m.getId(), "  Write code ", today.plusDays(2), null, null);
        Task loaded = planner.getTask(t.getId());
        assertEquals("Write code", loaded.getDescription());
        assertEquals("Medium", loaded.getPriority());
        assertEquals("To Do", loaded.getStatus());
        assertNull(loaded.getCompletionDate());
    }

    @Test
    void blankTaskDescriptionRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        assertThrows(ValidationException.class, () -> planner.createTask(m.getId(), "  ", today, null, null));
    }

    @Test
    void duplicateTaskInSameMilestoneRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        newTask(m, "T");
        assertThrows(DuplicateEntityException.class, () -> planner.createTask(m.getId(), "T", today, null, null));
    }

    @Test
    void taskDueAfterMilestoneRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createTask(m.getId(), "T", m.getTargetDate().plusDays(1), null, null));
        assertEquals("Task due date cannot exceed Milestone target date.", e.getMessage());
    }

    @Test
    void markingCompletedSetsCompletionDate() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        Task done = planner.markTaskCompleted(t.getId());
        assertEquals(today, done.getCompletionDate());
        assertEquals(today, planner.getTask(t.getId()).getCompletionDate());
    }

    @Test
    void leavingCompletedClearsCompletionDate() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        planner.markTaskCompleted(t.getId());
        Task reopened = planner.updateTask(t.getId(), "T", t.getDueDate(), "High", "In Progress");
        assertNull(reopened.getCompletionDate());
        assertNull(planner.getTask(t.getId()).getCompletionDate());
        assertEquals("High", planner.getTask(t.getId()).getPriority());
    }

    @Test
    void createdAsCompletedGetsCompletionDate() {
        Milestone m = newMilestone(newGoal("G"), "M");
        Task t = planner.createTask(m.getId(), "T", today, "Low", "Completed");
        assertEquals(today, t.getCompletionDate());
    }

    @Test
    void invalidPriorityRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        assertThrows(ValidationException.class, () -> planner.createTask(m.getId(), "T", today, "Urgent", null));
    }

    /** v0.7: tasks are listed with the soonest due date first. */
    @Test
    void tasksListedSoonestDueDateFirst() {
        Milestone m = newMilestone(newGoal("G"), "M");
        planner.createTask(m.getId(), "Late", today.plusDays(8), null, null);
        planner.createTask(m.getId(), "Early", today.plusDays(2), null, null);
        planner.createTask(m.getId(), "Middle", today.plusDays(5), null, null);

        List<String> descriptions = planner.getTasksForMilestone(m.getId()).stream()
                .map(Task::getDescription).toList();
        assertEquals(List.of("Early", "Middle", "Late"), descriptions);
    }

    // ---------------- progress entries

    @Test
    void validProgressNoteSavedWithAutomaticDate() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ProgressEntry e = planner.addProgressEntry(t.getId(), "  Finished part 1 ");
        assertEquals(today, e.getEntryDate());
        ProgressEntry loaded = planner.getProgressEntriesForTask(t.getId()).get(0);
        assertEquals("Finished part 1", loaded.getNote());
        assertEquals(today, loaded.getEntryDate());
    }

    @Test
    void blankProgressNoteRejected() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ValidationException e = assertThrows(ValidationException.class, () -> planner.addProgressEntry(t.getId(), " "));
        assertEquals("Progress note is required.", e.getMessage());
    }

    @Test
    void updateAndDeleteProgressEntry() {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ProgressEntry e = planner.addProgressEntry(t.getId(), "one");
        planner.updateProgressEntry(e.getId(), "two");
        assertEquals("two", planner.getProgressEntriesForTask(t.getId()).get(0).getNote());
        planner.deleteProgressEntry(e.getId());
        assertTrue(planner.getProgressEntriesForTask(t.getId()).isEmpty());
    }

    /** v0.7: progress history is listed by entry date in ascending order. */
    @Test
    void progressEntriesListedByEntryDateAscending() throws Exception {
        Task t = newTask(newMilestone(newGoal("G"), "M"), "T");
        ProgressEntry first = planner.addProgressEntry(t.getId(), "first");
        ProgressEntry second = planner.addProgressEntry(t.getId(), "second");

        // The application creates entries with today's date. Move the test rows to
        // different dates so the required ordering is independently verified.
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE progress_entries SET entry_date = ? WHERE id = ?")) {
            statement.setString(1, today.plusDays(2).toString());
            statement.setLong(2, first.getId());
            statement.executeUpdate();
            statement.setString(1, today.minusDays(1).toString());
            statement.setLong(2, second.getId());
            statement.executeUpdate();
        }

        List<String> notes = planner.getProgressEntriesForTask(t.getId()).stream()
                .map(ProgressEntry::getNote).toList();
        assertEquals(List.of("second", "first"), notes);
    }
}
