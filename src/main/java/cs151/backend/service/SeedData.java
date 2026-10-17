package cs151.backend.service;

import cs151.backend.model.Goal;

import java.time.LocalDate;

/**
 * Populates an empty database with exactly 3 goals and 3 milestones per goal (v0.6 requirement).
 * It does nothing when goals already exist, so calling it on every start never creates duplicates.
 * Dates are relative to today because a goal's target date must be today or later.
 */
public final class SeedData {
    private SeedData() {
    }

    /** Seeds the sample goals and milestones if the database has no goals yet. */
    public static void seedIfEmpty(GoalPlanner planner) {
        if (!planner.getAllGoals().isEmpty()) return;

        LocalDate today = LocalDate.now();

        Goal dsa = planner.createGoal("Master Data Structures and Algorithms",
                "Get comfortable with core data structures and interview-style problems.",
                "Academic", today.plusDays(90));
        planner.createMilestone(dsa.getId(), "Finish arrays and linked lists", today.plusDays(20), null);
        planner.createMilestone(dsa.getId(), "Finish trees and graphs", today.plusDays(50), null);
        planner.createMilestone(dsa.getId(), "Complete 50 practice problems", today.plusDays(85), null);

        Goal internship = planner.createGoal("Land a Summer Internship",
                "Apply to software engineering internships for next summer.",
                "Career", today.plusDays(150));
        planner.createMilestone(internship.getId(), "Update resume and LinkedIn", today.plusDays(15), null);
        planner.createMilestone(internship.getId(), "Submit 20 applications", today.plusDays(60), null);
        planner.createMilestone(internship.getId(), "Complete mock interviews", today.plusDays(120), null);

        Goal fitness = planner.createGoal("Run a 10K",
                "Build up running endurance to finish a 10K race.",
                "Health", today.plusDays(120));
        planner.createMilestone(fitness.getId(), "Run 3 km without stopping", today.plusDays(30), null);
        planner.createMilestone(fitness.getId(), "Run 5 km", today.plusDays(70), null);
        planner.createMilestone(fitness.getId(), "Run 8 km", today.plusDays(100), null);
    }
}
