package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Upon progressing a Release from PRD to CLOSE, update the PBL and clear the PRD tenant
 */
try {
    log.debug("start, New Release " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    log.debug("associated Application: " + appIssue?.key)

    String scmRel = JiraUtils.getCustomFieldString(issue, CustomRelText.SCM_REL)
    String oldPbl = JiraUtils.getCustomFieldString(issue, CustomAppText.PBL_CURRENT)

    //Update PBL to scmRel
    JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.PBL_CURRENT, scmRel)

    //Update PBL_PREV
    JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.PBL_PREV, oldPbl)

    //Clear PRD tenant field so that another release can enter the PEND_PRD loop
    JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.PRD_TENANT, "")

} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}