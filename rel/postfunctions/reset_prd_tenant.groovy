package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Reset amt_tenant_PRD to ""
 */
try {
    log.debug("start, for issue: " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.PRD_TENANT, "")
    JiraUtils.updateMessageToUser(issue, "")
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}