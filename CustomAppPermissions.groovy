package rmf

/**
 * Enumeration of the custom permissions used in Application Workflow
 * These permissions shall contain the permitted group name(s)
 * There should be one permission per transition in the workflows
 */
enum CustomAppPermissions implements ICustomField {

    /** Permission to SET the application production baseline (Update_Prd_Baseline) */
    SET_BASELINE("amt_SET_baseline"),

    /** Permission to SET password for OCP and SCM (Set_Password_OCP and Set_Password_SCM) */
    SET_PASSWORD("amt_SET_password")


    private final String fieldName

    CustomAppPermissions(String fieldName) {
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
