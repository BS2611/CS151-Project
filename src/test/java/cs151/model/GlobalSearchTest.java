package cs151.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the v0.9 global search behavior. */
class GlobalSearchTest extends BackendTestBase {

    @Test
    void sameGoalAppearsOnlyOnceWhenSeveralElementsMatch() {
        Goal goal = newGoal("Needle Goal");
        Milestone milestone = newMilestone(goal, "Needle Milestone");
        newTask(milestone, "Needle Task");

        List<Goal> results = planner.searchRelatedGoals("needle");

        assertEquals(1, results.size());
        assertEquals(goal.getId(), results.get(0).getId());
    }

    @Test
    void taskMatchReturnsItsRelatedGoal() {
        Goal matchingGoal = newGoal("Matching Parent");
        Milestone matchingMilestone =
                newMilestone(matchingGoal, "Matching Milestone");
        newTask(matchingMilestone, "Prepare unique presentation");

        Goal unrelatedGoal = newGoal("Unrelated Goal");
        Milestone unrelatedMilestone =
                newMilestone(unrelatedGoal, "Unrelated Milestone");
        newTask(unrelatedMilestone, "Read documentation");

        List<Goal> results =
                planner.searchRelatedGoals("unique");

        assertEquals(1, results.size());
        assertEquals(
                matchingGoal.getId(),
                results.get(0).getId()
        );
    }

    @Test
    void searchIsCaseInsensitiveAndSortedByTargetDate() {
        Goal later = planner.createGoal(
                "Shared Later Goal",
                "description",
                "Academic",
                today.plusDays(30)
        );

        Goal earlier = planner.createGoal(
                "Shared Earlier Goal",
                "description",
                "Academic",
                today.plusDays(20)
        );

        List<Goal> results =
                planner.searchRelatedGoals("SHARED");

        assertEquals(2, results.size());
        assertEquals(earlier.getId(), results.get(0).getId());
        assertEquals(later.getId(), results.get(1).getId());
    }

    @Test
    void blankSearchReturnsNoGoals() {
        assertTrue(planner.searchRelatedGoals("").isEmpty());
        assertTrue(planner.searchRelatedGoals("   ").isEmpty());
        assertTrue(planner.searchRelatedGoals(null).isEmpty());
    }
}