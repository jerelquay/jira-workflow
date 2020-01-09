package rmf.app.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Set OCP passwords
 */
log.debug("start, New Application: " + issue.key)
try {
    // Handle OCP admin password field
    def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.OCP_ADMIN_PASSWORD)
    if (pass?.trim()) { // check that the field contains non-whitespace characters

        // Reset amt_ocp_admin_password password entry to blank
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.OCP_ADMIN_PASSWORD, "")

        //"Encrypt" password and save in pid field
        def obsPass = JiraUtils.obfuscate(pass)
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_ADMIN_PID, obsPass)

    } else { // ignore if the field only contains whitespace
        log.trace("ocp password field was empty")
    }

    // Handle OCP user password field
    pass = JiraUtils.getCustomFieldString(issue, CustomAppText.OCP_USER_PASSWORD)
    if (pass?.trim()) { // check that the field contains non-whitespace characters

        // Reset amt_ocp_admin_password password entry to blank
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.OCP_USER_PASSWORD, "")

        //"Encrypt" password and save in pid field
        def obsPass = JiraUtils.obfuscate(pass)
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.OCP_USER_PID, obsPass)

    } else { // ignore if the field only contains whitespace
        log.trace("ocp password field was empty")
    }
} catch (Exception e) {
    log.error("script error", e)
} finally {
    log.debug("end")
}