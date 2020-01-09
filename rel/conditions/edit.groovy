package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to edit the release
 */
try {
    //log.debug("start " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    String currentStatus = issue.getStatus().getName()
    log.trace("Current Status: " + currentStatus)
    if (RelStatus.WIP.equals(currentStatus) || RelStatus.REWORK.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.EDIT_WIP)
    } else if (RelStatus.REVIEW.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.EDIT_REVIEW)
    } else if (RelStatus.OPEN.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.EDIT_OPEN)
    } else {
        passesCondition = false
    }
} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}
