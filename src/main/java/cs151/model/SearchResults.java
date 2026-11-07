package cs151.model;

import java.util.List;

/** Result of a global search: every matching Goal, Milestone and Task. */
public record SearchResults(List<Goal> goals, List<Milestone> milestones, List<Task> tasks) {
    public boolean isEmpty() {
        return goals.isEmpty() && milestones.isEmpty() && tasks.isEmpty();
    }
}
