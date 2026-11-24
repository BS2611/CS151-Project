package cs151.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global search over goals, milestones and tasks. Part of the subsystem hidden behind the {@link GoalPlanner}
 * facade; storage goes through the {@link DAL} it was given.
 */
public class SearchService {
    private final DAL dal;
    private final GoalService goals;
    private final MilestoneService milestones;

    /** @precondition no argument is null */
    public SearchService(DAL dal, GoalService goals, MilestoneService milestones) {
        this.dal = dal;
        this.goals = goals;
        this.milestones = milestones;
    }

    /**
     * Global, case-insensitive search. Goals match on name, description, category, status, dates or progress;
     * milestones on name, target date or status; tasks on description, due date, priority or status.
     *
     * @postcondition a blank (or null) query returns no results
     */
    public SearchResults search(String query) {
        if (query == null || query.isBlank()) {
            return new SearchResults(List.of(), List.of(), List.of());
        }
        return new SearchResults(dal.searchGoals(query), dal.searchMilestones(query), dal.searchTasks(query));
    }

    /**
     * Returns every Goal related to the search text: a Goal is returned when the text matches the Goal itself,
     * one of its Milestones, or one of its Tasks.
     *
     * @postcondition each goal appears only once, sorted by target date, then name
     */
    public List<Goal> searchRelatedGoals(String query) {
        SearchResults results = search(query);
        Map<Long, Goal> uniqueGoals = new LinkedHashMap<>();

        for (Goal goal : results.goals()) {
            uniqueGoals.put(goal.getId(), goal);
        }
        for (Milestone milestone : results.milestones()) {
            Goal goal = goals.getGoal(milestone.getGoalId());
            uniqueGoals.put(goal.getId(), goal);
        }
        for (Task task : results.tasks()) {
            Milestone milestone = milestones.getMilestone(task.getMilestoneId());
            Goal goal = goals.getGoal(milestone.getGoalId());
            uniqueGoals.put(goal.getId(), goal);
        }

        List<Goal> found = new ArrayList<>(uniqueGoals.values());
        found.sort(Comparator.comparing(Goal::getTargetDate)
                .thenComparing(Goal::getName, String.CASE_INSENSITIVE_ORDER));
        return found;
    }
}
