package rmf.app.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Set Nexus passwords
 */
log.debug("start, New Application: " + issue.key)
try {
    /*
    // Handle nexus admin password field
    def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.NEXUS_ADMIN_PASSWORD)
    if (pass?.trim()) { // check that the field contains non-whitespace characters

        // Reset password entry to blank
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_ADMIN_PASSWORD, "")

        //"Encrypt" password and save in pid field
        def obsPass = JiraUtils.obfuscate(pass)
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_ADMIN_PID, obsPass)

    } else { // ignore if the field only contains whitespace
        log.trace("NEXUS admin password field was empty")
    }

     */

    // Handle NEXUS user password field
    def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.NEXUS_USER_PASSWORD)
    if (pass?.trim()) { // check that the field contains non-whitespace characters

        // Reset password entry to blank
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_USER_PASSWORD, "")

        //"Encrypt" password and save in pid field
        def obsPass = JiraUtils.obfuscate(pass)
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_USER_PID, obsPass)

    } else { // ignore if the field only contains whitespace
        log.trace("NEXUS user password field was empty")
    }
} catch (Exception e) {
    log.error("script error", e)
} finally {
    log.debug("end")
}