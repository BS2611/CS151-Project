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
                GoalCategory.ACADEMIC,
                today.plusDays(30)
        );

        Goal earlier = planner.createGoal(
                "Shared Earlier Goal",
                "description",
                GoalCategory.ACADEMIC,
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

    @Test
    void goalFieldMatchReturnsOnlyThatGoal() {
        Goal match = planner.createGoal("Alpha Goal", "Learn zebra stripes", GoalCategory.CAREER, today.plusDays(30));
        planner.createGoal("Beta Goal", "Something else", GoalCategory.HEALTH, today.plusDays(31));

        assertEquals(List.of(match.getId()), ids(planner.searchRelatedGoals("zebra")));   // description
        assertEquals(List.of(match.getId()), ids(planner.searchRelatedGoals("career")));  // category
        assertEquals(List.of(match.getId()), ids(planner.searchRelatedGoals("alpha")));   // name
    }

    @Test
    void milestoneFieldMatchReturnsItsGoalOnce() {
        Goal parent = newGoal("Parent Goal");
        newMilestone(parent, "Quarterly Review");
        newMilestone(parent, "Quarterly Planning");
        Goal other = newGoal("Other Goal");
        newMilestone(other, "Warm Up");

        assertEquals(List.of(parent.getId()), ids(planner.searchRelatedGoals("quarterly")));
    }

    @Test
    void taskFieldsMatchTheirGoal() {
        Goal parent = newGoal("Parent Goal");
        Milestone milestone = newMilestone(parent, "Milestone One");
        planner.createTask(milestone.getId(), "Write report", today.plusDays(5), TaskPriority.HIGH, TaskStatus.IN_PROGRESS);
        Goal other = newGoal("Other Goal");
        planner.createTask(newMilestone(other, "Milestone Two").getId(), "Read book", today.plusDays(6), TaskPriority.LOW, TaskStatus.TO_DO);

        assertEquals(List.of(parent.getId()), ids(planner.searchRelatedGoals("high")));         // priority
        assertEquals(List.of(parent.getId()), ids(planner.searchRelatedGoals("in progress")));  // status
        assertEquals(List.of(parent.getId()), ids(planner.searchRelatedGoals("write report"))); // description
    }

    @Test
    void datesAreSearchable() {
        Goal goal = planner.createGoal("Dated Goal", "desc", GoalCategory.ACADEMIC, today.plusDays(300));
        Milestone milestone = planner.createMilestone(goal.getId(), "Dated Milestone", today.plusDays(200), null);
        planner.createTask(milestone.getId(), "Dated Task", today.plusDays(100), null, null);
        Goal other = newGoal("Other Goal");

        assertEquals(List.of(goal.getId()), ids(planner.searchRelatedGoals(today.plusDays(300).toString())));
        assertEquals(List.of(goal.getId()), ids(planner.searchRelatedGoals(today.plusDays(200).toString())));
        assertEquals(List.of(goal.getId()), ids(planner.searchRelatedGoals(today.plusDays(100).toString())));
        assertEquals(List.of(goal.getId()), ids(planner.searchRelatedGoals(today.plusDays(100).toString() + "  ")));
        assertTrue(planner.searchRelatedGoals(today.plusDays(400).toString()).isEmpty());
        assertEquals(other.getId(), planner.searchRelatedGoals(today.plusDays(30).toString()).get(0).getId());
    }

    @Test
    void goalProgressIsSearchableAsText() {
        Goal half = planner.createGoal("Half Done", "desc", GoalCategory.ACADEMIC, today.plusDays(30), GoalStatus.IN_PROGRESS, 73);
        planner.createGoal("Just Started", "desc", GoalCategory.ACADEMIC, today.plusDays(31), GoalStatus.NOT_STARTED, 0);

        assertEquals(List.of(half.getId()), ids(planner.searchRelatedGoals("73")));
    }

    @Test
    void noMatchReturnsEmptyList() {
        Goal goal = newGoal("Some Goal");
        newTask(newMilestone(goal, "Some Milestone"), "Some Task");

        assertTrue(planner.searchRelatedGoals("zzz-nothing").isEmpty());
        assertTrue(planner.searchRelatedGoals("%").isEmpty());
    }

    @Test
    void surroundingSpacesAreIgnored() {
        Goal goal = newGoal("Needle Goal");

        assertEquals(List.of(goal.getId()), ids(planner.searchRelatedGoals("   NEEDLE   ")));
    }

    private static List<Long> ids(List<Goal> goals) {
        return goals.stream().map(Goal::getId).toList();
    }
}
