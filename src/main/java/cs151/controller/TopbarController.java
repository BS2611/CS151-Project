package cs151.controller;

import cs151.application.Main;
import cs151.model.Goal;
import cs151.model.GoalPlanner;
import cs151.model.Milestone;
import cs151.model.SearchResults;
import cs151.model.Task;
import cs151.view.Debounce;
import cs151.view.Motion;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import cs151.view.WinFrame;
import cs151.view.WindowChrome;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller of the shared top bar (topbar-view.fxml): shows the breadcrumb and a quick-search box.
 * After a short typing pause a pop-up lists matching Goals, Milestones and Tasks; clicking one opens it directly.
 */
public class TopbarController {

    private static final int PER_KIND = 4;
    private static final double ROW_HEIGHT = 52;

    /** One line of the pop-up: kind badge, text, small detail and what happens on click (null = not clickable). */
    private record Suggestion(String kind, String text, String detail, Runnable open) {
    }

    @FunctionalInterface
    private interface Navigation {
        void go() throws IOException;
    }

    private final GoalPlanner goalPlanner = new GoalPlanner();
    private final ListView<Suggestion> suggestionList = new ListView<>();
    private final Popup popup = new Popup();
    private Debounce suggestPause;

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

        setUpSuggestions();

        // The top bar is the drag handle of the frameless window (nothing happens when there is no window yet).
        WindowChrome chrome = Main.getChrome();
        if (chrome != null && !WindowChrome.isMac()) {
            chrome.makeDraggable(topbar);
        }

        // macOS has its own title bar with traffic lights, so the Windows-style buttons are hidden there.
        boolean mac = WindowChrome.isMac();
        windowButtons.setVisible(!mac);
        windowButtons.setManaged(!mac);

        // Windows: tell the native frame where the draggable area and the maximise button are (snap layouts).
        // (registered even if the hook is not installed yet: the first page is built before the window exists)
        WinFrame.setLayoutNodes(topbar, maximizeButton, List.of(searchField, userChip, minimizeButton, closeButton));
        WinFrame.setStateListener(this::updateMaxIcon);
        Platform.runLater(this::updateMaxIcon);
    }

    /** Builds the quick-search pop-up and wires typing (after a pause), arrow keys and Escape. */
    private void setUpSuggestions() {
        suggestionList.getStyleClass().add("suggest-list");
        suggestionList.setFocusTraversable(false);
        suggestionList.getStylesheets().add(Main.class.getResource("/cs151/view/css/application.css").toExternalForm());
        suggestionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Suggestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                if (!item.kind().isEmpty()) {
                    Label kind = new Label(item.kind());
                    kind.getStyleClass().add("suggest-kind");
                    kind.setMinWidth(76);
                    kind.setAlignment(Pos.CENTER);
                    row.getChildren().add(kind);
                }
                // Title on the first line and the parent (smaller, grey) below it; long text is cut with "...".
                VBox lines = new VBox(1);
                Label text = new Label(item.text());
                text.getStyleClass().add("suggest-text");
                text.setMaxWidth(Double.MAX_VALUE);
                text.setTextOverrun(OverrunStyle.ELLIPSIS);
                lines.getChildren().add(text);
                if (!item.detail().isEmpty()) {
                    Label detail = new Label(item.detail());
                    detail.getStyleClass().add("suggest-detail");
                    detail.setMaxWidth(Double.MAX_VALUE);
                    detail.setTextOverrun(OverrunStyle.ELLIPSIS);
                    lines.getChildren().add(detail);
                }
                HBox.setHgrow(lines, Priority.ALWAYS);
                lines.setMinWidth(0);
                row.getChildren().add(lines);
                setGraphic(row);
                setPrefWidth(0); // the cell follows the list width, so there is no horizontal scroll bar
            }
        });
        suggestionList.setOnMouseClicked(event -> openSelected());
        popup.getContent().add(suggestionList);
        popup.setAutoHide(true);

        // Look up suggestions only after the user pauses typing, not on every key.
        suggestPause = new Debounce(Debounce.SEARCH_DELAY, this::showSuggestions);
        searchField.textProperty().addListener((observable, old, text) -> suggestPause.trigger());

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
                if (popup.isShowing()) {
                    int step = event.getCode() == KeyCode.DOWN ? 1 : -1;
                    int size = suggestionList.getItems().size();
                    int next = Math.floorMod(suggestionList.getSelectionModel().getSelectedIndex() + step, size);
                    suggestionList.getSelectionModel().select(next);
                    suggestionList.scrollTo(next);
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                suggestPause.cancel();
                popup.hide();
            }
        });
    }

    /** Searches through GoalPlanner and shows the matches (a few per kind) under the search box. */
    private void showSuggestions() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim();
        if (text.isEmpty() || searchField.getScene() == null) {
            popup.hide();
            return;
        }

        SearchResults results = goalPlanner.search(text);
        List<Suggestion> items = new ArrayList<>();
        for (Goal goal : results.goals().stream().limit(PER_KIND).toList()) {
            items.add(new Suggestion("Goal", goal.getName(), goal.getCategory(),
                    () -> open(() -> Main.showMilestoneListPage(goal))));
        }
        for (Milestone milestone : results.milestones().stream().limit(PER_KIND).toList()) {
            Goal parent = goalPlanner.getGoal(milestone.getGoalId());
            items.add(new Suggestion("Milestone", milestone.getMilestoneName(), parent.getName(),
                    () -> open(() -> Main.showTaskListPage(milestone))));
        }
        for (Task task : results.tasks().stream().limit(PER_KIND).toList()) {
            Milestone parent = goalPlanner.getMilestone(task.getMilestoneId());
            items.add(new Suggestion("Task", task.getDescription(), parent.getMilestoneName(),
                    () -> open(() -> Main.showTaskListPage(parent))));
        }
        if (items.isEmpty()) {
            items.add(new Suggestion("", "No matches for “" + text + "”", "", null));
        } else {
            items.add(new Suggestion("All", "See all results for “" + text + "”", "",
                    () -> open(() -> Main.showSearchPage(text))));
        }

        suggestionList.getItems().setAll(items);
        suggestionList.getSelectionModel().clearSelection();
        suggestionList.setPrefWidth(Math.max(searchField.getWidth(), 440));
        suggestionList.setPrefHeight(Math.min(items.size(), 9) * ROW_HEIGHT + 12);

        Point2D spot = searchField.localToScreen(0, searchField.getHeight() + 4);
        if (spot == null) {
            return;
        }
        if (popup.isShowing()) {
            popup.setX(spot.getX());
            popup.setY(spot.getY());
        } else {
            popup.show(searchField, spot.getX(), spot.getY());
        }
    }

    private void openSelected() {
        Suggestion selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected != null && selected.open() != null) {
            selected.open().run();
        }
    }

    /** Closes the pop-up and runs the navigation; a page that cannot be opened shows a toast. */
    private void open(Navigation navigation) {
        suggestPause.cancel();
        popup.hide();
        try {
            navigation.go();
        } catch (IOException exception) {
            Motion.toast("Could not open the page.", true);
        }
    }

    /** Enter in the search box: opens the highlighted suggestion, or the Search page with the typed text. */
    @FXML
    private void search() throws IOException {
        if (popup.isShowing() && suggestionList.getSelectionModel().getSelectedItem() != null) {
            openSelected();
            return;
        }
        suggestPause.cancel();
        popup.hide();
        Main.showSearchPage(searchField.getText());
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
