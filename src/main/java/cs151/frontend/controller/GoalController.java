package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.exception.PersistenceException;
import cs151.backend.exception.ValidationException;
import cs151.backend.model.Goal;
import cs151.backend.service.GoalPlanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Controller of the Goal page (goal-view.fxml). Reads the form, lets {@link GoalPlanner} validate and store
 * the goal, and shows a success or error message.
 */
public class GoalController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    @FXML
    private TextField goalNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private DatePicker targetDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextField progressField;

    @FXML
    private TextField creationDateField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label pageTitle;

    @FXML
    private javafx.scene.control.Button backLink;

    @FXML
    private javafx.scene.control.Button createGoalButton;

    /** The goal being edited, or null while the page creates a new goal. */
    private Goal editingGoal;

    @FXML
    private void initialize() {
        categoryComboBox.setItems(
                FXCollections.observableArrayList(Goal.CATEGORIES)
        );

        statusComboBox.setItems(
                FXCollections.observableArrayList(Goal.STATUSES)
        );

        progressField.setText("0");
        statusComboBox.setValue(Goal.STATUS_NOT_STARTED);
        creationDateField.setText(LocalDate.now().toString());
    }

    /** Called by Main when the page is opened from the Actions menu: fills the form with the goal and edits it. */
    public void setGoal(Goal goal) {
        editingGoal = goal;
        pageTitle.setText("Edit goal");
        backLink.setText("Back to Goals");
        createGoalButton.setText("Save changes");
        goalNameField.setText(goal.getName());
        descriptionArea.setText(goal.getDescription());
        categoryComboBox.setValue(goal.getCategory());
        targetDatePicker.setValue(goal.getTargetDate());
        statusComboBox.setValue(goal.getStatus());
        progressField.setText(String.valueOf(goal.getProgress()));
        creationDateField.setText(String.valueOf(goal.getCreationDate()));
    }

    @FXML
    private void createGoal() {
        String goalName = goalNameField.getText();
        String description = descriptionArea.getText();
        String category = categoryComboBox.getValue();
        LocalDate targetDate = targetDatePicker.getValue();
        String status = statusComboBox.getValue();

        int progress;

        try {
            progress = Integer.parseInt(progressField.getText().trim());
        } catch (NumberFormatException exception) {
            showError("Progress must be a whole number.");
            return;
        }

        try {
            if (editingGoal != null) {
                goalPlanner.updateGoal(editingGoal.getId(), goalName, description, category, targetDate, status, progress);
                cs151.frontend.Motion.toast("Goal updated successfully", false);
                Main.showGoalListPage();
                return;
            }

            goalPlanner.createGoal(
                    goalName,
                    description,
                    category,
                    targetDate,
                    status,
                    progress
            );

            feedbackLabel.setStyle("-fx-text-fill: #2E7D32;");
            feedbackLabel.setText("Goal created successfully.");
            cs151.frontend.Motion.toast("Goal created successfully", false);

            clearForm();

        } catch (java.io.IOException exception) {
            showError("Could not open the goal list.");
        } catch (ValidationException | PersistenceException exception) {
            // Business-rule violations and failed database operations are both shown to the user.
            showError(exception.getMessage());
        }
    }

    private void showError(String message) {
        feedbackLabel.setStyle("-fx-text-fill: #C62828;");
        feedbackLabel.setText(message);
    }

    private void clearForm() {
        goalNameField.clear();
        descriptionArea.clear();
        categoryComboBox.setValue(null);
        targetDatePicker.setValue(null);
        statusComboBox.setValue(Goal.STATUS_NOT_STARTED);
        progressField.setText("0");
        creationDateField.setText(LocalDate.now().toString());
    }

    @FXML
    private void goBack() throws IOException {
        if (editingGoal != null) {
            Main.showGoalListPage();
            return;
        }
        Main.showHomePage();
    }
}