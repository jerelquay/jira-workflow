package rmf.app

/**
 * Application Issue Statuses
 */
enum AppStatus {

    OPEN("Open"),

    CLOSE("Close")




    private final String name

    AppStatus(String name) {
        this.name = name
    }

    /**
     * Get the Jira name for this RelStatus
     * @return Jira name for this RelStatus
     */
    String getName() {
        return this.name
    }

    @Override
    String toString() {
        return this.name
    }

    boolean equals(String status) {
        return this.name.equals(status)
    }
}