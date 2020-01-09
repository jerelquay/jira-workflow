package rmf.rel.conditions

import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check for permission to rollback a production
 */
try {
    //log.debug("start " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    passesCondition = JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.ROLLBACK)

} catch (Exception e) {
    log.error("script error", e)
    passesCondition = false
} finally {
    //log.debug("end. Is permission granted? " + passesCondition)
}
