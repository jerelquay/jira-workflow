package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to progress a release to the REVIEW state
 */
try {
    //log.debug("start " + issue.key)
    def appIssue = JiraUtils.getAppIssue(issue)
    String currentStatus = issue.getStatus().getName()

    if (RelStatus.WIP.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVIEW_WIP)
    } else if (RelStatus.REWORK.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVIEW_REWORK)
    } else if (RelStatus.ROLLBACK.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVIEW_ROLLBACK)
    } else if (RelStatus.PEND_PRD.equals(currentStatus)) {
        passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVIEW_PEND_PRD)
    } else {
        log.warn("c/review.groovy does not explicitly handle the status: " + currentStatus)
    }
} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}