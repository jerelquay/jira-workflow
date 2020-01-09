package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Upon entry to the PEND_PRD status, update the PRD tenant field
 */
try {
    log.debug("start, New Release " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    log.debug("associated Application: " + appIssue?.key)

    String scmRel = JiraUtils.getCustomFieldString(issue, CustomRelText.SCM_REL)

    //Update PRD tenant field
    JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.PRD_TENANT, scmRel)

} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}
