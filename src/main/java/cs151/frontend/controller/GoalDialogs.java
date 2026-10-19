package cs151.frontend.controller;

import cs151.application.Main;
import cs151.backend.model.Goal;
import cs151.backend.model.Milestone;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** The two pop-up cards of the Goals page: goal details (read only) and the delete confirmation. */
final class GoalDialogs {

    private GoalDialogs() {
    }

    /** Read-only details of a goal with its milestones; buttons lead to editing or to the milestone list. */
    static void showDetails(Goal goal, List<Milestone> milestones, Runnable onEdit, Runnable onViewMilestones) {
        Button close = iconButton("fas-times");
        close.setOnAction(event -> Main.hideOverlay());

        Label name = new Label(goal.getName());
        name.getStyleClass().add("dialog-title");
        name.setWrapText(true);

        HBox pills = new HBox(8,
                GoalListController.pill(goal.getCategory(), GoalListController.categoryStyle(goal.getCategory())),
                GoalListController.statusPill(goal.getStatus()));
        pills.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(8, pills, name);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        HBox header = new HBox(12, titleBox, close);
        header.setAlignment(Pos.TOP_LEFT);

        String descriptionText = goal.getDescription() == null || goal.getDescription().isBlank()
                ? "No description." : goal.getDescription();
        Label description = new Label(descriptionText);
        description.getStyleClass().add("page-subtitle");
        description.setWrapText(true);

        // target date, created, progress
        long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        String remaining = "Completed".equals(goal.getStatus()) ? "Completed"
                : days >= 0 ? days + " days remaining" : Math.abs(days) + " days overdue";
        VBox target = infoBlock("TARGET DATE", goal.getTargetDate().toString(), remaining);
        VBox created = infoBlock("CREATED", goal.getCreationDate() == null ? "" : goal.getCreationDate().toString(), "");

        Label percent = new Label(goal.getProgress() + "%");
        boolean done = goal.getProgress() >= 100;
        percent.getStyleClass().addAll("progress-percent", done ? "progress-percent-ok" : "progress-percent-main");
        ProgressBar bar = new ProgressBar(goal.getProgress() / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        if (done) {
            bar.getStyleClass().add("bar-ok");
        }
        Label progressCaption = new Label("PROGRESS");
        progressCaption.getStyleClass().add("eyebrow");
        VBox progress = new VBox(6, progressCaption, percent, bar);
        HBox.setHgrow(progress, Priority.ALWAYS);

        HBox facts = new HBox(28, target, created, progress);
        facts.getStyleClass().add("dialog-facts");

        // milestones
        long doneCount = milestones.stream().filter(m -> Milestone.STATUS_COMPLETED.equals(m.getStatus())).count();
        Label milestoneTitle = new Label("Milestones  (" + doneCount + " / " + milestones.size() + " done)");
        milestoneTitle.getStyleClass().add("section-title");

        VBox list = new VBox(8);
        if (milestones.isEmpty()) {
            Label none = new Label("This goal has no milestones yet.");
            none.getStyleClass().add("helper-text");
            list.getChildren().add(none);
        }
        for (Milestone milestone : milestones) {
            boolean complete = Milestone.STATUS_COMPLETED.equals(milestone.getStatus());
            FontIcon icon = new FontIcon(complete ? "fas-check-circle" : "far-circle");
            icon.getStyleClass().add(complete ? "stored-icon" : "date-icon");
            Label milestoneName = new Label(milestone.getMilestoneName());
            milestoneName.getStyleClass().add("cell-title");
            Label date = new Label(milestone.getTargetDate().toString());
            date.getStyleClass().add("cell-sub");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(10, icon, milestoneName, spacer, date, GoalListController.statusPill(milestone.getStatus()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dialog-row");
            list.getChildren().add(row);
        }
        javafx.scene.control.ScrollPane listScroll = new javafx.scene.control.ScrollPane(list);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        listScroll.getStyleClass().add("dialog-scroll");
        listScroll.setPrefViewportHeight(Math.min(200, 44 * Math.max(1, milestones.size())));

        // buttons
        Button edit = textButton("Edit goal", "fas-pen", "secondary-button");
        edit.setOnAction(event -> {
            Main.hideOverlay();
            onEdit.run();
        });
        Button viewMilestones = textButton("View Milestones", "fas-list-ul", "secondary-button");
        viewMilestones.setOnAction(event -> {
            Main.hideOverlay();
            onViewMilestones.run();
        });
        Button closeButton = textButton("Close", null, "primary-button");
        closeButton.setOnAction(event -> Main.hideOverlay());
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(10, edit, viewMilestones, footerSpacer, closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(18, header, description, facts, milestoneTitle, listScroll, footer);
        card.getStyleClass().add("dialog-card");
        card.setMaxWidth(620);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        Main.showOverlay(card);
    }

    /** "Delete this goal?" confirmation; {@code onConfirm} runs only if the user presses Delete. */
    static void confirmDelete(Goal goal, int milestoneCount, Runnable onConfirm) {
        FontIcon trash = new FontIcon("fas-trash");
        trash.getStyleClass().add("danger-icon");
        Label badge = new Label();
        badge.setGraphic(trash);
        badge.getStyleClass().add("danger-badge");

        Label title = new Label("Delete this goal?");
        title.getStyleClass().add("dialog-title");

        String extra = milestoneCount == 0 ? "" : " and its " + milestoneCount + (milestoneCount == 1 ? " milestone" : " milestones");
        Label text = new Label("\"" + goal.getName() + "\"" + extra + " will be permanently deleted. This cannot be undone.");
        text.getStyleClass().add("page-subtitle");
        text.setWrapText(true);

        Button cancel = textButton("Cancel", null, "secondary-button");
        cancel.setOnAction(event -> Main.hideOverlay());
        Button delete = textButton("Delete goal", "fas-trash", "danger-button");
        delete.setOnAction(event -> {
            Main.hideOverlay();
            onConfirm.run();
        });
        HBox buttons = new HBox(10, cancel, delete);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(8, title, text);
        HBox top = new HBox(16, badge, body);
        top.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(body, Priority.ALWAYS);

        VBox card = new VBox(22, top, buttons);
        card.getStyleClass().add("dialog-card");
        card.setMaxWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        Main.showOverlay(card);
    }

    private static VBox infoBlock(String caption, String value, String sub) {
        Label c = new Label(caption);
        c.getStyleClass().add("eyebrow");
        Label v = new Label(value);
        v.getStyleClass().add("cell-title");
        VBox box = new VBox(6, c, v);
        if (!sub.isEmpty()) {
            Label s = new Label(sub);
            s.getStyleClass().add("cell-sub");
            box.getChildren().add(s);
        }
        return box;
    }

    private static Button iconButton(String icon) {
        Button button = new Button();
        button.setGraphic(new FontIcon(icon));
        button.getStyleClass().add("icon-button");
        button.setFocusTraversable(false);
        return button;
    }

    private static Button textButton(String text, String icon, String styleClass) {
        Button button = new Button(text);
        if (icon != null) {
            button.setGraphic(new FontIcon(icon));
            button.setGraphicTextGap(8);
        }
        button.getStyleClass().add(styleClass);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }
}
