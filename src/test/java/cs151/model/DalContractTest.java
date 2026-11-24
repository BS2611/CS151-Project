package cs151.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules every {@link DAL} strategy must follow. Each storage (SQLite, text file) extends this class, so the same
 * tests run against both, and then {@link GoalPlanner} / {@link ReportGenerator} are checked on top of the same DAL.
 */
abstract class DalContractTest {
    @TempDir
    protected Path tempDir;

    protected DAL dal;
    protected GoalPlanner planner;
    protected ReportGenerator reports;
    protected final LocalDate today = LocalDate.now();

    /** Creates a new, empty storage inside the given folder. */
    protected abstract DAL createDal(Path folder);

    @BeforeEach
    void setUpDal() {
        dal = createDal(tempDir);
        planner = new GoalPlanner(dal);
        reports = new ReportGenerator(dal);
    }

    private Goal goal(String name) {
        return new Goal(name, "desc", GoalCategory.ACADEMIC, today.plusDays(30), GoalStatus.NOT_STARTED, 0);
    }

    private Goal savedGoal(String name) {
        Goal g = goal(name);
        dal.insertGoal(g);
        return g;
    }

    private Milestone savedMilestone(Goal g, String name, int days) {
        Milestone m = new Milestone(g.getId(), name, today.plusDays(days), MilestoneStatus.NOT_STARTED);
        dal.insertMilestone(m);
        return m;
    }

    private Task savedTask(Milestone m, String description, int days) {
        Task t = new Task(m.getId(), description, today.plusDays(days), TaskPriority.MEDIUM, TaskStatus.TO_DO);
        dal.insertTask(t);
        return t;
    }

    // ------------------------------------------------------------------ the DAL itself

    @Test
    void insertSetsIdsAndFindReturnsTheRecords() {
        Goal g = savedGoal("G");
        Milestone m = savedMilestone(g, "M", 5);
        Task t = savedTask(m, "T", 2);
        ProgressEntry e = new ProgressEntry(t.getId(), "note");
        long entryId = dal.insertProgressEntry(e);

        assertTrue(g.getId() > 0 && m.getId() > 0 && t.getId() > 0);
        assertEquals(entryId, e.getId());
        assertEquals("G", dal.findGoalById(g.getId()).orElseThrow().getName());
        assertEquals("M", dal.findMilestoneById(m.getId()).orElseThrow().getMilestoneName());
        assertEquals("T", dal.findTaskById(t.getId()).orElseThrow().getDescription());
        assertEquals("note", dal.findProgressEntryById(entryId).orElseThrow().getNote());
        assertTrue(dal.findGoalById(999).isEmpty());
        assertTrue(dal.findTaskById(999).isEmpty());
    }

    @Test
    void findGoalByNameIsExactAndCaseSensitive() {
        savedGoal("Learn Java");

        assertTrue(dal.findGoalByName("Learn Java").isPresent());
        assertTrue(dal.findGoalByName("  Learn Java ").isPresent());
        assertTrue(dal.findGoalByName("learn java").isEmpty());
        assertTrue(dal.findGoalByName("Learn").isEmpty());
    }

    @Test
    void listsAreSortedByDateThenName() {
        Goal late = new Goal("B late", "", GoalCategory.OTHER, today.plusDays(50), GoalStatus.NOT_STARTED, 0);
        Goal soonB = new Goal("B soon", "", GoalCategory.OTHER, today.plusDays(10), GoalStatus.NOT_STARTED, 0);
        Goal soonA = new Goal("A soon", "", GoalCategory.OTHER, today.plusDays(10), GoalStatus.NOT_STARTED, 0);
        dal.insertGoal(late);
        dal.insertGoal(soonB);
        dal.insertGoal(soonA);

        assertEquals(List.of("A soon", "B soon", "B late"),
                dal.getAllGoals().stream().map(Goal::getName).toList());

        Goal g = savedGoal("Parent");
        savedMilestone(g, "later", 20);
        savedMilestone(g, "sooner", 3);
        assertEquals(List.of("sooner", "later"),
                dal.getMilestonesForGoal(g.getId()).stream().map(Milestone::getMilestoneName).toList());
    }

    @Test
    void updateChangesTheStoredRecordAndReportsMissingOnes() {
        Goal g = savedGoal("Old");
        g.update("New", g.getDescription(), g.getCategory(), g.getTargetDate(), GoalStatus.IN_PROGRESS, 40);
        assertTrue(dal.updateGoal(g));
        Goal loaded = dal.findGoalById(g.getId()).orElseThrow();
        assertEquals("New", loaded.getName());
        assertEquals(40, loaded.getProgress());

        Milestone m = savedMilestone(g, "M", 5);
        m.update("M2", m.getTargetDate(), MilestoneStatus.COMPLETED);
        assertTrue(dal.updateMilestone(m));
        assertEquals("M2", dal.findMilestoneById(m.getId()).orElseThrow().getMilestoneName());

        Task t = savedTask(m, "T", 2);
        t.update("T2", t.getDueDate(), t.getPriority(), t.getStatus());
        t.markCompleted();
        assertTrue(dal.updateTask(t));
        Task savedTask = dal.findTaskById(t.getId()).orElseThrow();
        assertEquals("T2", savedTask.getDescription());
        assertEquals(TaskStatus.COMPLETED, savedTask.getStatus());
        assertEquals(today, savedTask.getCompletionDate());

        ProgressEntry e = new ProgressEntry(t.getId(), "first");
        dal.insertProgressEntry(e);
        e.setNote("second");
        assertTrue(dal.updateProgressEntry(e));
        assertEquals("second", dal.findProgressEntryById(e.getId()).orElseThrow().getNote());

        assertFalse(dal.updateGoal(new Goal(999, "x", "", GoalCategory.OTHER, today, GoalStatus.NOT_STARTED, 0, today)));
        assertFalse(dal.deleteGoal(999));
        assertFalse(dal.deleteMilestone(999));
        assertFalse(dal.deleteTask(999));
        assertFalse(dal.deleteProgressEntry(999));
    }

    @Test
    void namesMustBeUniqueWhereTheSchemaSaysSo() {
        Goal g = savedGoal("Same");
        assertThrows(DuplicateEntityException.class, () -> dal.insertGoal(goal("Same")));

        Milestone m = savedMilestone(g, "M", 5);
        assertThrows(DuplicateEntityException.class, () -> savedMilestone(g, "M", 6));
        Goal other = savedGoal("Other goal");
        savedMilestone(other, "M", 5);               // same name under another goal is fine

        savedTask(m, "T", 2);
        assertThrows(DuplicateEntityException.class, () -> savedTask(m, "T", 3));
        Milestone m2 = savedMilestone(g, "M2", 5);
        savedTask(m2, "T", 2);                       // same description under another milestone is fine
    }

    @Test
    void childrenNeedAnExistingParent() {
        assertThrows(NotFoundException.class,
                () -> dal.insertMilestone(new Milestone(99, "M", today, MilestoneStatus.NOT_STARTED)));
        assertThrows(NotFoundException.class,
                () -> dal.insertTask(new Task(99, "T", today, TaskPriority.MEDIUM, TaskStatus.TO_DO)));
        assertThrows(NotFoundException.class, () -> dal.insertProgressEntry(new ProgressEntry(99, "n")));
    }

    @Test
    void deletingAParentRemovesEverythingBelowIt() {
        Goal g = savedGoal("G");
        Milestone m = savedMilestone(g, "M", 5);
        Task t = savedTask(m, "T", 2);
        ProgressEntry e = new ProgressEntry(t.getId(), "n");
        dal.insertProgressEntry(e);
        Goal keep = savedGoal("Keep");
        Milestone keepM = savedMilestone(keep, "M", 5);

        assertTrue(dal.deleteGoal(g.getId()));

        assertTrue(dal.findMilestoneById(m.getId()).isEmpty());
        assertTrue(dal.findTaskById(t.getId()).isEmpty());
        assertTrue(dal.findProgressEntryById(e.getId()).isEmpty());
        assertTrue(dal.findMilestoneById(keepM.getId()).isPresent());

        Milestone m2 = savedMilestone(keep, "M2", 5);
        Task t2 = savedTask(m2, "T2", 2);
        ProgressEntry e2 = new ProgressEntry(t2.getId(), "n");
        dal.insertProgressEntry(e2);
        assertTrue(dal.deleteTask(t2.getId()));
        assertTrue(dal.findProgressEntryById(e2.getId()).isEmpty());
        Task t3 = savedTask(m2, "T3", 2);
        assertTrue(dal.deleteMilestone(m2.getId()));
        assertTrue(dal.findTaskById(t3.getId()).isEmpty());
    }

    @Test
    void searchIsCaseInsensitiveAndCoversTheDocumentedColumns() {
        Goal g = new Goal("Learn Java", "weekly practice", GoalCategory.CAREER, today.plusDays(30), GoalStatus.IN_PROGRESS, 45);
        dal.insertGoal(g);
        Milestone m = savedMilestone(g, "Finish Streams chapter", 10);
        savedTask(m, "Read the LAMBDA notes", 5);

        assertEquals(1, dal.searchGoals("java").size());
        assertEquals(1, dal.searchGoals("PRACTICE").size());
        assertEquals(1, dal.searchGoals("career").size());
        assertEquals(1, dal.searchGoals("45").size());
        assertEquals(1, dal.searchGoals(today.plusDays(30).toString()).size());
        assertEquals(0, dal.searchGoals("python").size());
        assertEquals(1, dal.searchMilestones("streams").size());
        assertEquals(1, dal.searchMilestones("not started").size());
        assertEquals(1, dal.searchTasks("lambda").size());
        assertEquals(1, dal.searchTasks("medium").size());
        assertEquals(0, dal.searchTasks("zzz").size());
    }

    // ------------------------------------------------------------------ the planner on top of this DAL

    @Test
    void goalPlannerAppliesTheSameBusinessRulesOnAnyStorage() {
        Goal g = planner.createGoal("Plan", "d", GoalCategory.HEALTH, today.plusDays(30), null, 0);
        Milestone m = planner.createMilestone(g.getId(), "M", today.plusDays(10), null);
        Task t = planner.createTask(m.getId(), "T", today.plusDays(5), null, null);
        planner.addProgressEntry(t.getId(), "started");

        assertThrows(DuplicateEntityException.class,
                () -> planner.createGoal("Plan", "d", GoalCategory.HEALTH, today.plusDays(30), null, 0));
        assertThrows(ValidationException.class,
                () -> planner.createMilestone(g.getId(), "Late", today.plusDays(60), null));
        assertThrows(ValidationException.class,
                () -> planner.createTask(m.getId(), "Late", today.plusDays(20), null, null));
        assertThrows(NotFoundException.class, () -> planner.getGoal(999));

        planner.markTaskCompleted(t.getId());
        planner.updateGoalProgress(g.getId(), 60);
        assertEquals(60, planner.getGoal(g.getId()).getProgress());
        assertEquals(today, planner.getTask(t.getId()).getCompletionDate());
        assertEquals(1, planner.getProgressEntriesForTask(t.getId()).size());
        assertEquals(new GoalDeletionImpact(1, 1, 1), planner.getDeletionImpact(g.getId()));

        assertEquals(1, planner.searchRelatedGoals("Not Started").size());   // found through the goal itself
        assertEquals(1, planner.searchRelatedGoals("T").size());              // one goal even with many matches
        assertTrue(planner.searchRelatedGoals("zzz").isEmpty());

        planner.deleteGoal(g.getId());
        assertThrows(NotFoundException.class, () -> planner.getMilestone(m.getId()));
        assertThrows(NotFoundException.class, () -> planner.getTask(t.getId()));
    }

    @Test
    void reportGeneratorWorksOnAnyStorage() {
        Goal done = planner.createGoal("Done", "d", GoalCategory.CAREER, today.plusDays(30), null, 0);
        planner.markGoalCompleted(done.getId());
        Goal open = planner.createGoal("Open", "d", GoalCategory.CAREER, today.plusDays(40), GoalStatus.IN_PROGRESS, 50);
        Milestone m = planner.createMilestone(open.getId(), "M", today.plusDays(10), null);
        planner.createTask(m.getId(), "T1", today.plusDays(2), null, null);
        planner.createTask(m.getId(), "T2", today.plusDays(3), null, null);

        OverallSummary summary = reports.overallSummary();
        assertEquals(2, summary.totalGoals());
        assertEquals(1, summary.completedGoals());
        assertEquals(1, summary.activeGoals());
        assertEquals(2, summary.totalTasks());
        assertEquals(50.0, summary.completionRate(), 0.001);

        assertEquals(1, reports.statusSummary().get("Completed"));
        assertEquals(2, reports.goalsOverview().size());
        GoalNode detail = reports.selectedGoalDetail(open.getId());
        assertEquals(1, detail.milestones().size());
        assertEquals(2, detail.milestones().get(0).tasks().size());
        assertEquals(75.0, reports.categorySummary().stream()
                .filter(r -> r.category().equals(GoalCategory.CAREER.label())).findFirst().orElseThrow().averageProgress(), 0.001);
        assertTrue(reports.overdueItems().goals().isEmpty());
    }
}
