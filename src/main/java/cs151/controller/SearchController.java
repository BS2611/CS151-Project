package cs151.controller;

import cs151.application.Main;
import cs151.model.Goal;
import cs151.model.GoalPlanner;
import cs151.view.Debounce;
import cs151.view.Motion;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.List;

public class SearchController {

    private final GoalPlanner goalPlanner = new GoalPlanner();

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Goal> resultsTable;

    @FXML
    private TableColumn<Goal, String> goalColumn;

    @FXML
    private TableColumn<Goal, String> categoryColumn;

    @FXML
    private TableColumn<Goal, String> statusColumn;

    @FXML
    private void initialize() {
        goalColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getName()
                )
        );

        categoryColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getCategory()
                )
        );

        statusColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getStatus()
                )
        );

        resultsTable.setPlaceholder(
                new Label("Enter text to search Goals, Milestones, and Tasks.")
        );

        // Double-clicking a result opens the Milestone list of that Goal (same as the Goal List page).
        resultsTable.setRowFactory(table -> {
            TableRow<Goal> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openMilestoneList(row.getItem());
                }
            });
            return row;
        });

        // Pressing Enter inside the search field searches right away.
        searchField.setOnAction(event -> handleSearch());

        // While typing, search only after a pause (not on every key).
        searchPause = new Debounce(Debounce.SEARCH_DELAY, () -> runSearch(false));
        searchField.textProperty().addListener((observable, old, text) -> searchPause.trigger());

        // Coming from the top bar: show the results for the text typed there.
        String pending = Main.takePendingGlobalSearch();
        if (pending != null) {
            searchField.setText(pending);
            searchPause.cancel();
            runSearch(true);
        }
    }

    private Debounce searchPause;

    /** Search button / Enter: searches immediately and warns about blank text. */
    @FXML
    private void handleSearch() {
        searchPause.cancel();
        runSearch(true);
    }

    /** @param explicit true when the user asked for it (Enter / button), false for the search-as-you-type pause */
    private void runSearch(boolean explicit) {
        String query = searchField.getText();

        if (query == null || query.isBlank()) {
            resultsTable.getItems().clear();
            resultsTable.setPlaceholder(new Label(explicit
                    ? "Please enter search text."
                    : "Enter text to search Goals, Milestones, and Tasks."));
            return;
        }

        List<Goal> goals = goalPlanner.searchRelatedGoals(query);

        resultsTable.setItems(
                FXCollections.observableArrayList(goals)
        );

        if (goals.isEmpty()) {
            resultsTable.setPlaceholder(
                    new Label("No matching Goals found.")
            );
        }
    }

    /** View Milestones button: opens the Milestone list of the selected search result. */
    @FXML
    private void handleViewMilestones() {
        Goal selected = resultsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            Motion.toast("Select a goal first.", false);
            return;
        }

        openMilestoneList(selected);
    }

    private void openMilestoneList(Goal goal) {
        try {
            Main.showMilestoneListPage(goal);
        } catch (IOException exception) {
            Motion.toast("Could not open the milestone page.", true);
        }
    }

    @FXML
    private void handleBack() throws IOException {
        Main.showHomePage();
    }
}