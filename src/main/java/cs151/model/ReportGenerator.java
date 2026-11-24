package cs151.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the data for the required reports as plain Java objects (see {@code cs151.model});
 * the frontend decides how to display them.
 */
public class ReportGenerator {
    private final DAL dal;

    /** Reads the data through the given {@link DAL}, so the reports work with any storage. */
    public ReportGenerator(DAL dal) {
        this.dal = dal;
    }

    /** Report 1: category, status, progress, target date and milestone/task counts for every goal. */
    public List<GoalOverviewRow> goalsOverview() {
        List<GoalOverviewRow> rows = new ArrayList<>();
        for (Goal g : dal.getAllGoals()) {
            List<Milestone> milestones = dal.getMilestonesForGoal(g.getId());
            int tasks = 0;
            for (Milestone m : milestones) tasks += dal.getTasksForMilestone(m.getId()).size();
            rows.add(new GoalOverviewRow(g.getId(), g.getName(), g.getCategory().label(), g.getStatus().label(),
                    g.getProgress(),
                    g.getTargetDate(), milestones.size(), tasks));
        }
        return rows;
    }

    /** Report 2: every goal with its milestones, tasks and progress entries. */
    public List<GoalNode> goalHierarchy() {
        List<GoalNode> nodes = new ArrayList<>();
        for (Goal g : dal.getAllGoals()) nodes.add(buildNode(g));
        return nodes;
    }

    /** Report 3: number of goals per status (all five statuses are present, in order, even when 0). */
    public Map<String, Integer> statusSummary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GoalStatus status : GoalStatus.values()) counts.put(status.label(), 0);
        for (Goal g : dal.getAllGoals()) counts.merge(g.getStatus().label(), 1, Integer::sum);
        return counts;
    }

    /** Report 4: goal count and average progress for each of the six categories. */
    public List<CategorySummaryRow> categorySummary() {
        List<Goal> goals = dal.getAllGoals();
        List<CategorySummaryRow> rows = new ArrayList<>();
        for (GoalCategory category : GoalCategory.values()) {
            int count = 0;
            int total = 0;
            for (Goal g : goals) {
                if (category == g.getCategory()) {
                    count++;
                    total += g.getProgress();
                }
            }
            rows.add(new CategorySummaryRow(category.label(), count, count == 0 ? 0.0 : (double) total / count));
        }
        return rows;
    }

    /** Report 5: one goal with its milestones and tasks (due dates, priorities and statuses included). */
    public GoalNode selectedGoalDetail(long goalId) {
        Goal goal = dal.findGoalById(goalId).orElseThrow(() -> new NotFoundException("Goal not found."));
        return buildNode(goal);
    }

    /**
     * Report 6: goals, milestones and tasks whose date is before today and that are not finished.
     * Finished means Completed/Abandoned for goals, Completed/Skipped for milestones and
     * Completed/Cancelled for tasks.
     */
    public OverdueItems overdueItems() {
        LocalDate today = LocalDate.now();
        List<Goal> goals = new ArrayList<>();
        List<Milestone> milestones = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        for (Goal g : dal.getAllGoals()) {
            if (g.getTargetDate().isBefore(today) && g.getStatus() != GoalStatus.COMPLETED
                    && g.getStatus() != GoalStatus.ABANDONED) {
                goals.add(g);
            }
            for (Milestone m : dal.getMilestonesForGoal(g.getId())) {
                if (m.getTargetDate().isBefore(today) && m.getStatus() != MilestoneStatus.COMPLETED
                        && m.getStatus() != MilestoneStatus.SKIPPED) {
                    milestones.add(m);
                }
                for (Task t : dal.getTasksForMilestone(m.getId())) {
                    if (t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.COMPLETED
                            && t.getStatus() != TaskStatus.CANCELLED) {
                        tasks.add(t);
                    }
                }
            }
        }
        return new OverdueItems(goals, milestones, tasks);
    }

    /** Report 7: totals, active goals (not Completed/Abandoned), completion rate (%) and total tasks. */
    public OverallSummary overallSummary() {
        List<Goal> goals = dal.getAllGoals();
        int completed = 0;
        int active = 0;
        int tasks = 0;
        for (Goal g : goals) {
            if (g.getStatus() == GoalStatus.COMPLETED) completed++;
            else if (g.getStatus() != GoalStatus.ABANDONED) active++;
            for (Milestone m : dal.getMilestonesForGoal(g.getId())) {
                tasks += dal.getTasksForMilestone(m.getId()).size();
            }
        }
        double rate = goals.isEmpty() ? 0.0 : 100.0 * completed / goals.size();
        return new OverallSummary(goals.size(), completed, active, rate, tasks);
    }

    private GoalNode buildNode(Goal g) {
        List<MilestoneNode> milestones = new ArrayList<>();
        for (Milestone m : dal.getMilestonesForGoal(g.getId())) {
            List<TaskNode> tasks = new ArrayList<>();
            for (Task t : dal.getTasksForMilestone(m.getId())) {
                tasks.add(new TaskNode(t, dal.getProgressEntriesForTask(t.getId())));
            }
            milestones.add(new MilestoneNode(m, tasks));
        }
        return new GoalNode(g, milestones);
    }
}
