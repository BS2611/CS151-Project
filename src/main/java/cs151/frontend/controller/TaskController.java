package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.service.GoalPlanner;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import java.io.IOException;

public class TaskController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    private Milestone milestone;

    @FXML
    private Label milestoneLabel;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    /**
     * Receives the milestone selected by the user.
     */
    public void setMilestone(Milestone milestone) {
        this.milestone = milestone;
        milestoneLabel.setText("Create Task for: " + milestone.getMilestoneName());
    }

    /**
     * Returns to the Milestone List page.
     */
    @FXML
    private void goBack() throws IOException {
        Goal goal = goalPlanner.getGoal(milestone.getGoalId());
        Main.showMilestoneListPage(goal);
    }
    @FXML
    private void initialize() {
        priorityComboBox.setItems(
                FXCollections.observableArrayList(Task.PRIORITIES)
        );

        statusComboBox.setItems(
                FXCollections.observableArrayList(Task.STATUSES)
        );

        priorityComboBox.setValue(Task.DEFAULT_PRIORITY);
        statusComboBox.setValue(Task.STATUS_TO_DO);
    }
}