package cs151.frontend.controller;

import cs151.application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.io.IOException;

public class MilestoneController {

    @FXML
    private TextField milestoneNameField;

    @FXML
    private DatePicker targetDatePicker;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private void initialize() {
        statusComboBox.getItems().addAll(
                "Not Started",
                "In Progress",
                "Completed"
        );
    }

    @FXML
    private void saveMilestone() {
        // Backend saving will be implemented in Task 2.
    }

    @FXML
    private void goBack() throws IOException {
        Main.showGoalListPage();
    }
}
