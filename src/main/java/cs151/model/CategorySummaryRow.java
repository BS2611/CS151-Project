package cs151.model;

/** Number of goals in a category and their average progress (0 when the category has no goals). */
public record CategorySummaryRow(String category, int goalCount, double averageProgress) {
}
