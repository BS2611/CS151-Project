package cs151.model;

/**
 * The status of a {@link Task}. The label is the text shown to the user and the text stored in the database / data file.
 */
public enum TaskStatus {
    TO_DO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    TaskStatus(String label) {
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
    public static TaskStatus fromLabel(String label) {
        for (TaskStatus value : values()) {
            if (value.label.equals(label)) {
                return value;
            }
        }
        throw new PersistenceException("Unknown TaskStatus value: " + label, null);
    }
}
