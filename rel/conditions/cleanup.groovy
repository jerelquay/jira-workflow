package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to clean up the release's tags (remove all except latest)
 */
try {
    //log.debug("start " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    String currentStatus = issue.getStatus().getName()
    log.trace("Current Status: " + currentStatus)
    if (RelStatus.REJECTED.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.CLEAN_UP_REJ)
    } else if (RelStatus.CLOSE.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.CLEAN_UP_CLOSE)
    } else {
        log.warn("Unhandled status: " + currentStatus)
        passesCondition = false
    }
} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}
