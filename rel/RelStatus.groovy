package rmf.rel

/**
 * Release Issue Statuses
 */
enum RelStatus {

    OPEN("Open"),

    WIP("Work In Progress"),

    REJECTED("Rejected"),

    REVIEW("Review"),

    PEND_SIT("Pend_SIT"),

    SIT("SIT"),

    PEND_UAT("Pend_UAT"),

    UAT("UAT"),

    STAGING("Staging"),

    PEND_PRD("Pend_PRD"),

    PRD("PRD"),

    ROLLBACK("Rollback"),

    REWORK("Re-Work_CodeMerge"),

    CLOSE("Close")


    private final String name

    RelStatus(String name) {
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