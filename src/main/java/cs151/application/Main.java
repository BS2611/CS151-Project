package cs151.application;

import cs151.backend.model.Goal;
import cs151.backend.service.GoalPlanner;
import cs151.backend.service.SeedData;
import cs151.backend.model.Milestone;
import cs151.frontend.controller.MilestoneController;
import cs151.frontend.controller.MilestoneListController;
import cs151.frontend.controller.TaskController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import cs151.frontend.Motion;
import cs151.frontend.WinFrame;
import cs151.frontend.WindowChrome;
import javafx.scene.Parent;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Entry point of the Personal Goal Planner. Opens the window with the Home page and provides the static
 * methods the controllers use to switch between pages.
 */
public class Main extends Application {

    private static Scene scene;
    private static Stage stage;
    private static WindowChrome chrome;
    private static StackPane pageHost;
    private static StackPane overlayLayer;

    /** True while the Home page is showing, so the sidebar highlights the right item. */
    private static boolean homeActive = true;

    /** Text of the breadcrumb in the top bar, set by every showXxx method. */
    private static String breadcrumb = "Home";

    /** Search text typed in the top bar, picked up once by the Goal List page. */
    private static String pendingSearch;

    @Override
    public void start(Stage primaryStage) throws IOException {
        loadFonts();

        // v0.6: make sure the sample goals and milestones exist on first launch
        SeedData.seedIfEmpty(new GoalPlanner());

        stage = primaryStage;
        stage.initStyle(StageStyle.UNDECORATED);

        scene = new Scene(createShell(), 1280, 800);
        scene.getStylesheets().add(
                Main.class.getResource("/cs151/frontend/css/application.css").toExternalForm()
        );
        Motion.install(scene);

        chrome = new WindowChrome(stage);

        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setTitle("Personal Goal Planner");
        stage.setScene(scene);
        stage.show();

        // Windows: give the frameless window native snap/resize/drag; otherwise move and resize it from Java.
        if (WinFrame.install(stage)) {
            chrome.useNativeFrame();
        } else {
            chrome.installResize(scene);
        }
    }

    /**
     * Builds the root of the window: the current page plus a transparent layer on top of it for toasts.
     * The first page (Home) is put in place here.
     */
    public static Parent createShell() throws IOException {
        pageHost = new StackPane();
        pageHost.getStyleClass().add("window-frame");

        VBox toasts = new VBox(10);
        toasts.setAlignment(Pos.TOP_RIGHT);
        toasts.setPickOnBounds(false);
        toasts.setMouseTransparent(true);
        toasts.setPadding(new javafx.geometry.Insets(70, 24, 0, 24));
        Motion.setToastLayer(toasts);

        // A dimmed layer for pop-up cards (goal details, delete confirmation); a click outside the card closes it.
        overlayLayer = new StackPane();
        overlayLayer.getStyleClass().add("overlay");
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
        overlayLayer.setOnMouseClicked(event -> {
            if (event.getTarget() == overlayLayer) {
                hideOverlay();
            }
        });

        StackPane shell = new StackPane(pageHost, overlayLayer, toasts);
        shell.getStyleClass().add("window-shell");
        pageHost.getChildren().add(loadView("home-view.fxml"));
        return shell;
    }

    /** Puts a freshly loaded page in the window and plays the page transition. */
    private static void setPage(Parent page) {
        pageHost.getChildren().setAll(page);
        Motion.pageIn(page);
    }

    /** Shows a pop-up card centred over the page. */
    public static void showOverlay(javafx.scene.Node card) {
        overlayLayer.getChildren().setAll(card);
        overlayLayer.setManaged(true);
        overlayLayer.setVisible(true);
        Motion.popIn(overlayLayer, card);
    }

    public static void hideOverlay() {
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
        overlayLayer.getChildren().clear();
    }

    public static Stage getStage() {
        return stage;
    }

    public static WindowChrome getChrome() {
        return chrome;
    }

    /** Loads the bundled Inter font (the typeface used in the Figma design) so it does not need to be installed. */
    public static void loadFonts() {
        for (String file : new String[]{"Inter-Regular.ttf", "Inter-Bold.ttf"}) {
            Font.loadFont(Main.class.getResourceAsStream("/cs151/frontend/fonts/" + file), 12);
        }
    }

    public static boolean isHomeActive() {
        return homeActive;
    }

    public static String getBreadcrumb() {
        return breadcrumb;
    }

    /** Returns the search text waiting for the Goal List page (and forgets it). */
    public static String takePendingSearch() {
        String text = pendingSearch;
        pendingSearch = null;
        return text;
    }

    public static void showHomePage() throws IOException {
        homeActive = true;
        breadcrumb = "Home";
        setPage(loadView("home-view.fxml"));
    }

    public static void showGoalPage() throws IOException {
        homeActive = false;
        breadcrumb = "Goals  \u203A  New Goal";
        setPage(loadView("goal-view.fxml"));
    }

    /** Shows the Goal page in edit mode for an existing goal. */
    public static void showEditGoalPage(Goal goal) throws IOException {
        homeActive = false;
        breadcrumb = "Goals  \u203A  Edit Goal";
        FXMLLoader loader = createLoader("goal-view.fxml");
        setPage(loader.load());

        cs151.frontend.controller.GoalController controller = loader.getController();
        controller.setGoal(goal);
    }

    public static void showGoalListPage() throws IOException {
        showGoalListPage(null);
    }

    /** Shows the Goal List with the given text pre-filled in its search box (null or blank for none). */
    public static void showGoalListPage(String search) throws IOException {
        homeActive = false;
        breadcrumb = "Goals";
        pendingSearch = search == null || search.isBlank() ? null : search.trim();
        setPage(loadView("goal-list-view.fxml"));
    }

    /** Shows the Milestone page (create form) with the given goal pre-selected. */
    public static void showMilestonePage(Goal goal) throws IOException {
        homeActive = false;
        breadcrumb = "Goals  \u203A  New Milestone";
        FXMLLoader loader = createLoader("milestone-view.fxml");
        setPage(loader.load());

        MilestoneController controller = loader.getController();
        controller.setGoal(goal);
    }

    /** Shows the Milestone List page with all milestones of the given goal. */
    public static void showMilestoneListPage(Goal goal) throws IOException {
        homeActive = false;
        breadcrumb = "Goals  \u203A  Milestones";
        FXMLLoader loader = createLoader("milestone-list-view.fxml");
        setPage(loader.load());

        MilestoneListController controller = loader.getController();
        controller.setGoal(goal);
    }
    /** Opens the Task page for the selected milestone. */
    public static void showTaskPage(Milestone milestone) throws IOException {
        homeActive = false;
        breadcrumb = "Goals  ›  Milestones  ›  New Task";

        FXMLLoader loader = createLoader("task-view.fxml");
        setPage(loader.load());

        TaskController controller = loader.getController();
        controller.setMilestone(milestone);
    }

    private static FXMLLoader createLoader(String fileName) {
        return new FXMLLoader(
                Main.class.getResource("/cs151/frontend/view/" + fileName)
        );
    }

    private static Parent loadView(String fileName) throws IOException {
        return createLoader(fileName).load();
    }

    public static void main(String[] args) {
        launch();
    }
}