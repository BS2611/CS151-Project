package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.service.GoalPlanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class TaskController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    private Milestone milestone;

    @FXML
    private Label summaryName;

    @FXML
    private Label summaryDeadline;

    @FXML
    private Label summaryHint;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private void initialize() {
        priorityComboBox.setItems(
                FXCollections.observableArrayList(Task.PRIORITIES)
        );

        priorityComboBox.setValue(Task.DEFAULT_PRIORITY);

        statusComboBox.setItems(
                FXCollections.observableArrayList(Task.STATUSES)
        );

        statusComboBox.setValue(Task.STATUS_TO_DO);
    }

    public void setMilestone(Milestone milestone) {
        this.milestone = milestone;

        summaryName.setText(milestone.getMilestoneName());
        summaryDeadline.setText(
                "Target deadline: " + milestone.getTargetDate()
        );
        summaryHint.setText(
                "Task due date must be on or before "
                        + milestone.getTargetDate()
        );
    }

    @FXML
    private void goBack() throws IOException {
        Goal goal = goalPlanner.getGoal(milestone.getGoalId());
        Main.showMilestoneListPage(goal);
    }
}