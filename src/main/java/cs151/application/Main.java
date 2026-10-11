package cs151.application;

import cs151.backend.model.Goal;
import cs151.frontend.controller.MilestoneController;
import cs151.frontend.controller.MilestoneListController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Entry point of the Personal Goal Planner. Opens the window with the Home page and provides the static
 * methods the controllers use to switch between pages.
 */
public class Main extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {


        scene = new Scene(loadView("home-view.fxml"),700, 650);

        stage.setTitle("Personal Goal Planner");
        stage.setScene(scene);
        stage.show();
    }

    public static void showHomePage() throws IOException {
        scene.setRoot(loadView("home-view.fxml"));
    }

    public static void showGoalPage() throws IOException {
        scene.setRoot(loadView("goal-view.fxml"));
    }

    public static void showGoalListPage() throws IOException {
        scene.setRoot(loadView("goal-list-view.fxml"));
    }

    /** Shows the Milestone page (create form) with the given goal pre-selected. */
    public static void showMilestonePage(Goal goal) throws IOException {
        FXMLLoader loader = createLoader("milestone-view.fxml");
        scene.setRoot(loader.load());

        MilestoneController controller = loader.getController();
        controller.setGoal(goal);
    }

    /** Shows the Milestone List page with all milestones of the given goal. */
    public static void showMilestoneListPage(Goal goal) throws IOException {
        FXMLLoader loader = createLoader("milestone-list-view.fxml");
        scene.setRoot(loader.load());

        MilestoneListController controller = loader.getController();
        controller.setGoal(goal);
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