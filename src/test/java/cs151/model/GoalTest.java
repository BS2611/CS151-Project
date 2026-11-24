package cs151.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoalTest extends BackendTestBase {

    @Test
    void validGoalIsCreatedAndSaved() {
        Goal g = planner.createGoal("  Learn SQL  ", "desc", GoalCategory.CAREER, today, GoalStatus.IN_PROGRESS, 40);
        assertTrue(g.getId() > 0);
        Goal loaded = planner.getGoal(g.getId());
        assertEquals("Learn SQL", loaded.getName());
        assertEquals(GoalCategory.CAREER, loaded.getCategory());
        assertEquals(GoalStatus.IN_PROGRESS, loaded.getStatus());
        assertEquals(40, loaded.getProgress());
        assertEquals(today, loaded.getTargetDate());
    }

    @Test
    void defaultsAreNotStartedAndZero() {
        Goal g = planner.createGoal("Defaults", null, GoalCategory.OTHER, today.plusDays(1));
        assertEquals(GoalStatus.NOT_STARTED, g.getStatus());
        assertEquals(0, g.getProgress());
    }

    /** v0.6: goals are listed with the soonest target date on top. */
    @Test
    void goalsListedSoonestTargetDateFirst() {
        planner.createGoal("Soon", "", GoalCategory.OTHER, today.plusDays(5));
        planner.createGoal("Far", "", GoalCategory.OTHER, today.plusDays(60));
        planner.createGoal("Mid", "", GoalCategory.OTHER, today.plusDays(20));

        List<String> names = planner.getAllGoals().stream().map(Goal::getName).toList();
        assertEquals(List.of("Soon", "Mid", "Far"), names);
    }

    @Test
    void blankNameRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("   ", "", GoalCategory.OTHER, today, GoalStatus.NOT_STARTED, 0));
        assertEquals("Goal name is required.", e.getMessage());
    }

    @Test
    void duplicateTrimmedNameRejected() {
        newGoal("Run a marathon");
        DuplicateEntityException e = assertThrows(DuplicateEntityException.class,
                () -> planner.createGoal("  Run a marathon ", "", GoalCategory.HEALTH, today, GoalStatus.NOT_STARTED, 0));
        assertEquals("Goal name already exists.", e.getMessage());
    }

    @Test
    void pastTargetDateRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("Old", "", GoalCategory.OTHER, today.minusDays(1), GoalStatus.NOT_STARTED, 0));
        assertEquals("Target date must be today or later.", e.getMessage());
    }

    @Test
    void missingCategoryRejectedAndMissingStatusDefaults() {
        // an unknown category or status cannot be written any more: the enum types make it a compile error
        assertThrows(ValidationException.class,
                () -> planner.createGoal("A", "", null, today, GoalStatus.NOT_STARTED, 0));
        assertEquals(GoalStatus.NOT_STARTED,
                planner.createGoal("B", "", GoalCategory.OTHER, today, null, 0).getStatus());
    }

    @Test
    void progressBelowZeroRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("Neg", "", GoalCategory.OTHER, today, GoalStatus.NOT_STARTED, -1));
        assertEquals("Progress must be between 0 and 100.", e.getMessage());
    }

    @Test
    void progressAboveHundredRejected() {
        assertThrows(ValidationException.class,
                () -> planner.createGoal("Big", "", GoalCategory.OTHER, today, GoalStatus.IN_PROGRESS, 101));
    }

    @Test
    void completedWithoutFullProgressRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> planner.createGoal("Almost", "", GoalCategory.OTHER, today, GoalStatus.COMPLETED, 99));
        assertEquals("Completed goals must have 100% progress.", e.getMessage());
    }

    @Test
    void validCompletedGoalSaved() {
        Goal g = planner.createGoal("Done", "", GoalCategory.OTHER, today, GoalStatus.COMPLETED, 100);
        assertEquals(GoalStatus.COMPLETED, planner.getGoal(g.getId()).getStatus());
        assertEquals(100, planner.getGoal(g.getId()).getProgress());
    }

    @Test
    void creationDateGeneratedAutomatically() {
        Goal g = newGoal("Dated");
        assertEquals(today, g.getCreationDate());
        assertEquals(today, planner.getGoal(g.getId()).getCreationDate());
    }

    @Test
    void progressUpdateAndCompletion() {
        Goal g = newGoal("Progressing");
        assertEquals(60, planner.updateGoalProgress(g.getId(), 60).getProgress());
        assertThrows(ValidationException.class, () -> planner.updateGoalProgress(g.getId(), 101));

        Goal done = planner.markGoalCompleted(g.getId());
        assertEquals(GoalStatus.COMPLETED, done.getStatus());
        assertEquals(100, planner.getGoal(g.getId()).getProgress());
        assertThrows(ValidationException.class, () -> planner.updateGoalProgress(g.getId(), 50));
    }

    @Test
    void updateGoalKeepsUniqueNames() {
        Goal a = newGoal("A");
        newGoal("B");
        assertThrows(DuplicateEntityException.class,
                () -> planner.updateGoal(a.getId(), "B", "", GoalCategory.OTHER, today.plusDays(30), GoalStatus.NOT_STARTED, 0));
        Goal renamed = planner.updateGoal(a.getId(), "A2", "x", GoalCategory.HEALTH, today.plusDays(31), GoalStatus.ON_HOLD, 10);
        assertEquals("A2", planner.getGoal(renamed.getId()).getName());
        assertEquals(today, planner.getGoal(a.getId()).getCreationDate());
    }

    @Test
    void deleteMissingGoalThrows() {
        assertThrows(ValidationException.class, () -> planner.deleteGoal(9999));
    }
}
