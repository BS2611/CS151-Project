package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.exception.PersistenceException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.service.GoalPlanner;
import cs151.frontend.Motion;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
    private TextField descriptionField;

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

    /**
     * Reads the form and asks {@link GoalPlanner} to validate and permanently store the task under the
     * selected milestone. The error message of a failed validation is shown as a toast.
     */
    @FXML
    private void saveTask() {
        try {
            goalPlanner.createTask(
                    milestone.getId(),
                    descriptionField.getText(),
                    dueDatePicker.getValue(),
                    priorityComboBox.getValue(),
                    statusComboBox.getValue()
            );

            Motion.toast("Task saved successfully.", false);
            clearForm();
        } catch (ValidationException | PersistenceException exception) {
            Motion.toast(exception.getMessage(), true);
        }
    }

    /** Empties the form after a successful save so another task can be entered. */
    private void clearForm() {
        descriptionField.clear();
        dueDatePicker.setValue(null);
        priorityComboBox.setValue(Task.DEFAULT_PRIORITY);
        statusComboBox.setValue(Task.STATUS_TO_DO);
    }

    @FXML
    private void goBack() throws IOException {
        Goal goal = goalPlanner.getGoal(milestone.getGoalId());
        Main.showMilestoneListPage(goal);
    }
}