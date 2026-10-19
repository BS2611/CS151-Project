package cs151.frontend.controller;

import cs151.application.Main;
import javafx.fxml.FXML;
import cs151.frontend.WinFrame;
import cs151.frontend.WindowChrome;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import java.util.List;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

/** Controller of the shared top bar (topbar-view.fxml): shows the breadcrumb and searches goals. */
public class TopbarController {

    @FXML
    private HBox crumbBox;

    @FXML
    private TextField searchField;

    @FXML
    private HBox topbar;

    @FXML
    private HBox userChip;

    @FXML
    private HBox windowButtons;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    @FXML
    private Button closeButton;

    @FXML
    private Region maxIcon;

    @FXML
    private void initialize() {
        // Breadcrumb: "Goals  ›  New Goal" is drawn as labels with a chevron icon between them.
        String[] parts = Main.getBreadcrumb().split("\u203A");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                crumbBox.getChildren().add(new FontIcon("fas-chevron-right"));
                crumbBox.getChildren().get(crumbBox.getChildren().size() - 1).getStyleClass().add("crumb-sep");
            }
            Label part = new Label(parts[i].trim());
            part.getStyleClass().add(i == parts.length - 1 ? "crumb" : "crumb-parent");
            crumbBox.getChildren().add(part);
        }

        // The top bar is the drag handle of the frameless window (nothing happens when there is no window yet).
        WindowChrome chrome = Main.getChrome();
        if (chrome != null) {
            chrome.makeDraggable(topbar);
        }

        // macOS draws its own traffic lights in the sidebar, so the Windows-style buttons are hidden there.
        boolean mac = WindowChrome.isMac();
        windowButtons.setVisible(!mac);
        windowButtons.setManaged(!mac);

        // Windows: tell the native frame where the draggable area and the maximise button are (snap layouts).
        // (registered even if the hook is not installed yet: the first page is built before the window exists)
        WinFrame.setLayoutNodes(topbar, maximizeButton, List.of(searchField, userChip, minimizeButton, closeButton));
        WinFrame.setStateListener(this::updateMaxIcon);
        Platform.runLater(this::updateMaxIcon);
    }

    /** Enter in the search box: opens the Goal List with the typed text already in its search field. */
    @FXML
    private void search() throws IOException {
        Main.showGoalListPage(searchField.getText());
    }

    @FXML
    private void minimize() {
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

    @FXML
    private void close() {
        Platform.exit();
    }

    /** Shows the "restore" icon while the window is maximised and the "maximise" icon otherwise. */
    private void updateMaxIcon() {
        boolean maximized = WinFrame.isMaximized();
        maxIcon.getStyleClass().remove("icon-restore");
        if (maximized) {
            maxIcon.getStyleClass().add("icon-restore");
        }
    }
}
