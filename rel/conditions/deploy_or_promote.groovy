package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.CustomRelPermissions
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to fast track a release to pend_prd
 */
try {
    //log.debug("start " + issue.key)
    String currentStatus = issue.getStatus().getName()
    def appIssue = JiraUtils.getAppIssue(issue)
    def deploymentType = JiraUtils.getCustomFieldString(issue, CustomLists.REL_DEPLOY_TYPE)

    switch (currentStatus) {
        case RelStatus.PEND_SIT.getName():
            def options = CustomLists.REL_DEPLOY_TYPE.getOptions()
            passesCondition = deploymentType.equals(options.get(0)) || deploymentType?.equals(options.get(2))
            passesCondition = passesCondition && JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.DEPLOY_SIT)
            break
        case RelStatus.SIT.getName():
            passesCondition = deploymentType.equals(CustomLists.REL_DEPLOY_TYPE.getOptions().get(0))
            passesCondition = passesCondition && JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.PROMOTE_UAT)
            break
        case RelStatus.PEND_UAT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.DEPLOY_UAT)
            break
        case RelStatus.UAT.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.PROMOTE_PRD)
            break
        case RelStatus.STAGING.getName():
            passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.PROMOTE_PRD)
            break
        case RelStatus.PEND_PRD.getName():
            def releaseType = JiraUtils.getCustomFieldString(issue, CustomLists.REL_RELEASE_TYPE)
            if (releaseType == CustomLists.REL_RELEASE_TYPE.getOptions().get(1)) {
                passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.DEPLOY_PRD_MIN)
            } else if (releaseType == CustomLists.REL_RELEASE_TYPE.getOptions().get(0)) {
                passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.DEPLOY_PRD_MAJ);
            } else {
                log.error("Unrecognised release type: " + releaseType)
                passesCondition = false
            }
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