package cs151.frontend.controller;

import cs151.application.Main;
import cs151.frontend.WindowChrome;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;

import java.io.IOException;

/** Controller of the shared sidebar (sidebar-view.fxml): navigates to Home or the Goal List and marks the active item. */
public class SidebarController {

    @FXML
    private Button homeNav;

    @FXML
    private Button goalsNav;

    @FXML
    private HBox trafficLights;

    @FXML
    private HBox brandRow;

    @FXML
    private void initialize() {
        (Main.isHomeActive() ? homeNav : goalsNav).getStyleClass().add("nav-item-active");

        // macOS: red / yellow / green traffic lights at the top left, and the sidebar top moves the window.
        if (WindowChrome.isMac()) {
            trafficLights.setVisible(true);
            trafficLights.setManaged(true);
        }
        if (Main.getChrome() != null) {
            Main.getChrome().makeDraggable(brandRow);
            Main.getChrome().makeDraggable(trafficLights);
        }
    }

    @FXML
    private void openHome() throws IOException {
        Main.showHomePage();
    }

    @FXML
    private void openGoals() throws IOException {
        Main.showGoalListPage();
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }

    @FXML
    private void minimizeWindow() {
        if (Main.getChrome() != null) {
            Main.getChrome().minimize();
        }
    }

    @FXML
    private void toggleMaximize() {
        if (Main.getChrome() != null) {
            Main.getChrome().toggleMaximize();
        }
    }
}
