package cs151.frontend.controller;

import cs151.application.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import java.io.IOException;

/** Controller of the Home page (home-view.fxml): navigates to the Goal page and the Goal List page. */
public class HomeController {

    @FXML
    private Label dateChip;

    @FXML
    private void initialize() {
        dateChip.setText("Today, " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)));
    }

    /** Create Goal button: opens the Goal page. */
    @FXML
    private void openGoalPage() throws IOException {
        Main.showGoalPage();
    }

    /** List Goals button: opens the Goal List page. */
    @FXML
    private void openGoalListPage() throws IOException {
        Main.showGoalListPage();
    }
}