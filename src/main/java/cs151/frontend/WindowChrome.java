package cs151.frontend;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Behaviour of the frameless window: minimise, maximise/restore, close, moving by the top bar and resizing from
 * the window edges. (The operating system does these for a normal window; an undecorated window needs them here.)
 */
public final class WindowChrome {

    private static final double EDGE = 6;

    private final Stage stage;
    private boolean maximized;
    private boolean nativeFrame;
    private Rectangle2D restoreBounds;

    // resize / move state
    private double pressScreenX;
    private double pressScreenY;
    private double startX;
    private double startY;
    private double startW;
    private double startH;
    private boolean resizeLeft;
    private boolean resizeRight;
    private boolean resizeTop;
    private boolean resizeBottom;

    public WindowChrome(Stage stage) {
        this.stage = stage;
    }

    /** True on macOS, where the window buttons are drawn as the familiar red/yellow/green traffic lights. */
    public static boolean isMac() {
        // -Dcs151.fakeMac=true previews the macOS layout on another system
        return Boolean.getBoolean("cs151.fakeMac") || System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    /**
     * On Windows the native frame hook ({@link WinFrame}) handles moving, resizing and snapping, so the
     * Java-only versions below must stay out of the way.
     */
    public void useNativeFrame() {
        nativeFrame = true;
    }

    public void minimize() {
        stage.setIconified(true);
    }

    public boolean isMaximized() {
        return nativeFrame ? WinFrame.isMaximized() : maximized;
    }

    public void toggleMaximize() {
        if (nativeFrame) {
            WinFrame.toggleMaximize();
            return;
        }
        if (maximized) {
            restore();
        } else {
            restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Rectangle2D screen = Screen.getScreensForRectangle(
                    stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).get(0).getVisualBounds();
            stage.setX(screen.getMinX());
            stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth());
            stage.setHeight(screen.getHeight());
            maximized = true;
        }
    }

    private void restore() {
        if (restoreBounds != null) {
            stage.setX(restoreBounds.getMinX());
            stage.setY(restoreBounds.getMinY());
            stage.setWidth(restoreBounds.getWidth());
            stage.setHeight(restoreBounds.getHeight());
        }
        maximized = false;
    }

    /** Lets the given node (the top bar) drag the window; double-click toggles maximise. */
    public void makeDraggable(Node handle) {
        handle.setOnMousePressed(event -> {
            if (nativeFrame || isInteractive(event.getTarget())) {
                return;
            }
            if (event.getClickCount() == 2) {
                toggleMaximize();
                return;
            }
            pressScreenX = event.getScreenX();
            pressScreenY = event.getScreenY();
            startX = stage.getX();
            startY = stage.getY();
        });
        handle.setOnMouseDragged(event -> {
            if (nativeFrame || isInteractive(event.getTarget()) || event.getClickCount() > 1) {
                return;
            }
            if (maximized) {
                // dragging a maximised window restores it under the pointer first
                double ratio = event.getScreenX() / stage.getWidth();
                restore();
                startX = event.getScreenX() - restoreBounds.getWidth() * ratio;
                startY = event.getScreenY() - 18;
                pressScreenX = event.getScreenX();
                pressScreenY = event.getScreenY();
            }
            stage.setX(startX + event.getScreenX() - pressScreenX);
            stage.setY(startY + event.getScreenY() - pressScreenY);
        });
    }

    private boolean isInteractive(Object target) {
        Node node = target instanceof Node n ? n : null;
        while (node != null) {
            if (node instanceof ButtonBase || node instanceof TextInputControl) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    /** Lets the user resize the window by dragging within a few pixels of its edges. */
    public void installResize(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (maximized) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            scene.setCursor(cursorFor(edges(event, scene)));
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (maximized) {
                return;
            }
            boolean[] edge = edges(event, scene);
            resizeLeft = edge[0];
            resizeRight = edge[1];
            resizeTop = edge[2];
            resizeBottom = edge[3];
            pressScreenX = event.getScreenX();
            pressScreenY = event.getScreenY();
            startX = stage.getX();
            startY = stage.getY();
            startW = stage.getWidth();
            startH = stage.getHeight();
            if (isResizing()) {
                event.consume();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!isResizing() || maximized) {
                return;
            }
            double dx = event.getScreenX() - pressScreenX;
            double dy = event.getScreenY() - pressScreenY;
            double minW = Math.max(stage.getMinWidth(), 200);
            double minH = Math.max(stage.getMinHeight(), 200);

            if (resizeRight) {
                stage.setWidth(Math.max(minW, startW + dx));
            }
            if (resizeBottom) {
                stage.setHeight(Math.max(minH, startH + dy));
            }
            if (resizeLeft) {
                double width = Math.max(minW, startW - dx);
                stage.setX(startX + (startW - width));
                stage.setWidth(width);
            }
            if (resizeTop) {
                double height = Math.max(minH, startH - dy);
                stage.setY(startY + (startH - height));
                stage.setHeight(height);
            }
            event.consume();
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            resizeLeft = false;
            resizeRight = false;
            resizeTop = false;
            resizeBottom = false;
        });
    }

    private boolean isResizing() {
        return resizeLeft || resizeRight || resizeTop || resizeBottom;
    }

    private boolean[] edges(MouseEvent event, Scene scene) {
        double x = event.getSceneX();
        double y = event.getSceneY();
        return new boolean[]{
                x < EDGE,
                x > scene.getWidth() - EDGE,
                y < EDGE,
                y > scene.getHeight() - EDGE
        };
    }

    private Cursor cursorFor(boolean[] edge) {
        boolean left = edge[0];
        boolean right = edge[1];
        boolean top = edge[2];
        boolean bottom = edge[3];
        if ((left && top) || (right && bottom)) {
            return Cursor.NW_RESIZE;
        }
        if ((right && top) || (left && bottom)) {
            return Cursor.NE_RESIZE;
        }
        if (left || right) {
            return Cursor.H_RESIZE;
        }
        if (top || bottom) {
            return Cursor.V_RESIZE;
        }
        return Cursor.DEFAULT;
    }
}
