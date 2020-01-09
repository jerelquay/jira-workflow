package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomRelText
import rmf.utils.JiraUtils
import rmf.utils.NexusUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Called as part of the rebuild action, find and remove the corresponding binary from nexus
 */
try {
    log.debug("start")
    def appIssue = JiraUtils.getAppIssue(issue)
    def releaseTag = JiraUtils.getCustomFieldString(issue, CustomRelText.SCM_REL)
    NexusUtils.deleteRelease(appIssue, releaseTag)
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line" +
            ".separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}