package rmf.app.postfunctions


import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())


/**
 * Modify SVN user password
 */
log.debug("start, Release: " + issue.key)
try {

    String scmType = JiraUtils.getCustomFieldString(issue, CustomLists.APP_SCM_TYPE)

    if (scmType.equals(CustomLists.APP_SCM_TYPE.getOptions().get(0))) {
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
    } else {
        def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_ACCESS_TOKEN)

        if (pass?.trim()) { // check that the field contains non-whitespace characters
            // Reset amt_scm_Password password entry to blank
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_ACCESS_TOKEN, "")

            //"Encrypt" password and save in pid field
            def obsPass = JiraUtils.obfuscate(pass)
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_USER_PID, obsPass)
        }
    }


//def CFM = ComponentAccessor.getCustomFieldManager()
    //JiraUtils.updateCustomListValue(issue, CustomLists.APP_SCM_TYPE, null)
//log.debug("v_p_tag: " + cfo.getName())
//cfo = CFM.getCustomFieldObject("customfield_10017")
//log.debug("v_p_branch: " + cfo.getName())
} catch (Exception e) {
    log.error("script error", e)
} finally {
    log.debug("end")
}
