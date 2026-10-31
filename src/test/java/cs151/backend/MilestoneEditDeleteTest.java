package cs151.backend;

import cs151.backend.exception.NotFoundException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the v0.8 Milestone edit and cascade-delete requirements.
 */
class MilestoneEditDeleteTest extends BackendTestBase {

    @Test
    void editingMilestonePermanentlySavesAllEditableFields() {
        Goal goal = newGoal("Goal");
        Milestone milestone = newMilestone(goal, "Original Milestone");
        LocalDate newTargetDate = today.plusDays(20);

        planner.updateMilestone(
                milestone.getId(),
                "Renamed Milestone",
                newTargetDate,
                "In Progress"
        );

        Milestone stored = planner.getMilestone(milestone.getId());

        assertEquals("Renamed Milestone", stored.getMilestoneName());
        assertEquals(newTargetDate, stored.getTargetDate());
        assertEquals("In Progress", stored.getStatus());
        assertEquals(goal.getId(), stored.getGoalId());
    }

    @Test
    void editingMilestoneRejectsDateBeforeItsTaskDueDate() {
        Goal goal = newGoal("Goal");
        Milestone milestone = newMilestone(goal, "Milestone");
        newTask(milestone, "Task");

        assertThrows(
                ValidationException.class,
                () -> planner.updateMilestone(
                        milestone.getId(),
                        "Milestone",
                        today.plusDays(4),
                        "In Progress"
                )
        );

        assertEquals(
                today.plusDays(10),
                planner.getMilestone(milestone.getId()).getTargetDate()
        );
    }

    @Test
    void deletingMilestoneRemovesItsTasksAndProgressOnly() {
        Goal goal = newGoal("Goal");

        Milestone deletedMilestone =
                newMilestone(goal, "Deleted Milestone");
        Task deletedTask =
                newTask(deletedMilestone, "Deleted Task");
        planner.addProgressEntry(deletedTask.getId(), "Deleted progress");

        Milestone survivingMilestone =
                newMilestone(goal, "Surviving Milestone");
        Task survivingTask =
                newTask(survivingMilestone, "Surviving Task");
        planner.addProgressEntry(survivingTask.getId(), "Kept progress");

        planner.deleteMilestone(deletedMilestone.getId());

        assertThrows(
                NotFoundException.class,
                () -> planner.getMilestone(deletedMilestone.getId())
        );

        assertEquals(
                0,
                planner.getTasksForMilestone(deletedMilestone.getId()).size()
        );

        assertEquals(
                0,
                planner.getProgressEntriesForTask(deletedTask.getId()).size()
        );

        assertEquals(goal.getId(), planner.getGoal(goal.getId()).getId());
        assertEquals(
                1,
                planner.getTasksForMilestone(survivingMilestone.getId()).size()
        );
        assertEquals(
                1,
                planner.getProgressEntriesForTask(survivingTask.getId()).size()
        );
    }
}