package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Milestone;
import cs151.backend.model.ProgressEntry;
import cs151.backend.model.Task;
import cs151.backend.service.GoalPlanner;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;

/**
 * Displays all stored progress entries for one selected task.
 */
public class ProgressListController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    private Task task;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private TableView<ProgressEntry> progressTable;

    @FXML
    private TableColumn<ProgressEntry, String> entryDateColumn;

    @FXML
    private TableColumn<ProgressEntry, String> noteColumn;

    @FXML
    private void initialize() {
        entryDateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getEntryDate().toString()));

        noteColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getNote()));

        progressTable.setPlaceholder(
                new Label("This task has no progress entries yet."));
    }

    /**
     * Receives the task selected on the Task List page.
     */
    public void setTask(Task task) {
        this.task = task;

        titleLabel.setText("Progress for " + task.getDescription());

        subtitleLabel.setText(
                "Progress entries sorted by entry date, ascending");

        refreshEntries();
    }

    /**
     * Loads the selected task's stored progress entries.
     */
    @FXML
    private void refreshEntries() {
        if (task == null) {
            return;
        }

        progressTable.setItems(
                FXCollections.observableArrayList(
                        goalPlanner.getProgressEntriesForTask(
                                task.getId())));
    }

    /**
     * Returns to the Task List for this task's milestone.
     */
    @FXML
    private void goBack() throws IOException {
        Milestone milestone =
                goalPlanner.getMilestone(task.getMilestoneId());

        Main.showTaskListPage(milestone);
    }
}