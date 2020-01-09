package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to approve a release in the open state (to progress to WIP state)
 */
try {
    //log.debug("start " + issue.key)
    def appIssue = JiraUtils.getAppIssue(issue)
    passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.APPROVE_RELEASE)
} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}