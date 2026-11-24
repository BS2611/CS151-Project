package cs151.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The text-file strategy ({@link FileSystemDAL}) must pass the DAL contract and keep its file readable. */
class FileSystemDalTest extends DalContractTest {
    private Path file;

    @Override
    protected DAL createDal(Path folder) {
        file = folder.resolve("data").resolve("tracker.txt");   // the folder is created by the DAL
        return new FileSystemDAL(file);
    }

    @Test
    void everythingIsStillThereAfterReopeningTheFile() {
        Goal g = planner.createGoal("Persist", "d", "Other", today.plusDays(30), null, 0);
        Milestone m = planner.createMilestone(g.getId(), "M", today.plusDays(10), null);
        Task t = planner.createTask(m.getId(), "T", today.plusDays(5), null, null);
        planner.addProgressEntry(t.getId(), "note");

        GoalPlanner reopened = new GoalPlanner(new FileSystemDAL(file));

        assertEquals("Persist", reopened.getGoal(g.getId()).getName());
        assertEquals(1, reopened.getMilestonesForGoal(g.getId()).size());
        assertEquals(1, reopened.getTasksForMilestone(m.getId()).size());
        assertEquals("note", reopened.getProgressEntriesForTask(t.getId()).get(0).getNote());
    }

    @Test
    void idsAreNotReusedAfterADelete() {
        Goal first = planner.createGoal("First", "d", "Other", today.plusDays(30), null, 0);
        Goal second = planner.createGoal("Second", "d", "Other", today.plusDays(31), null, 0);
        planner.deleteGoal(first.getId());
        Goal third = planner.createGoal("Third", "d", "Other", today.plusDays(32), null, 0);

        assertEquals(second.getId() + 1, third.getId());
    }

    @Test
    void separatorsAndLineBreaksInTextSurviveTheFile() {
        Goal g = planner.createGoal("A | B \\ C", "line one\nline two | end", "Other", today.plusDays(5), null, 0);
        Milestone m = planner.createMilestone(g.getId(), "pipe | milestone", today.plusDays(2), null);
        Task t = planner.createTask(m.getId(), "task\\with\\backslashes", today.plusDays(1), null, null);
        planner.addProgressEntry(t.getId(), "multi\nline | note");

        FileSystemDAL reopened = new FileSystemDAL(file);

        Goal loaded = reopened.findGoalById(g.getId()).orElseThrow();
        assertEquals("A | B \\ C", loaded.getName());
        assertEquals("line one\nline two | end", loaded.getDescription());
        assertEquals("pipe | milestone", reopened.findMilestoneById(m.getId()).orElseThrow().getMilestoneName());
        assertEquals("task\\with\\backslashes", reopened.findTaskById(t.getId()).orElseThrow().getDescription());
        assertEquals("multi\nline | note", reopened.getProgressEntriesForTask(t.getId()).get(0).getNote());
    }

    @Test
    void damagedAndUnknownLinesAreSkipped() throws IOException {
        Goal g = planner.createGoal("Good", "d", "Other", today.plusDays(5), null, 0);
        List<String> lines = new java.util.ArrayList<>(Files.readAllLines(file));
        lines.add("");
        lines.add("G|not-a-number|broken");
        lines.add("X|unknown|record");
        Files.write(file, lines);

        List<Goal> goals = new FileSystemDAL(file).getAllGoals();

        assertEquals(1, goals.size());
        assertEquals(g.getId(), goals.get(0).getId());
        assertTrue(goals.get(0).getName().equals("Good"));
    }
}
