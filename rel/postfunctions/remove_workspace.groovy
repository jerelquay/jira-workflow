package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * 1) Remove the checked-out source package from the JIRA server
 * 2) If called as part of the rebuild action, find and remove the corresponding binary from nexus
 */
try {
    log.debug("start")
    boolean isSuccess = JiraUtils.getScmUtils(issue).removeCheckedoutCode()
    if (isSuccess) {
        JiraUtils.updateMessageToUser(issue, "")
    } else {
        JiraUtils.updateMessageToUser(issue, "Error while trying to remove the checked-out source code")
    }
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line" +
        ".separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}