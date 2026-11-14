package cs151.model;

import java.util.List;

/** A Milestone with its tasks. */
public record MilestoneNode(Milestone milestone, List<TaskNode> tasks) {
}
