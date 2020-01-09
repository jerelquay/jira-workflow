package rmf.app.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Update an Application's current and previous product baseline
 */
try {
    log.debug("start, for issue: " + issue.key)

    // update previous baseline
    def newValue = JiraUtils.getCustomFieldString(issue, CustomAppText.PBL_CURRENT)
    JiraUtils.updateCustomFieldValue(issue, CustomAppText.PBL_PREV, newValue)

    // update current baseline
    newValue = JiraUtils.getCustomFieldString(issue, CustomAppText.PBL_INPUT)
    JiraUtils.updateCustomFieldValue(issue, CustomAppText.PBL_CURRENT, newValue)
    JiraUtils.updateMessageToUser(issue, "")
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    log.debug("end")
}
