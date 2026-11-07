package cs151.model;

/**
 * What deleting a goal removes besides the goal itself: the number of its milestones, of the tasks of those
 * milestones, and of the progress entries of those tasks. Used by the delete confirmation.
 */
public record GoalDeletionImpact(int milestones, int tasks, int progresses) {
}
