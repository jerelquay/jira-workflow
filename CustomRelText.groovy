package rmf

enum CustomRelText implements ICustomField {

    /** Associated application issue key for this release */
    APP_KEY("CR_appKey"),

    /**
     * Short, meaningful description of this release
     */
    DESCRIPTION("Description"),

    /** Developer Main working Branch for this release <Appname>_<Release #>*/
    SCM_DEV_BRANCH("Development Branch"),

    /** Developer Main working Branch URL for this release <Appname>_<Release #>*/
    SCM_DEV_BRANCH_URL("Development Branch Url"),

    /** Release Tag to identify Source & Binary Package (<Appname>_<Release #>_<minor #>*/
    SCM_REL("CR_scmReleaseTag"),

    /** Release Tag URL to identify Source & Binary Package (<Appname>_<Release #>_<minor #>*/
    SCM_REL_BRANCH_URL("Release Branch Url"),

    /** Instruction for users to clone the repository branch*/
    SCM_INSTRUCTION("Instruction"),

    /** Current codebase of this release */
    CURRENT_CODEBASE("CR_codebase"),

    /** Application's current PBL */
    PBL_CURRENT("CR_pblCurrent"),

    /** Generic message field, used to issue instructions to user */
    MESSAGE("Message"),

    /** Minor release number that starts at 0 and is incremented for every code merge or rework */
    REL_NUM_MINOR("Minor Release No"),

    /** Tabulated scan result */
    SCAN_RESULT_TABLE("Detailed Scan Result"),

    /** URL to SonarQube scan result */
    SONARQUBE_URL("SonarQube URL"),

    /** User-input remarks */
    REMARKS("CR_remarks")

    //OVERWRITE_TAG("CR_???")

    private final String fieldName

    CustomRelText(String fieldName) {
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