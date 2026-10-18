package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.model.Task;
import cs151.backend.service.GoalPlanner;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;

public class TaskListController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    private Milestone milestone;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private TableView<Task> tasksTable;

    @FXML
    private TableColumn<Task, String> descriptionColumn;

    @FXML
    private TableColumn<Task, String> dueDateColumn;

    @FXML
    private TableColumn<Task, String> priorityColumn;

    @FXML
    private TableColumn<Task, String> statusColumn;

    @FXML
    private void initialize() {
        descriptionColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getDescription()));

        dueDateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getDueDate().toString()));

        priorityColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getPriority()));

        statusColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getStatus()));

        tasksTable.setPlaceholder(
                new Label("This milestone has no tasks yet."));
    }

    public void setMilestone(Milestone milestone) {
        this.milestone = milestone;

        titleLabel.setText(milestone.getMilestoneName());

        subtitleLabel.setText(
                "Tasks sorted by target date");

        tasksTable.setItems(
                FXCollections.observableArrayList(
                        goalPlanner.getTasksForMilestone(
                                milestone.getId())));
    }

    @FXML
    private void goBack() throws IOException {
        Goal goal = goalPlanner.getGoal(milestone.getGoalId());
        Main.showMilestoneListPage(goal);
    }
}