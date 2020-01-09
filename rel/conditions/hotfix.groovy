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
    def typeDeployment = JiraUtils.getCustomFieldString(issue, CustomLists.REL_DEPLOY_TYPE)

    switch (currentStatus) {
        case RelStatus.PEND_SIT.getName():
            passesCondition = typeDeployment?.equals(CustomLists.REL_DEPLOY_TYPE.getOptions().get(1))
            passesCondition = passesCondition && JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.HOT_FIX_SKIP_SIT)
            break
        case RelStatus.SIT.getName():
            passesCondition = typeDeployment?.equals(CustomLists.REL_DEPLOY_TYPE.getOptions().get(2))
            passesCondition = passesCondition && JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.HOT_FIX_SKIP_UAT)
            break
    }

} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}

