package cs151.backend.persistence;

import cs151.backend.model.Goal;

import java.util.List;
import java.util.Optional;
/**
 * Data access layer for goals. The service layer only depends on this interface, so the storage
 * (SQLite, file system) can be replaced without changing the business rules.
 */
public interface DAL {
    /** Stores a new goal, sets its generated id and returns the id. */
    long insertGoal(Goal goal);

    /** Returns all goals (the SQLite implementation sorts them by target date, soonest first). */
    List<Goal> getAllGoals();

    /** Saves the changes of an existing goal; returns whether a goal was updated. */
    boolean updateGoal(Goal goal);

    /** Deletes the goal (and its milestones, tasks and progress entries); returns whether it existed. */
    boolean deleteGoal(long goalId);

    Optional<Goal> findGoalById(long goalId);

    /** Finds a goal by its exact name. */
    Optional<Goal> findGoalByName(String name);
}
