package cs151.model;

/**
 * A category of a {@link Goal}. The label is the text shown to the user and the text stored in the database / data file.
 */
public enum GoalCategory {
    ACADEMIC("Academic"),
    CAREER("Career"),
    HEALTH("Health"),
    FINANCIAL("Financial"),
    PERSONAL("Personal"),
    OTHER("Other");

    private final String label;

    GoalCategory(String label) {
        this.label = label;
    }

    /** The text shown to the user (for example "In Progress"). */
    public String label() {
        return label;
    }

    /** Shows the label, so a ComboBox or a table cell displays it directly. */
    @Override
    public String toString() {
        return label;
    }

    /**
     * Finds the constant with this label.
     *
     * @precondition label is one of the labels of this type
     * @throws PersistenceException if no constant has this label (damaged stored data)
     */
    public static GoalCategory fromLabel(String label) {
        for (GoalCategory value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new PersistenceException("Unknown GoalCategory value: " + label, null);
    }
}
