package cs151.model;

import java.util.List;

/** Incomplete goals, milestones and tasks whose date has already passed. */
public record OverdueItems(List<Goal> goals, List<Milestone> milestones, List<Task> tasks) {
}
