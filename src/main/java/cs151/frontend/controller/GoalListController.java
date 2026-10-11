package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.service.GoalPlanner;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Controller of the Goal List page (goal-list-view.fxml). Lists all goals (latest target date first) and lets
 * the user create a milestone for the selected goal or open the Milestone List page of a goal.
 */
public class GoalListController {

    @FXML
    private TableView<Goal> goalsTable;

    @FXML
    private Label messageLabel;

    @FXML
    private TableColumn<Goal, String> nameColumn;

    @FXML
    private TableColumn<Goal, String> descriptionColumn;

    @FXML
    private TableColumn<Goal, String> categoryColumn;

    @FXML
    private TableColumn<Goal, String> targetDateColumn;

    @FXML
    private TableColumn<Goal, String> statusColumn;

    @FXML
    private TableColumn<Goal, Integer> progressColumn;

    @FXML
    private TableColumn<Goal, String> creationDateColumn;

    private final GoalPlanner goalPlanner = new GoalPlanner();

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getName())
        );

        descriptionColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription())
        );

        categoryColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getCategory())
        );

        targetDateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getTargetDate()))
        );

        statusColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus())
        );

        progressColumn.setCellValueFactory(
                cell -> new ReadOnlyIntegerWrapper(
                        cell.getValue().getProgress()
                ).asObject()
        );

        creationDateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getCreationDate()))
        );

        // Double-clicking a goal row opens the list of its milestones.
        goalsTable.setRowFactory(table -> {
            TableRow<Goal> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openMilestoneList(row.getItem());
                }
            });
            return row;
        });

        refreshGoals();
    }

    /** The backend already returns the goals sorted by target date, latest first. */
    @FXML
    private void refreshGoals() {
        goalsTable.setItems(
                FXCollections.observableArrayList(goalPlanner.getAllGoals())
        );
    }

    /** Opens the Milestone page for the selected goal. */
    @FXML
    private void createMilestone() throws IOException {
        Goal selected = goalsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select a goal first.");
            return;
        }

        Main.showMilestonePage(selected);
    }

    /** Opens the Milestone List page for the selected goal. */
    @FXML
    private void viewMilestones() {
        Goal selected = goalsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select a goal first.");
            return;
        }

        openMilestoneList(selected);
    }

    private void openMilestoneList(Goal goal) {
        try {
            Main.showMilestoneListPage(goal);
        } catch (IOException exception) {
            showMessage("Could not open the milestone page.");
        }
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    @FXML
    private void goBack() throws IOException {
        Main.showHomePage();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
