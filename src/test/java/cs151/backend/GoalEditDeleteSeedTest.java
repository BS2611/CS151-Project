package cs151.backend;

import cs151.backend.dto.GoalDeletionImpact;
import cs151.backend.exception.NotFoundException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;
import cs151.backend.service.SeedData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** v0.8: editing and deleting a goal (including its creation date) and the progress sample data. */
class GoalEditDeleteSeedTest extends BackendTestBase {

    @Test
    void editingGoalSavesAllFieldsIncludingCreationDate() {
        Goal g = newGoal("Original");
        LocalDate newCreation = today.minusDays(10);

        planner.updateGoal(g.getId(), "Renamed", "new description", "Career", today.plusDays(40),
                "In Progress", 25, newCreation);

        Goal stored = planner.getGoal(g.getId());
        assertEquals("Renamed", stored.getName());
        assertEquals("Career", stored.getCategory());
        assertEquals(25, stored.getProgress());
        assertEquals(newCreation, stored.getCreationDate());
    }

    @Test
    void editWithoutCreationDateKeepsTheStoredOne() {
        Goal g = newGoal("Keep");
        LocalDate original = g.getCreationDate();

        planner.updateGoal(g.getId(), "Keep", "", "Academic", today.plusDays(30), "Not Started", 0);

        assertEquals(original, planner.getGoal(g.getId()).getCreationDate());
    }

    @Test
    void creationDateIsRequiredAndCannotBeInTheFuture() {
        Goal g = newGoal("Dates");

        assertThrows(ValidationException.class, () -> planner.updateGoal(g.getId(), "Dates", "", "Academic",
                today.plusDays(30), "Not Started", 0, null));
        assertThrows(ValidationException.class, () -> planner.updateGoal(g.getId(), "Dates", "", "Academic",
                today.plusDays(30), "Not Started", 0, today.plusDays(1)));
    }

    @Test
    void deletionImpactCountsMilestonesTasksAndProgressEntries() {
        Goal g = newGoal("Impact");
        Milestone m1 = newMilestone(g, "M1");
        Milestone m2 = newMilestone(g, "M2");
        Task t1 = newTask(m1, "T1");
        Task t2 = newTask(m1, "T2");
        newTask(m2, "T3");
        planner.addProgressEntry(t1.getId(), "one");
        planner.addProgressEntry(t1.getId(), "two");
        planner.addProgressEntry(t2.getId(), "three");

        assertEquals(new GoalDeletionImpact(2, 3, 3), planner.getDeletionImpact(g.getId()));
    }

    @Test
    void deletingGoalRemovesEverythingLinkedToIt() {
        Goal g = newGoal("Doomed");
        Milestone m = newMilestone(g, "M");
        Task t = newTask(m, "T");
        planner.addProgressEntry(t.getId(), "note");
        Goal other = newGoal("Survivor");
        Task otherTask = newTask(newMilestone(other, "OM"), "OT");
        planner.addProgressEntry(otherTask.getId(), "kept");

        planner.deleteGoal(g.getId());

        assertThrows(NotFoundException.class, () -> planner.getGoal(g.getId()));
        assertEquals(0, planner.getMilestonesForGoal(g.getId()).size());
        assertEquals(0, planner.getTasksForMilestone(m.getId()).size());
        assertEquals(0, planner.getProgressEntriesForTask(t.getId()).size());
        assertEquals(1, planner.getProgressEntriesForTask(otherTask.getId()).size());
    }

    @Test
    void seedGivesTwoProgressEntriesForEveryTaskSortedByEntryDate() {
        SeedData.seedIfEmpty(planner);

        int tasks = 0;
        for (Goal g : planner.getAllGoals()) {
            for (Milestone m : planner.getMilestonesForGoal(g.getId())) {
                for (Task t : planner.getTasksForMilestone(m.getId())) {
                    tasks++;
                    List<ProgressEntry> entries = planner.getProgressEntriesForTask(t.getId());
                    assertEquals(2, entries.size());
                    assertTrue(entries.get(0).getEntryDate().isBefore(entries.get(1).getEntryDate()));
                }
            }
        }
        assertEquals(18, tasks);
    }

    @Test
    void seedingTwiceStillGivesThreeGoalsWithThreeMilestones() {
        SeedData.seedIfEmpty(planner);
        SeedData.seedIfEmpty(planner);

        assertEquals(3, planner.getAllGoals().size());
        for (Goal g : planner.getAllGoals()) {
            assertEquals(3, planner.getMilestonesForGoal(g.getId()).size());
        }
    }
}
