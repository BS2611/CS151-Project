package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;

public class SearchController {

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
    private void handleSearch() {
        // Search logic will be connected later.
    }

    @FXML
    private void handleBack() throws IOException {
        Main.showHomePage();
    }
}
