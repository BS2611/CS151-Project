package cs151.model;

import java.time.LocalDate;

/**
 * Populates an empty database with exactly 3 goals, 3 milestones per goal, 2 tasks per milestone and
 * 2 progress entries per task. Dates are relative to today because a goal's target date must be today or later.
 */
public final class SeedData {

    private SeedData() {
    }

    /** Seeds the sample data if the database has no goals yet, so calling it on every start never duplicates. */
    public static void seedIfEmpty(GoalPlanner planner) {
        if (!planner.getAllGoals().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Goal 1: Master Data Structures and Algorithms
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

        Task arraysTask1 = planner.createTask(
                arrays.getId(),
                "Review array operations and time complexity",
                today.plusDays(7),
                "Medium",
                null
        );
        progress(planner, arraysTask1, today.minusDays(6),
                "Reviewed insertion, deletion and search costs for arrays.");
        progress(planner, arraysTask1, today.minusDays(2),
                "Wrote a summary table of Big-O for common array operations.");

        Task arraysTask2 = planner.createTask(
                arrays.getId(),
                "Complete linked-list coding exercises",
                today.plusDays(14),
                "High",
                null
        );
        progress(planner, arraysTask2, today.minusDays(5),
                "Solved the reverse-list and detect-cycle exercises.");
        progress(planner, arraysTask2, today.minusDays(1),
                "Stuck on merging two sorted lists; will retry with a dummy node.");

        Milestone trees = planner.createMilestone(
                dsa.getId(),
                "Finish trees and graphs",
                today.plusDays(50),
                null
        );

        Task treesTask1 = planner.createTask(
                trees.getId(),
                "Implement binary-tree traversals",
                today.plusDays(30),
                "Medium",
                null
        );
        progress(planner, treesTask1, today.minusDays(6),
                "Implemented in-order and pre-order traversals recursively.");
        progress(planner, treesTask1, today.minusDays(3),
                "Added iterative post-order traversal with a stack.");

        Task treesTask2 = planner.createTask(
                trees.getId(),
                "Practice graph-search problems using BFS and DFS",
                today.plusDays(42),
                "High",
                null
        );
        progress(planner, treesTask2, today.minusDays(5),
                "Solved two BFS shortest-path problems on grids.");
        progress(planner, treesTask2, today.minusDays(2),
                "DFS cycle detection still unclear; plan to revisit next week.");

        Milestone practice = planner.createMilestone(
                dsa.getId(),
                "Complete 50 practice problems",
                today.plusDays(85),
                null
        );

        Task practiceTask1 = planner.createTask(
                practice.getId(),
                "Complete the first 25 practice problems",
                today.plusDays(60),
                "Medium",
                null
        );
        progress(planner, practiceTask1, today.minusDays(6),
                "Finished 10 easy problems on arrays and strings.");
        progress(planner, practiceTask1, today.minusDays(2),
                "Reached 18 problems; two medium ones took over an hour.");

        Task practiceTask2 = planner.createTask(
                practice.getId(),
                "Complete the remaining 25 practice problems",
                today.plusDays(75),
                "High",
                null
        );
        progress(planner, practiceTask2, today.minusDays(4),
                "Picked the next 25 problems, mostly trees and dynamic programming.");
        progress(planner, practiceTask2, today.minusDays(1),
                "Started the first dynamic programming problems.");

        // Goal 2: Land a Summer Internship
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

        Task resumeTask1 = planner.createTask(
                resume.getId(),
                "Revise project descriptions on resume",
                today.plusDays(5),
                "High",
                null
        );
        progress(planner, resumeTask1, today.minusDays(5),
                "Rewrote the term-project bullet points with measurable results.");
        progress(planner, resumeTask1, today.minusDays(2),
                "Asked a friend to proofread the resume.");

        Task resumeTask2 = planner.createTask(
                resume.getId(),
                "Update LinkedIn skills and experience",
                today.plusDays(10),
                "Medium",
                null
        );
        progress(planner, resumeTask2, today.minusDays(4),
                "Added Java, JavaFX and SQLite to the skills section.");
        progress(planner, resumeTask2, today.minusDays(1),
                "Updated the headline and uploaded a new profile photo.");

        Milestone applications = planner.createMilestone(
                internship.getId(),
                "Submit 20 applications",
                today.plusDays(60),
                null
        );

        Task applicationsTask1 = planner.createTask(
                applications.getId(),
                "Research ten suitable internship opportunities",
                today.plusDays(30),
                "Medium",
                null
        );
        progress(planner, applicationsTask1, today.minusDays(6),
                "Listed six companies with open internship postings.");
        progress(planner, applicationsTask1, today.minusDays(3),
                "Found four more roles through the career center portal.");

        Task applicationsTask2 = planner.createTask(
                applications.getId(),
                "Submit twenty tailored internship applications",
                today.plusDays(50),
                "High",
                null
        );
        progress(planner, applicationsTask2, today.minusDays(4),
                "Submitted the first three applications with tailored cover letters.");
        progress(planner, applicationsTask2, today.minusDays(1),
                "Obstacle: some forms need a transcript; requested one from the registrar.");

        Milestone interviews = planner.createMilestone(
                internship.getId(),
                "Complete mock interviews",
                today.plusDays(120),
                null
        );

        Task interviewsTask1 = planner.createTask(
                interviews.getId(),
                "Complete a technical mock interview",
                today.plusDays(80),
                "High",
                null
        );
        progress(planner, interviewsTask1, today.minusDays(5),
                "Booked a mock interview slot with the career center.");
        progress(planner, interviewsTask1, today.minusDays(2),
                "Practiced explaining solutions out loud for 30 minutes.");

        Task interviewsTask2 = planner.createTask(
                interviews.getId(),
                "Complete a behavioral mock interview",
                today.plusDays(105),
                "Medium",
                null
        );
        progress(planner, interviewsTask2, today.minusDays(6),
                "Wrote STAR-format answers for five common questions.");
        progress(planner, interviewsTask2, today.minusDays(3),
                "Rehearsed the answers with a classmate.");

        // Goal 3: Run a 10K
        Goal run = planner.createGoal(
                "Run a 10K",
                "Build up running endurance to finish a 10K race.",
                "Health",
                today.plusDays(120)
        );

        Milestone three = planner.createMilestone(
                run.getId(),
                "Run 3 km without stopping",
                today.plusDays(30),
                null
        );

        Task threeTask1 = planner.createTask(
                three.getId(),
                "Complete three short training runs",
                today.plusDays(10),
                "Medium",
                null
        );
        progress(planner, threeTask1, today.minusDays(6),
                "First 2 km run at an easy pace, took 14 minutes.");
        progress(planner, threeTask1, today.minusDays(3),
                "Ran 2.5 km with one short walking break.");

        Task threeTask2 = planner.createTask(
                three.getId(),
                "Complete a continuous 3 km practice run",
                today.plusDays(20),
                "High",
                null
        );
        progress(planner, threeTask2, today.minusDays(4),
                "Planned the route and scheduled the run for Saturday.");
        progress(planner, threeTask2, today.minusDays(1),
                "Ran 2.8 km without stopping; almost there.");

        Milestone five = planner.createMilestone(
                run.getId(),
                "Run 5 km",
                today.plusDays(70),
                null
        );

        Task fiveTask1 = planner.createTask(
                five.getId(),
                "Complete two interval-training sessions",
                today.plusDays(40),
                "Medium",
                null
        );
        progress(planner, fiveTask1, today.minusDays(5),
                "Researched a 1-minute run, 1-minute walk interval plan.");
        progress(planner, fiveTask1, today.minusDays(2),
                "Completed the first interval session of eight rounds.");

        Task fiveTask2 = planner.createTask(
                five.getId(),
                "Complete a continuous 5 km practice run",
                today.plusDays(60),
                "High",
                null
        );
        progress(planner, fiveTask2, today.minusDays(6),
                "Bought new running shoes to avoid shin pain.");
        progress(planner, fiveTask2, today.minusDays(2),
                "Next step: add 500 m to the weekly long run.");

        Milestone eight = planner.createMilestone(
                run.getId(),
                "Run 8 km",
                today.plusDays(100),
                null
        );

        Task eightTask1 = planner.createTask(
                eight.getId(),
                "Complete a 6 km endurance run",
                today.plusDays(80),
                "Medium",
                null
        );
        progress(planner, eightTask1, today.minusDays(5),
                "Mapped a flat 6 km loop near campus.");
        progress(planner, eightTask1, today.minusDays(2),
                "Obstacle: rainy week; moved the run to the weekend.");

        Task eightTask2 = planner.createTask(
                eight.getId(),
                "Complete a continuous 8 km practice run",
                today.plusDays(90),
                "High",
                null
        );
        progress(planner, eightTask2, today.minusDays(4),
                "Set a weekly mileage target of 20 km.");
        progress(planner, eightTask2, today.minusDays(1),
                "Next steps: practice hydration and pacing on long runs.");

    }

    /** Adds a progress entry with a past entry date so the Entry Date sort order is visible. */
    private static void progress(GoalPlanner planner, Task task, LocalDate entryDate, String note) {
        planner.addProgressEntry(task.getId(), note, entryDate);
    }
}
