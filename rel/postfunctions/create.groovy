package rmf.rel.postfunctions


import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Upon creation of a new "Release" issue, update the release summary to <App Summary>_Unapproved_<Description>
 */
try {
    log.debug("Create New Release " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    log.debug("associated Application: " + appIssue?.key)

    // store the application issue's key, so that even if application is closed, we will retain the association
    JiraUtils.updateCustomFieldValue(issue, CustomRelText.APP_KEY, appIssue.key)

    String releaseType = JiraUtils.getCustomFieldString(issue, CustomLists.REL_RELEASE_TYPE)
    String deploymentType = JiraUtils.getCustomFieldString(issue, CustomLists.REL_DEPLOY_TYPE)
    String trimmedSummary = JiraUtils.trimSummary(releaseType, deploymentType)
    JiraUtils.updateSummary(issue, (appIssue.getSummary() + " : Open (" + trimmedSummary + ") "))

} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    log.debug("end")
}
