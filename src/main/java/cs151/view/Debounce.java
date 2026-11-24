package cs151.view;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * Runs an action only after the user has stopped typing for a short time: every call to {@link #trigger()}
 * restarts the timer, so typing "hello" quickly runs the action once instead of five times.
 * Must be used on the JavaFX application thread.
 */
public final class Debounce {
    /** Default pause after the last key before a search runs. */
    public static final Duration SEARCH_DELAY = Duration.millis(250);

    private final PauseTransition pause;

    public Debounce(Duration delay, Runnable action) {
        pause = new PauseTransition(delay);
        pause.setOnFinished(event -> action.run());
    }

    /** (Re)starts the timer. */
    public void trigger() {
        pause.playFromStart();
    }

    /** Drops a pending run, for example when the user presses Enter and the action runs right away. */
    public void cancel() {
        pause.stop();
    }
}
