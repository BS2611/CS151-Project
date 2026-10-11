# Name of application: My Goal Tracker

# Who did What

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

How to use: On the Home page click "List Goals" (goals are sorted by Target Date, latest first). Select a goal and click "Create Milestone" to open the Milestone page, then enter the milestone and click "Save". Double-click a goal (or select it and click "View Milestones") to open the Milestone List page, which shows that goal's milestones in a table sorted by Target Date, latest first. Every page has a "Back" button.

Thank You!
