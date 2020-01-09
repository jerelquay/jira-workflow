package rmf

enum CustomAppText implements ICustomField {
    /** Application description */
    APP_DESCRIPTION("Description"),

    /** Latest release number input by user upon creation of new Application */
    REL_NUM_LATEST_INPUT("amt_set_release_num"),

    /** Latest release number used by an approved release. Incremented each time a release is approved */
    REL_NUM_LATEST("Release Number"),

    /** Current baseline in production. <NAME_SHORT>_<REL_NUM_MAJOR>_<REL_NUM_MINOR> */
    PBL_CURRENT("Current Production Baseline"),

    /** For user to manually input PBL when application is created / PBL is updated */
    PBL_INPUT("amt_pblNew"),

    /** Previous production baseline, used for roll back */
    PBL_PREV("Previous Production Baseline"),

    /** For user to input previous baseline when application is created / PBL is updated */
    PBL_PREV_INPUT("amt_set_Prd_baseline_old"),

    /** OCP Cluster 1 admin token */
    OCP_ADMIN_TOKEN("amt_admin_token"),

    /** OCP Cluster 2 admin token */
    OCP_ADMIN_TOKEN2("amt_admin2_token"),

    /** OCP Cluster 1 token */
    OCP_TOKEN("amt_c1_token"),

    /** OCP Cluster 2 token */
    OCP_TOKEN2("amt_c2_token"),

    /** NEXUS repository name */
    NEXUS_REPO_NAME("Nexus Repo Name"),

    /** NEXUS Admin id */
    NEXUS_ADMIN_ID("amt_nexusAdmin"),

    /** NEXUS Admin password field (used to receive input from the user */
    NEXUS_ADMIN_PASSWORD("amt_nexusAdminPassword"),

    /** NEXUS Admin obfuscated password, hidden field */
    NEXUS_ADMIN_PID("amt_nexusAdminPid"),

    /** NEXUS user_id */
    NEXUS_USER_ID("Nexus User"),

    /** NEXUS user password field (used to receive input from the user */
    NEXUS_USER_PASSWORD("Nexus Password"),

    /** NEXUS user obfuscated password, hidden field */
    NEXUS_USER_PID("amt_nexusUserPid"),

    /** URl to Nexus repository manager */
    NEXUS_URL("Nexus Repo"),

    /** SCM hostname */
    SCM_HOSTNAME("SCM Hostname"),

    /** SCM user_id */
    SCM_USER_ID("SCM User"),

    /** SCM password field (used to receive input from the user */
    SCM_USER_PASSWORD("SCM Password"),

    /** SCM obfuscated password, hidden field */
    SCM_USER_PID("amt_scm_uidpw"),

    /** SCM_URL (for gitlab/svn/etc... url to main repo) */
    SCM_URL("SCM URL"),

    /** SCM Access Token field (used to do REST api calls to gitlab) */
    SCM_ACCESS_TOKEN("SCM Access Token"),

    /** SCM obsfucated access token field (used to do REST api calls to gitlab) */
    SCM_ACCESS_TOKEN_PID("SCM Access Token"),

    /** URL to SCM dev branch */
    SCM_DEV_URL("Development Repo"),

    /** URL to SCM release branch */
    SCM_REL_URL("Release Repo"),

    /** URL to SCM template branch */
    SCM_TEM_URL("SCM Template Repo"),

    /** Build Server IP / Hostname */
    ENV_BUILD("Build Server"),

    /** SIT Server IP / Hostname */
    ENV_SIT("SIT Server"),

    /** Availability SIT Server IP / Hostname */
    ENV_SIT_AVAILABILITY("SIT Server Availability"),

    /** UAT Server IP / Hostname */
    ENV_UAT("UAT Server"),

    /** Availability UAT Server IP / Hostname */
    ENV_UAT_AVAILABILITY("UAT Server Availability"),

    /** STG Server IP / Hostname */
    ENV_STG("amt_svr_STG"),

    /** PRD Server IP / Hostname */
    ENV_PRD("PRD Server"),

    /** Build Server user */
    ENV_BUILD_USER("Build Server User"),

    /** SIT Server user */
    ENV_SIT_USER("SIT User"),

    /** UAT Server user */
    ENV_UAT_USER("UAT User"),

    /** STG Server user */
    ENV_STG_USER("amt_usrSTG"),

    /** PRD Server user */
    ENV_PRD_USER("PRD User"),

    ENV_BACKUP("Deployment Backup Folder"),

    ENV_BUILD_FOLDER("Build Folder"),

    ENV_TARGET("Deployment Folder"),

    /** Current release that is being rolled out to production (i.e. PENDING_PRD status) */
    PRD_TENANT("amt_tenant_PRD"),

    /** SCM working path on Jira server */
    JIRA_SCM_WORKPATH("SCM Workspace"),

    /** Working path on Jira server to release package folder  */
    JIRA_BIN_WORKPATH("Bin Workspace"),

    /** Read only text field to display error messages */
    ERROR_MSG("amt_errorMessage"),

    /** L1DX Server IP/Hostname */
    L1DX_SERVER("L1DX Server"),

    /** ProjectID for Gitlab */
    GITLAB_PROJECTID("GITLAB_PROJECTID"),

    /** SSH URL for Gitlab */
    GITLAB_SSH_URL("GITLAB_SSH_URL")

    private final String fieldName

    CustomAppText(String fieldName) {
        this.fieldName = fieldName
    }

    /**
     * Get the custom field name for this permission
     * @return custom field name corresponding to this permission
     */
    String getFieldName() {
        return this.fieldName
    }

    @Override
    String toString() {
        return this.fieldName
    }
}