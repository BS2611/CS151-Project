package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import cs151.backend.service.GoalPlanner;
import cs151.frontend.Motion;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Controller of the Goal List page (goal-list-view.fxml). Lists all goals (latest target date first) and lets
 * the user create a milestone for the selected goal or open the Milestone List page of a goal.
 */
public class GoalListController {

    @FXML
    private TableView<Goal> goalsTable;

    @FXML
    private Label messageLabel;

    @FXML
    private Label countLabel;

    @FXML
    private javafx.scene.Node refreshIcon;

    @FXML
    private HBox summaryBox;

    @FXML
    private javafx.scene.layout.VBox toolbarCard;

    @FXML
    private HBox toolbarRow;

    @FXML
    private HBox hintBox;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private javafx.scene.layout.Pane categoryChips;

    @FXML
    private ComboBox<Integer> rowsPerPageCombo;

    @FXML
    private Button previousButton;

    @FXML
    private Button nextButton;

    @FXML
    private TableColumn<Goal, String> milestonesColumn;

    @FXML
    private TableColumn<Goal, String> nameColumn;

    @FXML
    private TableColumn<Goal, String> descriptionColumn;

    @FXML
    private TableColumn<Goal, String> categoryColumn;

    @FXML
    private TableColumn<Goal, String> targetDateColumn;

    @FXML
    private TableColumn<Goal, String> statusColumn;

    @FXML
    private TableColumn<Goal, Integer> progressColumn;

    @FXML
    private TableColumn<Goal, String> actionsColumn;

    private static final String ALL_STATUSES = "All Statuses";
    private static final String ALL_CATEGORIES = "All Categories";
    private static final String SORT_LATEST = "Target Date (Latest first)";
    private static final String SORT_SOONEST = "Target Date (Soonest first)";
    private static final String SORT_NAME = "Name (A-Z)";
    private static final String SORT_PROGRESS = "Progress (High to low)";

    private static final String HINT = "Select a goal, then add or view its milestones. Double-click a row to open its milestones.";

    private final GoalPlanner goalPlanner = new GoalPlanner();
    private javafx.animation.PauseTransition hintReset;

    // Presentation state: the filters and the page only decide which goals the table shows.
    private List<Goal> allGoals = List.of();
    private List<Goal> filteredGoals = List.of();
    private String selectedCategory = ALL_CATEGORIES;
    private int pageIndex = 0;

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getName())
        );

        descriptionColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription())
        );

        categoryColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getCategory())
        );

        targetDateColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getTargetDate()))
        );

        statusColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus())
        );

        progressColumn.setCellValueFactory(
                cell -> new ReadOnlyIntegerWrapper(
                        cell.getValue().getProgress()
                ).asObject()
        );

        milestonesColumn.setCellValueFactory(cell -> {
            List<Milestone> milestones = goalPlanner.getMilestonesForGoal(cell.getValue().getId());
            long done = milestones.stream().filter(m -> Milestone.STATUS_COMPLETED.equals(m.getStatus())).count();
            return new ReadOnlyStringWrapper(done + "/" + milestones.size());
        });

        actionsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(""));

        // Presentation only: goal name with its description, pills for category and status, a progress bar.
        nameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                Goal goal = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (goal == null) {
                    setGraphic(null);
                    return;
                }
                Label title = new Label(name);
                title.getStyleClass().add("cell-title");
                Label description = new Label(goal.getDescription() == null ? "" : goal.getDescription());
                description.getStyleClass().add("cell-sub");
                description.setMaxWidth(240);
                VBox box = new VBox(2, title, description);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        categoryColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                setText(null);
                setGraphic(empty || category == null ? null : pill(category, categoryStyle(category)));
            }
        });
        milestonesColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String counts, boolean empty) {
                super.updateItem(counts, empty);
                setText(null);
                if (empty || counts == null) {
                    setGraphic(null);
                    return;
                }
                String[] parts = counts.split("/");
                int done = Integer.parseInt(parts[0]);
                int total = Integer.parseInt(parts[1]);
                boolean all = total > 0 && done == total;
                Label label = new Label(total == 0 ? "None yet" : done + " / " + total + " done");
                label.setGraphic(new FontIcon(all ? "fas-check-circle" : "far-check-circle"));
                label.setGraphicTextGap(6);
                label.getStyleClass().addAll("pill", "milestone-pill", all ? "milestone-pill-ok" : "milestone-pill-open");
                setGraphic(label);
            }
        });
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button menuButton = new Button();

            {
                menuButton.setGraphic(new FontIcon("fas-ellipsis-h"));
                menuButton.getStyleClass().add("icon-button");
                menuButton.setFocusTraversable(false);
                menuButton.setOnAction(event -> {
                    Goal goal = getTableRow() == null ? null : getTableRow().getItem();
                    if (goal != null) {
                        getTableView().getSelectionModel().select(getIndex());
                        showActionsMenu(menuButton, goal);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty ? null : menuButton);
                setAlignment(Pos.CENTER);
            }
        });
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                setGraphic(empty || status == null ? null : statusPill(status));
            }
        });
        targetDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                Goal goal = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (goal == null || goal.getTargetDate() == null) {
                    setGraphic(null);
                    return;
                }
                long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
                boolean soon = days <= 14 && !"Completed".equals(goal.getStatus());
                FontIcon calendar = new FontIcon(soon ? "far-clock" : "far-calendar-alt");
                calendar.getStyleClass().add(soon ? "date-icon-warn" : "date-icon");
                Label dateLabel = new Label(date);
                dateLabel.getStyleClass().add(soon ? "cell-title-warn" : "cell-title");
                HBox top = new HBox(8, calendar, dateLabel);
                top.setAlignment(Pos.CENTER_LEFT);
                String text = "Completed".equals(goal.getStatus()) ? "Completed"
                        : days >= 0 ? days + " days remaining" : Math.abs(days) + " days overdue";
                Label sub = new Label(text);
                sub.getStyleClass().add(soon ? "cell-sub-warn" : "cell-sub");
                VBox box = new VBox(2, top, sub);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        progressColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer progress, boolean empty) {
                super.updateItem(progress, empty);
                setText(null);
                if (empty || progress == null) {
                    setGraphic(null);
                    return;
                }
                boolean done = progress >= 100;
                Label caption = new Label(done ? "All milestones met" : "Complete");
                caption.getStyleClass().add("cell-sub");
                caption.setMinWidth(0);
                Label percent = new Label(progress + "%");
                percent.getStyleClass().addAll("progress-percent", done ? "progress-percent-ok" : "progress-percent-main");
                percent.setMinWidth(Label.USE_PREF_SIZE);
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                HBox labels = new HBox(6, caption, spacer, percent);
                labels.setAlignment(Pos.CENTER_LEFT);
                ProgressBar bar = new ProgressBar(progress / 100.0);
                bar.setMaxWidth(Double.MAX_VALUE);
                if (done) {
                    bar.getStyleClass().add("bar-ok");
                }
                VBox box = new VBox(5, labels, bar);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        // Double-clicking a goal row opens the list of its milestones.
        goalsTable.setRowFactory(table -> {
            TableRow<Goal> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openMilestoneList(row.getItem());
                }
            });
            return row;
        });

        setUpFilters();

        String pending = Main.takePendingSearch();
        if (pending != null) {
            searchField.setText(pending);
        }

        // Narrow windows: hide the less important parts instead of clipping them.
        goalsTable.sceneProperty().addListener((observable, old, scene) -> {
            if (scene != null) {
                scene.widthProperty().addListener((o, oldWidth, width) -> adaptToWidth(width.doubleValue()));
                adaptToWidth(scene.getWidth());
            }
        });

        refreshGoals();
    }

    /** Fills the search box, drop-downs and category chips and re-applies the filters whenever one changes. */
    private void setUpFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
                ALL_STATUSES, "Not Started", "In Progress", "On Hold", "Completed", "Abandoned"));
        statusFilter.setValue(ALL_STATUSES);

        sortCombo.setItems(FXCollections.observableArrayList(SORT_LATEST, SORT_SOONEST, SORT_NAME, SORT_PROGRESS));
        sortCombo.setValue(SORT_LATEST);

        rowsPerPageCombo.setItems(FXCollections.observableArrayList(5, 10, 25));
        rowsPerPageCombo.setValue(10);

        ToggleGroup group = new ToggleGroup();
        for (String category : new String[]{ALL_CATEGORIES, "Academic", "Career", "Health", "Financial", "Personal", "Other"}) {
            ToggleButton chip = new ToggleButton(category);
            chip.getStyleClass().add("chip");
            chip.setToggleGroup(group);
            chip.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            chip.setSelected(ALL_CATEGORIES.equals(category));
            categoryChips.getChildren().add(chip);
        }
        group.selectedToggleProperty().addListener((observable, old, selected) -> {
            if (selected == null) {
                old.setSelected(true); // one chip always stays selected
                return;
            }
            selectedCategory = ((ToggleButton) selected).getText();
            pageIndex = 0;
            applyFilters();
        });

        searchField.textProperty().addListener((observable, old, text) -> {
            pageIndex = 0;
            applyFilters();
        });
        statusFilter.valueProperty().addListener((observable, old, value) -> {
            pageIndex = 0;
            applyFilters();
        });
        sortCombo.valueProperty().addListener((observable, old, value) -> {
            pageIndex = 0;
            applyFilters();
        });
        rowsPerPageCombo.valueProperty().addListener((observable, old, value) -> {
            pageIndex = 0;
            showPage();
        });

        goalsTable.setPlaceholder(new Label("No goals match your filters."));
    }

    private void adaptToWidth(double width) {
        boolean wide = width >= 1150;
        summaryBox.setVisible(wide);
        summaryBox.setManaged(wide);
        milestonesColumn.setVisible(width >= 950);

        // Narrow window: the hint / error message gets its own line above the buttons instead of being squeezed.
        boolean narrow = width < 1050;
        boolean separate = !toolbarRow.getChildren().contains(hintBox);
        if (narrow && !separate) {
            toolbarRow.getChildren().remove(hintBox);
            toolbarCard.getChildren().add(toolbarCard.getChildren().indexOf(toolbarRow), hintBox);
        } else if (!narrow && separate) {
            toolbarCard.getChildren().remove(hintBox);
            toolbarRow.getChildren().add(0, hintBox);
        }
    }

    /** Applies the search text, status, category and sort order to all goals, then shows the current page. */
    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        Comparator<Goal> order = switch (sortCombo.getValue()) {
            case SORT_SOONEST -> Comparator.comparing(Goal::getTargetDate);
            case SORT_NAME -> Comparator.comparing(goal -> goal.getName().toLowerCase());
            case SORT_PROGRESS -> Comparator.comparingInt(Goal::getProgress).reversed();
            default -> Comparator.comparing(Goal::getTargetDate).reversed();
        };

        filteredGoals = allGoals.stream()
                .filter(goal -> query.isEmpty()
                        || goal.getName().toLowerCase().contains(query)
                        || (goal.getDescription() != null && goal.getDescription().toLowerCase().contains(query))
                        || goal.getCategory().toLowerCase().contains(query))
                .filter(goal -> ALL_STATUSES.equals(status) || goal.getStatus().equals(status))
                .filter(goal -> ALL_CATEGORIES.equals(selectedCategory) || goal.getCategory().equals(selectedCategory))
                .sorted(order)
                .toList();

        showPage();
    }

    /** Puts the current page of the filtered goals into the table and updates the footer. */
    private void showPage() {
        int size = rowsPerPageCombo.getValue() == null ? 10 : rowsPerPageCombo.getValue();
        int pages = Math.max(1, (int) Math.ceil(filteredGoals.size() / (double) size));
        pageIndex = Math.min(pageIndex, pages - 1);

        int from = pageIndex * size;
        int to = Math.min(from + size, filteredGoals.size());

        goalsTable.setItems(FXCollections.observableArrayList(filteredGoals.subList(from, to)));
        countLabel.setText(filteredGoals.isEmpty()
                ? "Showing 0 of " + allGoals.size() + " goals"
                : "Showing " + (from + 1) + "-" + to + " of " + filteredGoals.size() + " goals");
        // The table is exactly as tall as its rows, so the whole page scrolls instead of the table.
        double height = 48 + Math.max(goalsTable.getItems().size(), 3) * goalsTable.getFixedCellSize();
        goalsTable.setMinHeight(height);
        goalsTable.setPrefHeight(height);
        goalsTable.setMaxHeight(height);
        previousButton.setDisable(pageIndex == 0);
        nextButton.setDisable(pageIndex >= pages - 1);
        javafx.application.Platform.runLater(() -> goalsTable.scrollTo(0));
    }

    @FXML
    private void previousPage() {
        pageIndex--;
        showPage();
    }

    @FXML
    private void nextPage() {
        pageIndex++;
        showPage();
    }

    /** The backend already returns the goals sorted by target date, latest first. */
    @FXML
    private void refreshGoals() {
        allGoals = goalPlanner.getAllGoals();
        showSummary();
        applyFilters();
    }

    /** Counts goals per status for the summary strip at the top of the page. */
    private void showSummary() {
        summaryBox.getChildren().clear();
        for (String status : new String[]{"In Progress", "Not Started", "Completed"}) {
            long count = allGoals.stream().filter(g -> status.equals(g.getStatus())).count();
            Label dot = new Label();
            dot.getStyleClass().addAll("dot", "dot-" + statusStyle(status).substring(5));
            Label number = new Label(String.valueOf(count));
            number.getStyleClass().add("stat-number");
            Label name = new Label(status);
            name.getStyleClass().add("stat-name");
            HBox stat = new HBox(8, dot, number, name);
            stat.setAlignment(Pos.CENTER_LEFT);
            stat.getStyleClass().add("stat");
            if (!summaryBox.getChildren().isEmpty()) {
                javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
                divider.getStyleClass().add("stat-divider");
                summaryBox.getChildren().add(divider);
            }
            summaryBox.getChildren().add(stat);
        }
    }

    /** The "..." menu of a row: view details, edit, milestones and delete. */
    private void showActionsMenu(Button anchor, Goal goal) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("actions-menu");

        MenuItem view = menuItem("View details", "fas-eye", false);
        view.setOnAction(event -> GoalDialogs.showDetails(goal, goalPlanner.getMilestonesForGoal(goal.getId()),
                () -> openEditPage(goal), () -> openMilestoneList(goal)));

        MenuItem edit = menuItem("Edit goal", "fas-pen", false);
        edit.setOnAction(event -> openEditPage(goal));

        MenuItem milestones = menuItem("Milestones", "fas-list-ul", false);
        milestones.setOnAction(event -> openMilestoneList(goal));

        MenuItem delete = menuItem("Delete goal", "fas-trash", true);
        delete.setOnAction(event -> GoalDialogs.confirmDelete(goal,
                goalPlanner.getMilestonesForGoal(goal.getId()).size(), () -> deleteGoal(goal)));

        menu.getItems().addAll(view, edit, milestones, new SeparatorMenuItem(), delete);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private static MenuItem menuItem(String text, String icon, boolean danger) {
        MenuItem item = new MenuItem(text);
        FontIcon graphic = new FontIcon(icon);
        graphic.getStyleClass().add(danger ? "danger-icon" : "menu-icon");
        item.setGraphic(graphic);
        if (danger) {
            item.getStyleClass().add("danger-item");
        }
        return item;
    }

    private void openEditPage(Goal goal) {
        try {
            Main.showEditGoalPage(goal);
        } catch (IOException exception) {
            showMessage("Could not open the edit page.");
        }
    }

    private void deleteGoal(Goal goal) {
        try {
            goalPlanner.deleteGoal(goal.getId());
            refreshGoals();
            Motion.toast("Goal deleted", false);
        } catch (RuntimeException exception) {
            // not found / database errors are shown like the other messages on this page
            showMessage(exception.getMessage());
        }
    }

    /** Opens the Create Goal page. */
    @FXML
    private void createGoal() throws IOException {
        Main.showGoalPage();
    }

    /** A table cell that shows its text as a coloured pill. */
    private static TableCell<Goal, String> pillCell(String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                setText(null);
                setGraphic(empty || text == null ? null : pill(text, styleClass));
            }
        };
    }

    static Label pill(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("pill", styleClass);
        return label;
    }

    /** A status pill with a small coloured dot in front, like the reference design. */
    static Label statusPill(String status) {
        Label label = pill(status, statusStyle(status));
        javafx.scene.layout.Region dot = new javafx.scene.layout.Region();
        dot.getStyleClass().add("pill-dot");
        label.setGraphic(dot);
        label.setGraphicTextGap(6);
        return label;
    }

    /** Category colours: light blue, light indigo, light green, light amber, light gray, neutral gray. */
    static String categoryStyle(String category) {
        return switch (category) {
            case "Academic" -> "pill-cat-academic";
            case "Career" -> "pill-cat-career";
            case "Health" -> "pill-cat-health";
            case "Financial" -> "pill-cat-financial";
            case "Personal" -> "pill-cat-personal";
            default -> "pill-cat-other";
        };
    }

    static String statusStyle(String status) {
        return switch (status) {
            case "Completed" -> "pill-ok";
            case "In Progress" -> "pill-progress";
            case "On Hold" -> "pill-warn";
            case "Abandoned" -> "pill-danger";
            default -> "pill-muted";
        };
    }

    /** Opens the Milestone page for the selected goal. */
    @FXML
    private void createMilestone() throws IOException {
        Goal selected = goalsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select a goal first.");
            return;
        }

        Main.showMilestonePage(selected);
    }

    /** Opens the Milestone List page for the selected goal. */
    @FXML
    private void viewMilestones() {
        Goal selected = goalsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showMessage("Select a goal first.");
            return;
        }

        openMilestoneList(selected);
    }

    private void openMilestoneList(Goal goal) {
        try {
            Main.showMilestoneListPage(goal);
        } catch (IOException exception) {
            showMessage("Could not open the milestone page.");
        }
    }

    /** Shows the message on the left of the toolbar buttons (always visible), shakes the table and shows a toast. */
    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().remove("feedback-error");
        messageLabel.getStyleClass().add("feedback-error");
        Motion.shake(goalsTable);
        Motion.toast(message, true);

        if (hintReset != null) {
            hintReset.stop();
        }
        hintReset = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3.5));
        hintReset.setOnFinished(event -> {
            messageLabel.getStyleClass().remove("feedback-error");
            messageLabel.setText(HINT);
        });
        hintReset.play();
    }

    /** Refresh button: reloads the goals with a spinning icon, a quick fade of the table and a toast. */
    @FXML
    private void refreshClicked() {
        Motion.spin(refreshIcon);
        refreshGoals();
        Motion.refreshFade(goalsTable);
        Motion.toast("Goals refreshed", false);
    }

    @FXML
    private void goBack() throws IOException {
        Main.showHomePage();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
