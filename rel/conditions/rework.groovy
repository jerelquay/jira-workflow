package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * check for permission to rework a release
 */
try {
    //log.debug("start " + issue.key)
    String currentStatus = issue.getStatus().getName()
    def appIssue = JiraUtils.getAppIssue(issue)

    switch (currentStatus) {
        case RelStatus.PEND_SIT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_PEND_SIT)
            break
        case RelStatus.REVIEW.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_REVIEW)
            break
        case RelStatus.SIT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_SIT)
            break
        case RelStatus.PEND_UAT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_PEND_UAT)
            break
        case RelStatus.UAT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_UAT)
            break
        case RelStatus.STAGING.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_STAGING)
            break
        case RelStatus.PEND_PRD.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_PEND_PRD)
            break
        case RelStatus.ROLLBACK.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REWORK_ROLLBACK)
            break
    }

} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}