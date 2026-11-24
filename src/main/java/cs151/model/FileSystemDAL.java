package cs151.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A second {@link DAL} strategy: stores goals, milestones, tasks and progress entries in one text file (default
 * {@code data/tracker.txt}), one record per line with pipe-separated fields. The first field tells the kind of
 * record ({@code G}oal, {@code M}ilestone, {@code T}ask, {@code P}rogress entry). A backslash escapes {@code |},
 * line breaks and itself inside a field, so any text can be stored. Ids are (highest stored id of that kind + 1).
 *
 * <p>It follows exactly the same rules as {@link DataManager} (see {@link DAL}): unique names, parent must exist,
 * cascading deletes and the same sort orders, so {@link GoalPlanner} behaves the same with either one.
 */
public class FileSystemDAL implements DAL {
    /** Default data file, relative to the application's working directory. */
    public static final Path DEFAULT_PATH = Paths.get("data", "tracker.txt");

    private static final String GOAL = "G";
    private static final String MILESTONE = "M";
    private static final String TASK = "T";
    private static final String PROGRESS = "P";

    private final Path filePath;

    /** Uses the default file {@code data/tracker.txt}. */
    public FileSystemDAL() {
        this(DEFAULT_PATH);
    }

    /** Uses the given file, creating its folder and the file itself if needed. */
    public FileSystemDAL(Path filePath) {
        this.filePath = filePath;
        try {
            Path parent = filePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not create the data file.", e);
        }
    }

    /** Everything in the file, loaded into memory for one operation. */
    private static final class Store {
        final List<Goal> goals = new ArrayList<>();
        final List<Milestone> milestones = new ArrayList<>();
        final List<Task> tasks = new ArrayList<>();
        final List<ProgressEntry> entries = new ArrayList<>();
    }

    // ------------------------------------------------------------------ goals

    @Override
    public long insertGoal(Goal goal) {
        Store s = load();
        requireUniqueGoalName(s, goal);
        long id = s.goals.stream().mapToLong(Goal::getId).max().orElse(0) + 1;
        goal.setId(id);
        s.goals.add(goal);
        save(s);
        return id;
    }

    @Override
    public List<Goal> getAllGoals() {
        List<Goal> goals = load().goals;
        goals.sort(Comparator.comparing(Goal::getTargetDate).thenComparing(Goal::getName));
        return goals;
    }

    @Override
    public boolean updateGoal(Goal goal) {
        Store s = load();
        int index = indexOf(s.goals, g -> g.getId() == goal.getId());
        if (index < 0) {
            return false;
        }
        requireUniqueGoalName(s, goal);
        s.goals.set(index, goal);
        save(s);
        return true;
    }

    @Override
    public boolean deleteGoal(long goalId) {
        Store s = load();
        if (!s.goals.removeIf(g -> g.getId() == goalId)) {
            return false;
        }
        removeMilestonesWhere(s, m -> m.getGoalId() == goalId);
        save(s);
        return true;
    }

    @Override
    public Optional<Goal> findGoalById(long goalId) {
        return load().goals.stream().filter(g -> g.getId() == goalId).findFirst();
    }

    @Override
    public Optional<Goal> findGoalByName(String name) {
        String wanted = name == null ? "" : name.trim();
        return load().goals.stream().filter(g -> g.getName().equals(wanted)).findFirst();
    }

    private static void requireUniqueGoalName(Store s, Goal goal) {
        for (Goal other : s.goals) {
            if (other.getId() != goal.getId() && other.getName().equals(goal.getName())) {
                throw new DuplicateEntityException("Goal name already exists.");
            }
        }
    }

    // ------------------------------------------------------------------ milestones

    @Override
    public long insertMilestone(Milestone milestone) {
        Store s = load();
        requireGoal(s, milestone.getGoalId());
        requireUniqueMilestoneName(s, milestone);
        long id = s.milestones.stream().mapToLong(Milestone::getId).max().orElse(0) + 1;
        milestone.setId(id);
        s.milestones.add(milestone);
        save(s);
        return id;
    }

    @Override
    public boolean updateMilestone(Milestone milestone) {
        Store s = load();
        int index = indexOf(s.milestones, m -> m.getId() == milestone.getId());
        if (index < 0) {
            return false;
        }
        Milestone stored = s.milestones.get(index);
        // like the SQL update, the goal of a milestone never changes
        Milestone changed = new Milestone(stored.getId(), stored.getGoalId(), milestone.getMilestoneName(),
                milestone.getTargetDate(), milestone.getStatus());
        requireUniqueMilestoneName(s, changed);
        s.milestones.set(index, changed);
        save(s);
        return true;
    }

    @Override
    public boolean deleteMilestone(long milestoneId) {
        Store s = load();
        if (!removeMilestonesWhere(s, m -> m.getId() == milestoneId)) {
            return false;
        }
        save(s);
        return true;
    }

    @Override
    public Optional<Milestone> findMilestoneById(long milestoneId) {
        return load().milestones.stream().filter(m -> m.getId() == milestoneId).findFirst();
    }

    @Override
    public List<Milestone> getMilestonesForGoal(long goalId) {
        List<Milestone> found = new ArrayList<>();
        for (Milestone m : load().milestones) {
            if (m.getGoalId() == goalId) {
                found.add(m);
            }
        }
        found.sort(milestoneOrder());
        return found;
    }

    private static void requireGoal(Store s, long goalId) {
        if (s.goals.stream().noneMatch(g -> g.getId() == goalId)) {
            throw new NotFoundException("The parent record does not exist.");
        }
    }

    private static void requireUniqueMilestoneName(Store s, Milestone milestone) {
        for (Milestone other : s.milestones) {
            if (other.getId() != milestone.getId() && other.getGoalId() == milestone.getGoalId()
                    && other.getMilestoneName().equals(milestone.getMilestoneName())) {
                throw new DuplicateEntityException("Milestone name already exists for this goal.");
            }
        }
    }

    /** Removes the matching milestones with their tasks and progress entries; returns whether any matched. */
    private static boolean removeMilestonesWhere(Store s, Predicate<Milestone> test) {
        List<Long> removedIds = new ArrayList<>();
        for (Milestone m : s.milestones) {
            if (test.test(m)) {
                removedIds.add(m.getId());
            }
        }
        s.milestones.removeIf(m -> removedIds.contains(m.getId()));
        for (long milestoneId : removedIds) {
            removeTasksWhere(s, t -> t.getMilestoneId() == milestoneId);
        }
        return !removedIds.isEmpty();
    }

    // ------------------------------------------------------------------ tasks

    @Override
    public long insertTask(Task task) {
        Store s = load();
        if (s.milestones.stream().noneMatch(m -> m.getId() == task.getMilestoneId())) {
            throw new NotFoundException("The parent record does not exist.");
        }
        requireUniqueTaskDescription(s, task);
        long id = s.tasks.stream().mapToLong(Task::getId).max().orElse(0) + 1;
        task.setId(id);
        s.tasks.add(task);
        save(s);
        return id;
    }

    @Override
    public boolean updateTask(Task task) {
        Store s = load();
        int index = indexOf(s.tasks, t -> t.getId() == task.getId());
        if (index < 0) {
            return false;
        }
        Task stored = s.tasks.get(index);
        // like the SQL update, the milestone of a task never changes
        Task changed = new Task(stored.getId(), stored.getMilestoneId(), task.getDescription(), task.getDueDate(),
                task.getPriority(), task.getStatus(), task.getCompletionDate());
        requireUniqueTaskDescription(s, changed);
        s.tasks.set(index, changed);
        save(s);
        return true;
    }

    @Override
    public boolean deleteTask(long taskId) {
        Store s = load();
        if (!removeTasksWhere(s, t -> t.getId() == taskId)) {
            return false;
        }
        save(s);
        return true;
    }

    @Override
    public Optional<Task> findTaskById(long taskId) {
        return load().tasks.stream().filter(t -> t.getId() == taskId).findFirst();
    }

    @Override
    public List<Task> getTasksForMilestone(long milestoneId) {
        List<Task> found = new ArrayList<>();
        for (Task t : load().tasks) {
            if (t.getMilestoneId() == milestoneId) {
                found.add(t);
            }
        }
        found.sort(taskOrder());
        return found;
    }

    private static void requireUniqueTaskDescription(Store s, Task task) {
        for (Task other : s.tasks) {
            if (other.getId() != task.getId() && other.getMilestoneId() == task.getMilestoneId()
                    && other.getDescription().equals(task.getDescription())) {
                throw new DuplicateEntityException("Task description already exists for this milestone.");
            }
        }
    }

    /** Removes the matching tasks with their progress entries; returns whether any matched. */
    private static boolean removeTasksWhere(Store s, Predicate<Task> test) {
        List<Long> removedIds = new ArrayList<>();
        for (Task t : s.tasks) {
            if (test.test(t)) {
                removedIds.add(t.getId());
            }
        }
        s.tasks.removeIf(t -> removedIds.contains(t.getId()));
        s.entries.removeIf(e -> removedIds.contains(e.getTaskId()));
        return !removedIds.isEmpty();
    }

    // ------------------------------------------------------------------ progress entries

    @Override
    public long insertProgressEntry(ProgressEntry entry) {
        Store s = load();
        if (s.tasks.stream().noneMatch(t -> t.getId() == entry.getTaskId())) {
            throw new NotFoundException("The parent record does not exist.");
        }
        long id = s.entries.stream().mapToLong(ProgressEntry::getId).max().orElse(0) + 1;
        entry.setId(id);
        s.entries.add(entry);
        save(s);
        return id;
    }

    @Override
    public boolean updateProgressEntry(ProgressEntry entry) {
        Store s = load();
        int index = indexOf(s.entries, e -> e.getId() == entry.getId());
        if (index < 0) {
            return false;
        }
        ProgressEntry stored = s.entries.get(index);
        s.entries.set(index, new ProgressEntry(stored.getId(), stored.getTaskId(), stored.getEntryDate(),
                entry.getNote()));
        save(s);
        return true;
    }

    @Override
    public boolean deleteProgressEntry(long entryId) {
        Store s = load();
        if (!s.entries.removeIf(e -> e.getId() == entryId)) {
            return false;
        }
        save(s);
        return true;
    }

    @Override
    public Optional<ProgressEntry> findProgressEntryById(long entryId) {
        return load().entries.stream().filter(e -> e.getId() == entryId).findFirst();
    }

    @Override
    public List<ProgressEntry> getProgressEntriesForTask(long taskId) {
        List<ProgressEntry> found = new ArrayList<>();
        for (ProgressEntry e : load().entries) {
            if (e.getTaskId() == taskId) {
                found.add(e);
            }
        }
        found.sort(Comparator.comparing(ProgressEntry::getEntryDate).thenComparingLong(ProgressEntry::getId));
        return found;
    }

    // ------------------------------------------------------------------ search

    @Override
    public List<Goal> searchGoals(String query) {
        String q = normalize(query);
        List<Goal> found = new ArrayList<>();
        for (Goal g : load().goals) {
            if (matches(q, g.getName(), g.getDescription(), g.getCategory(), g.getStatus(),
                    String.valueOf(g.getTargetDate()), String.valueOf(g.getCreationDate()),
                    String.valueOf(g.getProgress()))) {
                found.add(g);
            }
        }
        found.sort(Comparator.comparing(Goal::getTargetDate).thenComparing(Goal::getName));
        return found;
    }

    @Override
    public List<Milestone> searchMilestones(String query) {
        String q = normalize(query);
        List<Milestone> found = new ArrayList<>();
        for (Milestone m : load().milestones) {
            if (matches(q, m.getMilestoneName(), String.valueOf(m.getTargetDate()), m.getStatus())) {
                found.add(m);
            }
        }
        found.sort(milestoneOrder());
        return found;
    }

    @Override
    public List<Task> searchTasks(String query) {
        String q = normalize(query);
        List<Task> found = new ArrayList<>();
        for (Task t : load().tasks) {
            if (matches(q, t.getDescription(), String.valueOf(t.getDueDate()), t.getPriority(), t.getStatus())) {
                found.add(t);
            }
        }
        found.sort(taskOrder());
        return found;
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String query, String... fields) {
        for (String field : fields) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private static Comparator<Milestone> milestoneOrder() {
        return Comparator.comparing(Milestone::getTargetDate).thenComparing(Milestone::getMilestoneName);
    }

    private static Comparator<Task> taskOrder() {
        return Comparator.comparing(Task::getDueDate).thenComparing(Task::getDescription);
    }

    private static <T> int indexOf(List<T> list, Predicate<T> test) {
        for (int i = 0; i < list.size(); i++) {
            if (test.test(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ file access

    /** Reads every record; lines that are blank, unknown or damaged are skipped. */
    private Store load() {
        Store s = new Store();
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath);
        } catch (IOException e) {
            throw new PersistenceException("Could not load the data file.", e);
        }
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            List<String> f = splitRecord(line);
            try {
                switch (f.get(0)) {
                    case GOAL -> s.goals.add(new Goal(Long.parseLong(f.get(1)), f.get(2), f.get(3), f.get(4),
                            LocalDate.parse(f.get(5)), f.get(6), Integer.parseInt(f.get(7)),
                            LocalDate.parse(f.get(8))));
                    case MILESTONE -> s.milestones.add(new Milestone(Long.parseLong(f.get(1)),
                            Long.parseLong(f.get(2)), f.get(3), LocalDate.parse(f.get(4)), f.get(5)));
                    case TASK -> s.tasks.add(new Task(Long.parseLong(f.get(1)), Long.parseLong(f.get(2)), f.get(3),
                            LocalDate.parse(f.get(4)), f.get(5), f.get(6), parseDate(f.get(7))));
                    case PROGRESS -> s.entries.add(new ProgressEntry(Long.parseLong(f.get(1)),
                            Long.parseLong(f.get(2)), LocalDate.parse(f.get(3)), f.get(4)));
                    default -> { /* unknown record kind: skip */ }
                }
            } catch (NumberFormatException | DateTimeParseException | IndexOutOfBoundsException damaged) {
                // skip a damaged line instead of failing the whole page
            }
        }
        return s;
    }

    private void save(Store s) {
        List<String> lines = new ArrayList<>();
        for (Goal g : s.goals) {
            lines.add(record(GOAL, String.valueOf(g.getId()), g.getName(), g.getDescription(), g.getCategory(),
                    String.valueOf(g.getTargetDate()), g.getStatus(), String.valueOf(g.getProgress()),
                    String.valueOf(g.getCreationDate())));
        }
        for (Milestone m : s.milestones) {
            lines.add(record(MILESTONE, String.valueOf(m.getId()), String.valueOf(m.getGoalId()),
                    m.getMilestoneName(), String.valueOf(m.getTargetDate()), m.getStatus()));
        }
        for (Task t : s.tasks) {
            lines.add(record(TASK, String.valueOf(t.getId()), String.valueOf(t.getMilestoneId()), t.getDescription(),
                    String.valueOf(t.getDueDate()), t.getPriority(), t.getStatus(),
                    t.getCompletionDate() == null ? "" : t.getCompletionDate().toString()));
        }
        for (ProgressEntry e : s.entries) {
            lines.add(record(PROGRESS, String.valueOf(e.getId()), String.valueOf(e.getTaskId()),
                    String.valueOf(e.getEntryDate()), e.getNote()));
        }
        try {
            Files.write(filePath, lines);
        } catch (IOException e) {
            throw new PersistenceException("Could not save the data file.", e);
        }
    }

    private static LocalDate parseDate(String text) {
        return text.isEmpty() ? null : LocalDate.parse(text);
    }

    private static String record(String kind, String... fields) {
        StringBuilder line = new StringBuilder(kind);
        for (String field : fields) {
            line.append('|').append(escape(field));
        }
        return line.toString();
    }

    private static String escape(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /** Splits a line at every {@code |} that is not escaped and removes the escapes. */
    private static List<String> splitRecord(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                char next = line.charAt(++i);
                field.append(next == 'n' ? '\n' : next == 'r' ? '\r' : next);
            } else if (c == '|') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
