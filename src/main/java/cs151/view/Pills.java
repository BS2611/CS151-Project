package cs151.view;

import javafx.scene.control.Label;

/**
 * Small view helpers that build the coloured "pill" labels and pick the CSS style class
 * for a category or status. Shared by the list pages and the goal dialogs.
 */
public final class Pills {

    private Pills() {
    }

    public static Label pill(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("pill", styleClass);
        return label;
    }

    /** A status pill with a small coloured dot in front, like the reference design. */
    public static Label statusPill(String status) {
        Label label = pill(status, statusStyle(status));
        javafx.scene.layout.Region dot = new javafx.scene.layout.Region();
        dot.getStyleClass().add("pill-dot");
        label.setGraphic(dot);
        label.setGraphicTextGap(6);
        return label;
    }

    /** Category colours: light blue, light indigo, light green, light amber, light gray, neutral gray. */
    public static String categoryStyle(String category) {
        return switch (category) {
            case "Academic" -> "pill-cat-academic";
            case "Career" -> "pill-cat-career";
            case "Health" -> "pill-cat-health";
            case "Financial" -> "pill-cat-financial";
            case "Personal" -> "pill-cat-personal";
            default -> "pill-cat-other";
        };
    }

    public static String statusStyle(String status) {
        return switch (status) {
            case "Completed" -> "pill-ok";
            case "In Progress" -> "pill-progress";
            case "On Hold" -> "pill-warn";
            case "Abandoned" -> "pill-danger";
            default -> "pill-muted";
        };
    }
}
