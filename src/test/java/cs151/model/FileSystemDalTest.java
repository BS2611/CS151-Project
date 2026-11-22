package cs151.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that GoalPlanner works with a different DAL (the text-file one) and that FileSystemDAL is correct. */
class FileSystemDalTest {
    @TempDir
    Path tempDir;

    private Path goalFile;
    private FileSystemDAL dal;
    private GoalPlanner planner;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        goalFile = tempDir.resolve("goals.txt");
        dal = new FileSystemDAL(goalFile);
        planner = new GoalPlanner(dal, new DataManager(tempDir.resolve("test.db")));
    }

    private Goal newGoal(String name) {
        return planner.createGoal(name, "desc", "Academic", today.plusDays(30), "Not Started", 0);
    }

    @Test
    void plannerStoresAndReloadsGoalsThroughTheInjectedDal() {
        Goal saved = newGoal("Learn Java");

        assertEquals(1, saved.getId());
        Goal loaded = new FileSystemDAL(goalFile).findGoalById(saved.getId()).orElseThrow();
        assertEquals("Learn Java", loaded.getName());
        assertEquals("Academic", loaded.getCategory());
        assertEquals(today.plusDays(30), loaded.getTargetDate());
        assertEquals(1, planner.getAllGoals().size());
    }

    @Test
    void updateIsSavedToTheFile() {
        Goal goal = newGoal("Run a 5K");

        planner.updateGoal(goal.getId(), "Run a 10K", "longer", "Health", today.plusDays(60), "In Progress", 40);

        Goal reloaded = new FileSystemDAL(goalFile).findGoalById(goal.getId()).orElseThrow();
        assertEquals("Run a 10K", reloaded.getName());
        assertEquals("In Progress", reloaded.getStatus());
        assertEquals(40, reloaded.getProgress());
    }

    @Test
    void markCompletedAndProgressAreSaved() {
        Goal goal = newGoal("Read a book");

        planner.updateGoalProgress(goal.getId(), 55);
        assertEquals(55, new FileSystemDAL(goalFile).findGoalById(goal.getId()).orElseThrow().getProgress());

        planner.markGoalCompleted(goal.getId());
        Goal done = new FileSystemDAL(goalFile).findGoalById(goal.getId()).orElseThrow();
        assertEquals("Completed", done.getStatus());
        assertEquals(100, done.getProgress());
    }

    @Test
    void deleteRemovesTheGoalAndIdsAreNotReused() {
        Goal first = newGoal("First");
        Goal second = newGoal("Second");

        planner.deleteGoal(first.getId());
        Goal third = newGoal("Third");

        assertFalse(dal.findGoalById(first.getId()).isPresent());
        assertTrue(dal.findGoalById(second.getId()).isPresent());
        assertEquals(3, third.getId());
        assertThrows(NotFoundException.class, () -> planner.deleteGoal(first.getId()));
    }

    @Test
    void businessRulesStillApplyWhenTheDalIsDifferent() {
        newGoal("Unique name");

        assertThrows(DuplicateEntityException.class, () -> newGoal("Unique name"));
        assertThrows(NotFoundException.class, () -> planner.getGoal(99));
    }

    @Test
    void separatorsAndLineBreaksInTextSurviveTheFile() {
        Goal goal = planner.createGoal("A | B \\ C", "line one\nline two | end", "Other",
                today.plusDays(5), "Not Started", 0);

        Goal loaded = new FileSystemDAL(goalFile).findGoalById(goal.getId()).orElseThrow();
        assertEquals("A | B \\ C", loaded.getName());
        assertEquals("line one\nline two | end", loaded.getDescription());
    }

    @Test
    void updateAndDeleteOfAMissingGoalReturnFalse() {
        Goal never = new Goal(42, "Never saved", "", "Other", today, "Not Started", 0, today);

        assertFalse(dal.updateGoal(never));
        assertFalse(dal.deleteGoal(42));
    }
}
