package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.service.GoalPlanner;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
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

        milestonesTable.setPlaceholder(new Label("This goal has no milestones yet."));
    }

    /** Called by Main right after the page is loaded: remembers the goal and fills the table. */
    public void setGoal(Goal goal) {
        this.goal = goal;
        titleLabel.setText("Milestones of: " + goal.getName());
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
    }

    @FXML
    private void createMilestone() throws IOException {
        Main.showMilestonePage(goal);
    }

    @FXML
    private void goBack() throws IOException {
        Main.showGoalListPage();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
