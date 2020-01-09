package rmf

/**
 * Enumeration of the custom lists
 */
enum CustomLists implements ICustomField {

    /** Standard, Hot_Fix or Hot_Fix_SIT  */
    REL_DEPLOY_TYPE("Deployment Type", "Standard", "Hot Fix - Skip SIT", "Hot Fix - Skip UAT"),

    /** Major or Minor */
    REL_RELEASE_TYPE("Release Type", "Major", "Minor"),

    /** Code merge status */
    REL_IS_CODE_MERGED("CR_isCodeMerged", "No", "Yes"),

    /** Code merge review status */
    REL_IS_CODE_MERGE_VALIDATED("CR_isCodeMergeValidated", "No", "Yes"),

    /** Scan result checkbox */
    REL_SCAN_RESULT("Scan Result", "In progress...", "PASS", "FAIL"),

    /** Override checkbox */
    REL_SCAN_RESULT_OVERRIDE("Override Scan Result", "Override"),

    /** Checkbox to indicate that deploying / undeploying / rollback is still in progress */
    REL_TASKS_IN_PROGRESS("Task In Progress", "Yes", "No"),

    /** Retry flag is set when the retry action (PRD->PEND_PRD) is triggered, and unset when deploying PEND_PRD->PRD */
    REL_RETRY_FLAG("CR_retryFlag", "No", "Yes"),

    /** SVN or GIT */
    APP_SCM_TYPE("Source Control Management", "SVN", "GIT"),

    /** Nexus repository type: maven, raw etc */
    APP_NEXUS_REPO_TYPE("Nexus Repo Type", "Raw", "Maven2"),

    /** List of open Application Issues. Note that the options are dynamic, hence this enum does not list any options */
    OPEN_APPS("Application")


    private final String fieldName

    private final List<String> options

    CustomLists(String fieldName, String... options) {
        this.fieldName = fieldName
        this.options = Collections.unmodifiableList(options.toList())
    }

    /**
     * Get the custom field name for this list
     * @return custom field name corresponding to this list
     */
    String getFieldName() {
        return this.fieldName
    }

    /**
     * Get the available options (as Strings) for this List
     * @return options (as Strings) for this list
     */
    List<String> getOptions() {
        return this.options
    }

    @Override
    String toString() {
        return this.fieldName
    }
}
