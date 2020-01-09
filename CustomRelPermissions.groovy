package rmf

/**
 * Enumeration of the custom permissions used in Release Workflow
 * These permissions shall contain the permitted group name(s)
 * There should be one permission per transition in the workflows
 */
enum CustomRelPermissions implements ICustomField {

    /* Permission to create new release */
    CREATE("Create Release"),

    /* Valid */
    /** Open -> Work In Progress */
    APPROVE_RELEASE("Approve Release"),

    /** OPEN -> REJECTED  */
    REJECT_RELEASE("Reject Release"),

    /** REVIEW -> REJECTED */
    REJECT_REVIEW("Reject Review"),

    /** Open -> Open (edit) */
    EDIT_OPEN("Edit Release"),

    /** WIP -> WIP (edit) */
    EDIT_WIP("Edit WIP"),

    /** REVIEW -> REVIEW (edit) */
    EDIT_REVIEW("Edit Review"),

    /** REWORK -> REVIEW */
    REVIEW_REWORK("Review Rework"),

    /** WIP -> REVIEW */
    REVIEW_WIP("Review WIP"),

    /** CODEMERGE -> REVIEW */
    REVIEW_CODEMERGE("Review Rework"),

    /** PEND_PRD -> REVIEW */
    REVIEW_PEND_PRD("Review Pend PRD"),

    /** ROLLBACK -> REVIEW */
    REVIEW_ROLLBACK("Review PRD Rollback"),

    /** REVIEW -> PEND_SIT */
    BUILD("Build"),

    /** PEND_SIT -> REVIEW  */
    REBUILD("Rebuild"),

    /** FAST TRACK from PEND_SIT to PEND_PRD */
    HOT_FIX_SKIP_SIT("Skip SIT (hotfix)"),

    /** FAST TRACK from SIT to PEND_PRD */
    HOT_FIX_SKIP_UAT("Skip UAT (hotfix)"),

    /** PEND_SIT -> SIT */
    DEPLOY_SIT("Deploy to SIT"),

    /** SIT -> PEND_UAT */
    PROMOTE_UAT("Promote to UAT"),

    /** PEND_UAT -> UAT */
    DEPLOY_UAT("Deploy to UAT"),

    /** UAT -> STAGING */
    DEPLOY_STAGING("amtPerm_deployStaging"),

    /** STAGING -> PEND_PRD */
    PROMOTE_PRD("Promote to PRD"),

    /** PEND_PRD -> PRD for deployment_type: major */
    DEPLOY_PRD_MAJ("Deploy Major PRD"),

    /** PEND_PRD -> PRD for deployment_type: minor */
    DEPLOY_PRD_MIN("Deploy Minor PRD"),

    /** SIT -> PEND_SIT */
    REVERT_SIT("Revert SIT"),

    /** UAT -> PEND_UAT */
    REVERT_UAT("Revert UAT"),

    /** PRD -> PEND_PRD */
    REVERT_PRD("Revert PRD"),

    /** PRD -> ROLLBACK */
    ROLLBACK("Rollback PRD"),

    /** REVIEW -> CODEMERGE */
    REWORK_REVIEW("Rework/Codemerge"),

    /** PEND_SIT -> CODEMERGE */
    REWORK_PEND_SIT("Rework Pend SIT"),

    /** SIT -> CODEMERGE */
    REWORK_SIT("Rework SIT"),

    /** PEND_UAT -> CODEMERGE */
    REWORK_PEND_UAT("Rework Pend UAT"),

    /** UAT -> CODEMERGE */
    REWORK_UAT("Rework UAT"),

    /** STAGING -> CODEMERGE */
    REWORK_STAGING("amtPerm_reworkStaging"),

    /** PEND_PRD -> CODEMERGE */
    REWORK_PEND_PRD("Rework Pend PRD"),

    /** PEND_PRD -> CODEMERGE */
    REWORK_ROLLBACK("Rework Rollback"),

    /** PRD -> CLOSE */
    CLOSE_PRD("Close Release"),

    /** CLOSE -> CLOSE */
    CLEAN_UP_CLOSE("Clean Up Release"),

    /** REJECTED -> REJECTED */
    CLEAN_UP_REJ("Reject Clean Up Release"),



    private final String fieldName

    CustomRelPermissions(String fieldName) {
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
