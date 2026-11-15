package cs151.controller;

import cs151.application.Main;
import cs151.model.Goal;
import cs151.model.GoalPlanner;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
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

        // Pressing Enter inside the search field also performs the search.
        searchField.setOnAction(event -> handleSearch());
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();

        if (query == null || query.isBlank()) {
            resultsTable.getItems().clear();
            resultsTable.setPlaceholder(
                    new Label("Please enter search text.")
            );
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

    @FXML
    private void handleBack() throws IOException {
        Main.showHomePage();
    }
}