package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.AnsibleUtils
import rmf.utils.ISCMUtils
import rmf.utils.JiraUtils
import rmf.utils.NexusUtils

/**
 * 1) Clear the message field
 * 2) Clear the isCodeMerged and isCodeMergedValidated fields
 * 3) Set codebase to PBL
 */
final String buildScriptPath = "/automate/do_build.sh"
Logger log = Logger.getLogger(this.class.getTypeName())

try {
    log.debug("start " + issue.key)

    //reset checkboxes and error messages
    JiraUtils.updateMessageToUser(issue, "")
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, null)
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGE_VALIDATED, null)
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_SCAN_RESULT_OVERRIDE, null)

    // set current code base to PBL
    final def appIssue = JiraUtils.getAppIssue(issue)
    final def pbl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    JiraUtils.updateCustomFieldValue(issue, CustomRelText.CURRENT_CODEBASE, pbl)

    JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(0))
    JiraUtils.updateMessageToUser(issue, "Build in progress, refresh to check progress")

    // cleanup, checkout and build (in separate thread)
    new Thread() {
        void run() {
            try {
                def scmUtils = JiraUtils.getScmUtils(issue)
                scmUtils.removeCheckedoutCode()

                final String releaseTag = JiraUtils.getCustomFieldString(issue, CustomRelText.SCM_REL)
                final def checkoutLocation = scmUtils.checkout(releaseTag)

                final String environment = "build"
                final String outputLocation = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_BIN_WORKPATH) + "/" + releaseTag
                final String buildServer = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BUILD)
                final String buildWorkspace = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BUILD_FOLDER)
                new AnsibleUtils(issue).initHosts(checkoutLocation,environment)
				final def results = JiraUtils.runCmd([checkoutLocation + buildScriptPath, outputLocation, releaseTag, buildWorkspace], checkoutLocation)
                if (results.get(0)) {
                    // success
                    log.info("Build succeeded")
                    JiraUtils.updateMessageToUser(issue, "Build completed")
                    NexusUtils.uploadRelease(issue, outputLocation)
                    // allow to check if successfully uploaded to nexus
                    String nexusUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_URL)
                    String nexusRepoName = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_REPO_NAME)
                    JiraUtils.updateMessageToUser(issue, "Build completed. Uploaded to "+nexusUrl+"/#browse/browse:"+nexusRepoName+":"+releaseTag)
                } else {
                    log.error("Build failed: " + results.get(1))
                    JiraUtils.updateMessageToUser(issue, "Build script failed, please check the logs")
                }
            } catch (Exception e) {
                log.error("Script error", e)
                JiraUtils.updateMessageToUser(issue, "Build Error, please contact administrator." + System.getProperty("line.separator") + e.toString())
            } finally {
                JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(1))
                ISCMUtils scmUtils = JiraUtils.getScmUtils(issue)
                scmUtils.removeCheckedoutCode() // clean up
            }
        }
    }.start()
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}