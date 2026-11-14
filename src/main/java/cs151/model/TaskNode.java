package cs151.model;

import java.util.List;

/** A Task with its progress entries (leaf of the goal hierarchy report). */
public record TaskNode(Task task, List<ProgressEntry> progressEntries) {
}
