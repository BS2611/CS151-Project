package cs151.backend;

import cs151.backend.exception.DuplicateEntityException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.service.SeedData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** v0.6: seed data (3 goals x 3 milestones) and saving a task under its milestone. */
class SeedAndTaskSaveTest extends BackendTestBase {

    @Test
    void seedCreatesThreeGoalsWithThreeMilestonesEach() {
        SeedData.seedIfEmpty(planner);

        List<Goal> goals = planner.getAllGoals();
        assertEquals(3, goals.size());
        for (Goal g : goals) {
            assertEquals(3, planner.getMilestonesForGoal(g.getId()).size());
        }
    }

    /** v0.7: every one of the nine seeded milestones has exactly two tasks. */
    @Test
    void seedCreatesTwoTasksForEachMilestone() {
        SeedData.seedIfEmpty(planner);

        int milestoneCount = 0;
        int taskCount = 0;
        for (Goal goal : planner.getAllGoals()) {
            for (Milestone milestone : planner.getMilestonesForGoal(goal.getId())) {
                milestoneCount++;
                assertEquals(2, planner.getTasksForMilestone(milestone.getId()).size(),
                        "Each seeded milestone should contain exactly two tasks.");
                taskCount += planner.getTasksForMilestone(milestone.getId()).size();
            }
        }

        assertEquals(9, milestoneCount);
        assertEquals(18, taskCount);
    }

    @Test
    void seedingTwiceDoesNotCreateDuplicates() {
        SeedData.seedIfEmpty(planner);
        SeedData.seedIfEmpty(planner);

        assertEquals(3, planner.getAllGoals().size());
    }

    @Test
    void seedDoesNothingWhenGoalsAlreadyExist() {
        newGoal("Mine");
        SeedData.seedIfEmpty(planner);

        assertEquals(1, planner.getAllGoals().size());
    }

    @Test
    void savedTaskIsLinkedToItsMilestoneOnly() {
        Goal g = newGoal("G");
        Milestone m1 = newMilestone(g, "M1");
        Milestone m2 = newMilestone(g, "M2");
        Task t = newTask(m1, "Write code");

        List<Task> forM1 = planner.getTasksForMilestone(m1.getId());
        assertEquals(1, forM1.size());
        assertEquals(t.getId(), forM1.get(0).getId());
        assertEquals(m1.getId(), forM1.get(0).getMilestoneId());
        assertEquals(0, planner.getTasksForMilestone(m2.getId()).size());
    }

    @Test
    void duplicateTaskDescriptionInSameMilestoneIsRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");
        newTask(m, "Same");

        assertThrows(DuplicateEntityException.class, () -> newTask(m, "Same"));
    }

    @Test
    void taskDueDateAfterMilestoneTargetDateIsRejected() {
        Milestone m = newMilestone(newGoal("G"), "M");

        assertThrows(ValidationException.class,
                () -> planner.createTask(m.getId(), "Late", m.getTargetDate().plusDays(1), null, null));
    }
}
