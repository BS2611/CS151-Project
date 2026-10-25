package cs151.backend.service;

import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;

import java.time.LocalDate;

/**
 * Populates an empty database with exactly 3 goals,
 * 3 milestones per goal, and 2 tasks per milestone.
 */
public final class SeedData {

    private SeedData() {
    }

    public static void seedIfEmpty(GoalPlanner planner) {
        if (!planner.getAllGoals().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Goal 1: Data Structures and Algorithms
        Goal dsa = planner.createGoal(
                "Master Data Structures and Algorithms",
                "Get comfortable with core data structures and interview-style problems.",
                "Academic",
                today.plusDays(90)
        );

        Milestone arrays = planner.createMilestone(
                dsa.getId(),
                "Finish arrays and linked lists",
                today.plusDays(20),
                null
        );

        planner.createTask(
                arrays.getId(),
                "Review array operations and time complexity",
                today.plusDays(7),
                "Medium",
                null
        );

        planner.createTask(
                arrays.getId(),
                "Complete linked-list coding exercises",
                today.plusDays(14),
                "High",
                null
        );

        Milestone trees = planner.createMilestone(
                dsa.getId(),
                "Finish trees and graphs",
                today.plusDays(50),
                null
        );

        planner.createTask(
                trees.getId(),
                "Implement binary-tree traversals",
                today.plusDays(30),
                "Medium",
                null
        );

        planner.createTask(
                trees.getId(),
                "Practice graph-search problems using BFS and DFS",
                today.plusDays(42),
                "High",
                null
        );

        Milestone practiceProblems = planner.createMilestone(
                dsa.getId(),
                "Complete 50 practice problems",
                today.plusDays(85),
                null
        );

        planner.createTask(
                practiceProblems.getId(),
                "Complete the first 25 practice problems",
                today.plusDays(60),
                "Medium",
                null
        );

        planner.createTask(
                practiceProblems.getId(),
                "Complete the remaining 25 practice problems",
                today.plusDays(75),
                "High",
                null
        );

        // Goal 2: Internship
        Goal internship = planner.createGoal(
                "Land a Summer Internship",
                "Apply to software engineering internships for next summer.",
                "Career",
                today.plusDays(150)
        );

        Milestone resume = planner.createMilestone(
                internship.getId(),
                "Update resume and LinkedIn",
                today.plusDays(15),
                null
        );

        planner.createTask(
                resume.getId(),
                "Revise project descriptions on resume",
                today.plusDays(5),
                "High",
                null
        );

        planner.createTask(
                resume.getId(),
                "Update LinkedIn skills and experience",
                today.plusDays(10),
                "Medium",
                null
        );

        Milestone applications = planner.createMilestone(
                internship.getId(),
                "Submit 20 applications",
                today.plusDays(60),
                null
        );

        planner.createTask(
                applications.getId(),
                "Research ten suitable internship opportunities",
                today.plusDays(30),
                "Medium",
                null
        );

        planner.createTask(
                applications.getId(),
                "Submit twenty tailored internship applications",
                today.plusDays(50),
                "High",
                null
        );

        Milestone interviews = planner.createMilestone(
                internship.getId(),
                "Complete mock interviews",
                today.plusDays(120),
                null
        );

        planner.createTask(
                interviews.getId(),
                "Complete a technical mock interview",
                today.plusDays(80),
                "High",
                null
        );

        planner.createTask(
                interviews.getId(),
                "Complete a behavioral mock interview",
                today.plusDays(105),
                "Medium",
                null
        );

        // Goal 3: Fitness
        Goal fitness = planner.createGoal(
                "Run a 10K",
                "Build up running endurance to finish a 10K race.",
                "Health",
                today.plusDays(120)
        );

        Milestone runThreeKm = planner.createMilestone(
                fitness.getId(),
                "Run 3 km without stopping",
                today.plusDays(30),
                null
        );

        planner.createTask(
                runThreeKm.getId(),
                "Complete three short training runs",
                today.plusDays(10),
                "Medium",
                null
        );

        planner.createTask(
                runThreeKm.getId(),
                "Complete a continuous 3 km practice run",
                today.plusDays(20),
                "High",
                null
        );

        Milestone runFiveKm = planner.createMilestone(
                fitness.getId(),
                "Run 5 km",
                today.plusDays(70),
                null
        );

        planner.createTask(
                runFiveKm.getId(),
                "Complete two interval-training sessions",
                today.plusDays(40),
                "Medium",
                null
        );

        planner.createTask(
                runFiveKm.getId(),
                "Complete a continuous 5 km practice run",
                today.plusDays(60),
                "High",
                null
        );

        Milestone runEightKm = planner.createMilestone(
                fitness.getId(),
                "Run 8 km",
                today.plusDays(100),
                null
        );

        planner.createTask(
                runEightKm.getId(),
                "Complete a 6 km endurance run",
                today.plusDays(80),
                "Medium",
                null
        );

        planner.createTask(
                runEightKm.getId(),
                "Complete a continuous 8 km practice run",
                today.plusDays(90),
                "High",
                null
        );
    }
}