# Name of application: My Goal Tracker

# Who did What

# Version 0.8
1. Keerat Kaur: Implemented Milestone Edit and Delete functionality, including the prefilled edit form, permanent updates, deletion warning, cascade deletion of linked Tasks and Progress entries, and navigation back to the Goals list.
2. Bhavpreet Singh: Implemented Goal Edit and Delete functionality, including editable Creation Date, permanent updates, deletion warning and impact counts, cascade deletion of linked Milestones, Tasks, and Progress entries, and two meaningful Progress entries for each seeded Task.
3. Sujata Giri: Reviewed the project’s MVC organization, expanded the Goal edit persistence tests, added Milestone edit and cascade-delete tests, ran the complete test suite, updated the README, cleaned the project folder, and prepared the final ZIP.

# Version 0.7
1. Bhavpreet Singh: Implemented the Progress page UI with Entry Date and Note fields, connected the Save button to the backend with validation toasts, added double-click navigation from the Task List page, added a Back button and populated each of the 9 milestones with 2 meaningful tasks
2. Sujata Giri: Added the View Progress button and Progress List page using a JavaFX TableView sorted by Entry Date ascending, added a Back button and sorted Tasks by Due Date ascending
3. Keerat Kaur: Added tests for saving Progress entries, Task and Progress sorting, updated the README, cleaned the project folder and created the final ZIP.
   
# Version 0.6
1. Sujata Giri: Created the Task page UI (Description, Due Date, Priority, Status) and added the Create Task button to the Milestone list page
2. Bhavpreet Singh: Backend - saved Tasks permanently in SQLite and connected each Task to its associated Milestone (Save Task button), populated 3 Goals with 3 Milestones each, and sorted Goals and Milestones by Target Date ascending
3. Keerat Kaur: Implemented the Task List page (JavaFX TableView sorted by Target Date) and the View Tasks button, cleaned the project folder, updated the ReadMe.md, and created the final ZIP

# Version 0.5
1. Keerat Kaur: Created the Milestone page UI (Goal, Milestone Name, Target Date, Status) and added the Create Milestone button to the Goal list page
2. Bhavpreet Singh: Backend - saved Milestones permanently in SQLite and connected each Milestone to its associated Goal; Goals and Milestones are sorted by Target Date descending
3. Sujata Giri: Implemented the Milestone list page (JavaFX TableView), cleaned the project folder, updated the ReadMe.md, and created the final ZIP

# Version 0.4
1. Keerat Kaur: Implemented permanent saving of Goal data (DAL interface, FileSystemDAL, data/goals.txt) and the success message on the Goal page
2. Sujata Giri: Implemented the Stored Goals page (JavaFX TableView sorted by creation date, most recent on top) with Back/Refresh buttons and navigation from the Home page
3. Bhavpreet Singh: Cleaned the project folder (removed non-source files), updated the ReadMe.md, and created the final ZIP

# Version 0.2
1. Sujata Giri: Created the Goal and Home Page UI, implemented page navigation and integrated Goal creation with the backend
2. Bhavpreet Singh: Created the backend classes, validation, and data storage
3. Keerat Kaur: Reviewed final integration, removed non-code files and final submission

# Technical-Spec
1. Keerat Kaur: Data Model, Objective, and References
2. Sujata Giri: Class Diagram
3. Bhavpreet Singh: Sequence Diagram

# Functional-Spec
1. Keerat Kaur: Mockup#1
2. Sujata Giri: Use Case, Objective, References, Problem Statement, Scope
3. Bhavpreet Singh: Mockup#2, functional requirements, non-functional requirements


# Any other instruction that users need to know:
Run the project with Zulu 23 using Maven.

Please run 'Main.java' (package cs151.application) to run the application.

Data note: Goals and Milestones are saved permanently in the SQLite database file data/my-goal-tracker.db, which is created automatically at runtime. No manual setup is required. The sqlite-jdbc library used by the backend is declared as a dependency in pom.xml.

How to use: On the Home page click "List Goals" (goals are sorted by Target Date, soonest first). Select a goal and click "Create Milestone" to open the Milestone page, then enter the milestone and click "Save". Double-click a goal (or select it and click "View Milestones") to open the Milestone List page, which shows that goal's milestones in a table sorted by Target Date, soonest first. Select a milestone to view its tasks, which are sorted by Due Date, soonest first. Double-click a task to add a Progress entry, or select a task and click "View Progress" to see its Progress history sorted by Entry Date, oldest first. Every page has a "Back" button.

## UI notes (frontend)
- Run: `mvn clean javafx:run` (Zulu/JDK 23 or newer). The first build needs internet access to download the Ikonli icon libraries (`ikonli-javafx`, `ikonli-fontawesome5-pack`).
- Look and colors: the rules are in `UI_STYLE_GUIDE.md`. All colors are variables at the top of `src/main/resources/cs151/view/css/application.css`; indigo is used only for the active sidebar item, primary buttons, focus, links and main progress bars.
- Icons: Ikonli with FontAwesome 5 only (`<FontIcon iconLiteral="fas-home"/>` in FXML). Do not add emoji or image icons.
- Font: Inter is bundled in `src/main/resources/cs151/view/fonts/` (license included), so it does not need to be installed on the computer.
- Pages: each page is one FXML file in `src/main/resources/cs151/view/` with the shared `sidebar-view.fxml` and `topbar-view.fxml`; the shared helpers are `Motion` (animations, toasts), `Pills` (status and category labels), `GoalDialogs`, `WindowChrome` and `WinFrame` (frameless window) in `src/main/java/cs151/view/`.
- Window: on Windows (and Linux) the title bar is our own. On Windows the native frame hook gives Aero Snap (drag to an edge, Win+Arrow) and the Windows 11 snap layouts over the maximize button; if the hook cannot start, the app falls back to a Java-only move/resize. On macOS the normal system title bar is used (red/yellow/green buttons, window tiling via the green button or by dragging to a screen edge, full screen and resizing all work natively).
- Goals page: search, status, sort and category filters work on the stored goals; the "..." Actions menu offers View details, Edit goal, Milestones and Delete goal (with a confirmation). Deleting a goal also deletes its milestones.
- `-Dcs151.fakeMac=true` makes another system hide our custom window buttons like on macOS (the real macOS title bar itself can only be seen on a Mac).

## MVC Design
The application has exactly three MVC packages, each a single flat package: 'cs151.model', 'cs151.view' and 'cs151.controller'. 'cs151.application.Main' is only the entry point and is not part of M, V or C.
- Model ('cs151.model'): the POJOs (Goal, Milestone, Task, ProgressEntry), the business rules (GoalPlanner, ReportGenerator, SeedData), the SQLite persistence (DAL, DataManager, FileSystemDAL), the data transfer objects (for example GoalNode and OverallSummary) and the exceptions.
- View ('cs151.view'): the FXML pages, CSS and fonts under 'src/main/resources/cs151/view', plus the UI helper classes (Motion, Pills, GoalDialogs, WindowChrome, WinFrame) that build and animate what the user sees.
- Controller ('cs151.controller'): the *Controller classes handle user actions, collect form data, call the model, display feedback, and navigate between views.
- 'Main' starts the JavaFX application and loads the appropriate views. Controllers communicate with 'GoalPlanner' rather than accessing SQLite directly, and the model does not depend on JavaFX, view or controller classes.


Thank You!
