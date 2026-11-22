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
import java.util.Optional;

/**
 * Stores and loads Goal objects in a text file (default {@code data/goals.txt}), one goal per line with
 * pipe-separated fields. A backslash escapes {@code |}, line breaks and itself inside a field, so any name or
 * description can be stored. Ids are generated as (highest stored id + 1), so an id is not reused after a delete.
 */
public class FileSystemDAL implements DAL {
    /** Default goal file, relative to the application's working directory. */
    public static final Path DEFAULT_PATH = Paths.get("data", "goals.txt");

    private static final int FIELD_COUNT = 8;

    private final Path filePath;

    /** Uses the default file {@code data/goals.txt}. */
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
            throw new PersistenceException("Could not create the goals file.", e);
        }
    }

    @Override
    public long insertGoal(Goal goal) {
        List<Goal> goals = readAll();
        long id = goals.stream().mapToLong(Goal::getId).max().orElse(0) + 1;
        goal.setId(id);
        goals.add(goal);
        writeAll(goals);
        return id;
    }

    /** Returns all goals, the most recently created first. */
    @Override
    public List<Goal> getAllGoals() {
        List<Goal> goals = readAll();
        goals.sort(Comparator.comparing(Goal::getCreationDate).reversed());
        return goals;
    }

    @Override
    public boolean updateGoal(Goal goal) {
        List<Goal> goals = readAll();
        for (int i = 0; i < goals.size(); i++) {
            if (goals.get(i).getId() == goal.getId()) {
                goals.set(i, goal);
                writeAll(goals);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteGoal(long goalId) {
        List<Goal> goals = readAll();
        if (!goals.removeIf(goal -> goal.getId() == goalId)) {
            return false;
        }
        writeAll(goals);
        return true;
    }

    @Override
    public Optional<Goal> findGoalById(long goalId) {
        return readAll().stream().filter(goal -> goal.getId() == goalId).findFirst();
    }

    @Override
    public Optional<Goal> findGoalByName(String name) {
        return readAll().stream().filter(goal -> goal.getName().equalsIgnoreCase(name)).findFirst();
    }

    // ------------------------------------------------------------------ file access

    /** Reads every goal in file order; lines that are blank or damaged are skipped. */
    private List<Goal> readAll() {
        List<Goal> goals = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = splitRecord(line);
                if (fields.size() != FIELD_COUNT) {
                    continue;
                }
                try {
                    goals.add(new Goal(
                            Long.parseLong(fields.get(0)),
                            fields.get(1),
                            fields.get(2),
                            fields.get(3),
                            LocalDate.parse(fields.get(4)),
                            fields.get(5),
                            Integer.parseInt(fields.get(6)),
                            LocalDate.parse(fields.get(7))));
                } catch (NumberFormatException | DateTimeParseException damaged) {
                    // skip a damaged line instead of failing the whole page
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not load goals.", e);
        }
        return goals;
    }

    private void writeAll(List<Goal> goals) {
        List<String> lines = new ArrayList<>();
        for (Goal goal : goals) {
            lines.add(String.join("|",
                    String.valueOf(goal.getId()),
                    escape(goal.getName()),
                    escape(goal.getDescription()),
                    escape(goal.getCategory()),
                    String.valueOf(goal.getTargetDate()),
                    escape(goal.getStatus()),
                    String.valueOf(goal.getProgress()),
                    String.valueOf(goal.getCreationDate())));
        }
        try {
            Files.write(filePath, lines);
        } catch (IOException e) {
            throw new PersistenceException("Could not save goals.", e);
        }
    }

    private static String escape(String value) {
        return String.valueOf(value)
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
