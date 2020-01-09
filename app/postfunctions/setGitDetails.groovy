package rmf.app.postfunctions

import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())


/**
 * Upon creation of a new "Application" issue, update the read-only custom fields, and add a new option to the application list (CR_App_name)
 */
log.debug("start, Release: " + issue.key)
try {
// Handle SCM password field
    def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_USER_PASSWORD)
    if (pass?.trim()) { // check that the field contains non-whitespace characters
        // Reset amt_scm_Password password entry to blank
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_USER_PASSWORD, "")

        //"Encrypt" password and save in pid field
        def obsPass = JiraUtils.obfuscate(pass)
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_USER_PID, obsPass)

    } else { // ignore if the field only contains whitespace
        log.trace("scm password field was empty")
    }
} catch (Exception e) {
    log.error("script error", e)
} finally {
    log.debug("end")
}

def CFM = ComponentAccessor.getCustomFieldManager()
def cfo = CFM.getCustomFieldObject("customfield_10045")
log.debug(issue.getStatus().getName())
//log.debug("v_p_tag: " + cfo.getName())
//cfo = CFM.getCustomFieldObject("customfield_10017")
//log.debug("v_p_branch: " + cfo.getName())