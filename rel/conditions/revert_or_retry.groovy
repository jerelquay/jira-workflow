package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to revert from SIT to PEND_SIT
 */
try {
    //log.debug("start " + issue.key)
    String currentStatus = issue.getStatus().getName()
    def appIssue = JiraUtils.getAppIssue(issue)


    switch (currentStatus) {
        case RelStatus.SIT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVERT_SIT)
            break
        case RelStatus.UAT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVERT_UAT)
            break
        case RelStatus.PRD.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.REVERT_PRD)
            break
        default:
            log.error("Unhandled status: " + currentStatus)
            passesCondition = false
    }
} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    JiraUtils.updateReleaseSummary(issue)
    //log.debug("end. Is permission granted? " + passesCondition)
}