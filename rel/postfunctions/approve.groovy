package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Upon approval of a release,
 * - increment the application's release number
 * - assign release number (and scm dev branch path) to this release
 * - remember the code base
 * - create the SCM branch
 * - Update release's name (i.e. summary)
 */
try {
    log.debug("start for issue: " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    log.debug("associated Application: " + appIssue?.key)

    // increment the generated release number
    int newReleaseNumber = Integer.parseInt(JiraUtils.getCustomFieldString(appIssue, CustomAppText.REL_NUM_LATEST)) + 1
    JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.REL_NUM_LATEST, Integer.toString(newReleaseNumber))

    // update CR_Release_name_main (Developer Main working Branch for this release)
    String appName = appIssue.getSummary()
    String devBranchName = appName + "_" + newReleaseNumber
    JiraUtils.updateCustomFieldValue(issue, CustomRelText.SCM_DEV_BRANCH, devBranchName)

    // update CR_codebase_PRD_baseline (Current codebase of source)
    String prdBaseline = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    // Current Production Baseline: [app name]_[release number]_[minor #]
    JiraUtils.updateCustomFieldValue(issue, CustomRelText.CURRENT_CODEBASE, prdBaseline)

    // create SCM development branch
    def scmUtils = JiraUtils.getScmUtils(issue)
    String devBranchUrl = scmUtils.branchDevelopment(devBranchName)

    if (devBranchUrl == null) {
        log.error("Error creating the SCM development branch")
        JiraUtils.updateMessageToUser(issue, "Error creating the SCM development branch")
    } else {
        JiraUtils.updateMessageToUser(issue, "Development branch created successfully")

        // update release issue's name (i.e. summary)
        String currentStatus = issue.getStatus().getName()
        String releaseType = JiraUtils.getCustomFieldString(issue, CustomLists.REL_RELEASE_TYPE)
        String deploymentType = JiraUtils.getCustomFieldString(issue, CustomLists.REL_DEPLOY_TYPE)
        String trimmedSummary = JiraUtils.trimSummary(releaseType, deploymentType)
        JiraUtils.updateSummary(issue, (devBranchName + " : " + currentStatus + " (" + trimmedSummary + ") "))
        JiraUtils.updateCustomFieldValue(issue, CustomRelText.SCM_DEV_BRANCH_URL, devBranchUrl)
    }
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    log.debug("end")
}
