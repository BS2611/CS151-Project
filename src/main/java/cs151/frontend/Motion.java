package cs151.frontend;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/** Small, quick animations used across the pages: button press, card hover, page transition, spin, shake and toasts. */
public final class Motion {

    private static final String MOTION_KEY = "motion.transition";
    private static VBox toastLayer;

    private Motion() {
    }

    /** Registers the (non-interactive) layer in which toasts appear. */
    public static void setToastLayer(VBox layer) {
        toastLayer = layer;
    }

    /**
     * Installs scene-wide mouse filters so every button, chip, nav item and card gets a press effect and
     * cards lift when hovered, without having to touch each control.
     */
    public static void install(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Node node = motionNode(event.getTarget(), false);
            if (node != null) {
                scaleTo(node, 0.97, 80);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            Node node = motionNode(event.getTarget(), false);
            if (node != null) {
                scaleTo(node, 1.0, 120);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, event -> {
            if (event.getTarget() instanceof Node target && target.getStyleClass().contains("action-card")) {
                liftTo(target, -3, 140);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, event -> {
            if (event.getTarget() instanceof Node target) {
                if (target.getStyleClass().contains("action-card")) {
                    liftTo(target, 0, 140);
                }
                if (isPressable(target)) {
                    scaleTo(target, 1.0, 120);
                }
            }
        });
    }

    /** Fades a freshly shown page in and slides its content area up a little. */
    public static void pageIn(Parent page) {
        page.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(200), page);
        fade.setToValue(1);

        ParallelTransition both = new ParallelTransition(fade);
        if (page instanceof Pane pane && pane.getChildren().size() > 1) {
            Node content = pane.getChildren().get(1);
            content.setTranslateY(14);
            TranslateTransition slide = new TranslateTransition(Duration.millis(240), content);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            both.getChildren().add(slide);
        }
        both.play();
    }

    /** Fades the dimmed overlay in and lets its card grow in slightly. */
    public static void popIn(Node overlay, Node card) {
        overlay.setOpacity(0);
        card.setScaleX(0.96);
        card.setScaleY(0.96);
        FadeTransition fade = new FadeTransition(Duration.millis(160), overlay);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, scale).play();
    }

    /** Spins a node once (used for the refresh icon). */
    public static void spin(Node node) {
        node.setRotate(0);
        RotateTransition spin = new RotateTransition(Duration.millis(550), node);
        spin.setToAngle(360);
        spin.setInterpolator(Interpolator.EASE_BOTH);
        spin.setOnFinished(event -> node.setRotate(0));
        spin.play();
    }

    /** Fades a node from slightly transparent back to fully visible. */
    public static void refreshFade(Node node) {
        node.setOpacity(0.35);
        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setToValue(1);
        fade.play();
    }

    /** Quick left-right shake to say "that did not work". */
    public static void shake(Node node) {
        TranslateTransition a = new TranslateTransition(Duration.millis(60), node);
        a.setFromX(0);
        a.setToX(-7);
        TranslateTransition b = new TranslateTransition(Duration.millis(90), node);
        b.setToX(7);
        TranslateTransition c = new TranslateTransition(Duration.millis(90), node);
        c.setToX(-4);
        TranslateTransition d = new TranslateTransition(Duration.millis(60), node);
        d.setToX(0);
        new SequentialTransition(a, b, c, d).play();
    }

    /** Shows a small message card in the top-right corner that slides in and removes itself after a moment. */
    public static void toast(String message, boolean error) {
        if (toastLayer == null) {
            return;
        }
        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);
        text.setMaxWidth(320);

        Label mark = new Label(error ? "!" : "✓");
        mark.getStyleClass().addAll("toast-mark", error ? "toast-mark-error" : "toast-mark-ok");

        HBox toast = new HBox(10, mark, text);
        toast.getStyleClass().add("toast");
        toast.setMaxWidth(HBox.USE_PREF_SIZE);
        toast.setOpacity(0);
        toast.setTranslateX(40);
        toastLayer.getChildren().add(toast);

        FadeTransition in = new FadeTransition(Duration.millis(180), toast);
        in.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), toast);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        PauseTransition stay = new PauseTransition(Duration.millis(2600));
        FadeTransition out = new FadeTransition(Duration.millis(260), toast);
        out.setToValue(0);

        SequentialTransition all = new SequentialTransition(new ParallelTransition(in, slide), stay, out);
        all.setOnFinished(event -> toastLayer.getChildren().remove(toast));
        all.play();
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isPressable(Node node) {
        var classes = node.getStyleClass();
        return classes.contains("primary-button") || classes.contains("secondary-button") || classes.contains("chip")
                || classes.contains("page-button") || classes.contains("nav-item") || classes.contains("action-card");
    }

    /** Walks up from the clicked node to the nearest button/chip/card that should animate. */
    private static Node motionNode(EventTarget target, boolean unused) {
        Node node = target instanceof Node n ? n : null;
        while (node != null) {
            if (isPressable(node) && !node.getStyleClass().contains("action-card")) {
                return node;
            }
            node = node.getParent();
        }
        return null;
    }

    private static void scaleTo(Node node, double scale, double millis) {
        stopPrevious(node);
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        node.getProperties().put(MOTION_KEY, transition);
        transition.play();
    }

    private static void liftTo(Node node, double y, double millis) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(millis), node);
        transition.setToY(y);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.play();
    }

    private static void stopPrevious(Node node) {
        Object previous = node.getProperties().get(MOTION_KEY);
        if (previous instanceof ScaleTransition running) {
            running.stop();
        }
    }
}
