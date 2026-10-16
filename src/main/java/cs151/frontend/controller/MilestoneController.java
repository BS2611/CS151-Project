package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.exception.PersistenceException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.service.GoalPlanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.io.IOException;

/**
 * Controller of the Milestone page (milestone-view.fxml). Collects the milestone data, hands it to
 * {@link GoalPlanner} (which validates and stores it permanently under the selected goal) and shows feedback.
 */
public class MilestoneController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    @FXML
    private ComboBox<Goal> goalComboBox;

    @FXML
    private TextField milestoneNameField;

    @FXML
    private DatePicker targetDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label summaryCategory;

    @FXML
    private Label summaryName;

    @FXML
    private Label summaryDeadline;

    @FXML
    private Label summaryPercent;

    @FXML
    private ProgressBar summaryProgress;

    @FXML
    private Label summaryHint;

    @FXML
    private void initialize() {
        // Show only the goal name in the drop-down instead of Goal.toString().
        goalComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Goal goal) {
                return goal == null ? "" : goal.getName();
            }

            @Override
            public Goal fromString(String text) {
                return null;
            }
        });

        // Presentation only: keeps the goal summary card in sync with the selected goal.
        goalComboBox.valueProperty().addListener((observable, old, goal) -> showGoalSummary(goal));

        goalComboBox.setItems(
                FXCollections.observableArrayList(goalPlanner.getAllGoals())
        );

        statusComboBox.setItems(
                FXCollections.observableArrayList(Milestone.STATUSES)
        );
        statusComboBox.setValue(Milestone.STATUS_NOT_STARTED);
    }

    /** Called by Main right after the page is loaded: pre-selects the goal the user picked in the goal list. */
    public void setGoal(Goal goal) {
        goalComboBox.setValue(goal);
    }

    private void showGoalSummary(Goal goal) {
        boolean has = goal != null;
        summaryName.setText(has ? goal.getName() : "No goal selected");
        summaryCategory.setText(has ? goal.getCategory().toUpperCase() : "GOAL");
        summaryCategory.getStyleClass().removeIf(c -> c.startsWith("pill-cat-") || c.equals("pill-neutral"));
        summaryCategory.getStyleClass().add(has ? GoalListController.categoryStyle(goal.getCategory()) : "pill-neutral");
        summaryDeadline.setText(has ? "Target deadline: " + goal.getTargetDate() : "");
        summaryPercent.setText(has ? goal.getProgress() + "% completed" : "");
        summaryProgress.setProgress(has ? goal.getProgress() / 100.0 : 0);
        summaryHint.setText(has ? "Milestone dates must land on or before " + goal.getTargetDate() : "");
    }

    @FXML
    private void saveMilestone() {
        Goal goal = goalComboBox.getValue();

        if (goal == null) {
            showError("Goal is required.");
            return;
        }

        try {
            goalPlanner.createMilestone(
                    goal.getId(),
                    milestoneNameField.getText(),
                    targetDatePicker.getValue(),
                    statusComboBox.getValue()
            );

            showSuccess("Milestone saved successfully.");
            clearForm();

        } catch (ValidationException | PersistenceException exception) {
            showError(exception.getMessage());
        }
    }

    private void showSuccess(String message) {
        feedbackLabel.setStyle("-fx-text-fill: #2E7D32;");
        feedbackLabel.setText(message);
        cs151.frontend.Motion.toast(message, false);
    }

    private void showError(String message) {
        feedbackLabel.setStyle("-fx-text-fill: #C62828;");
        feedbackLabel.setText(message);
    }

    /** Keeps the selected goal so several milestones can be added in a row. */
    private void clearForm() {
        milestoneNameField.clear();
        targetDatePicker.setValue(null);
        statusComboBox.setValue(Milestone.STATUS_NOT_STARTED);
    }

    @FXML
    private void goBack() throws IOException {
        Main.showGoalListPage();
    }
}
