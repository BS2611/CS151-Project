package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.service.GoalPlanner;
import cs151.frontend.Motion;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Controller of the Milestone List page (milestone-list-view.fxml). Shows all stored milestones of one goal in a
 * TableView; the backend already returns them with the latest target date first.
 */
public class MilestoneListController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    private Goal goal;

    @FXML
    private Label titleLabel;

    @FXML
    private javafx.scene.Node refreshIcon;

    @FXML
    private Label subtitleLabel;

    @FXML
    private TableView<Milestone> milestonesTable;

    @FXML
    private TableColumn<Milestone, String> nameColumn;

    @FXML
    private TableColumn<Milestone, String> targetDateColumn;

    @FXML
    private TableColumn<Milestone, String> statusColumn;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getMilestoneName())
        );

        targetDateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getTargetDate()))
        );

        statusColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus())
        );

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                setGraphic(empty || status == null ? null : GoalListController.statusPill(status));
            }
        });

        milestonesTable.setPlaceholder(new Label("This goal has no milestones yet."));
    }

    /** Called by Main right after the page is loaded: remembers the goal and fills the table. */
    public void setGoal(Goal goal) {
        this.goal = goal;
        titleLabel.setText(goal.getName());
        subtitleLabel.setText(goal.getCategory() + "  •  Target " + goal.getTargetDate()
                + "  •  " + goal.getProgress() + "% complete  •  Milestones, latest target date first");
        refreshMilestones();
    }

    @FXML
    private void refreshMilestones() {
        if (goal == null) {
            return;
        }

        milestonesTable.setItems(
                FXCollections.observableArrayList(goalPlanner.getMilestonesForGoal(goal.getId()))
        );

        // The table is exactly as tall as its rows instead of leaving a big empty white card.
        double height = 48 + Math.max(milestonesTable.getItems().size(), 3) * milestonesTable.getFixedCellSize();
        milestonesTable.setMinHeight(height);
        milestonesTable.setPrefHeight(height);
        milestonesTable.setMaxHeight(height);
    }

    /** Refresh button: reloads the milestones with a spinning icon, a quick fade of the table and a toast. */
    @FXML
    private void refreshClicked() {
        Motion.spin(refreshIcon);
        refreshMilestones();
        Motion.refreshFade(milestonesTable);
        Motion.toast("Milestones refreshed", false);
    }

    @FXML
    private void createMilestone() throws IOException {
        Main.showMilestonePage(goal);
    }

    @FXML
    private void createTask() throws IOException {
        Milestone selectedMilestone =
                milestonesTable.getSelectionModel().getSelectedItem();

        if (selectedMilestone == null) {
            Motion.toast("Please select a milestone first.", false);
            return;
        }

        Main.showTaskPage(selectedMilestone);
    }

    @FXML
    private void goBack() throws IOException {
        Main.showGoalListPage();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
