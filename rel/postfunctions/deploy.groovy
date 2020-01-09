package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Perform deployment to the issue's new status (environment) and clean up the source code workspace
 */
try {
    final def newStatus = issue.getStatus().getName()
	
    //reset checkboxes and error messages
    JiraUtils.updateMessageToUser(issue, "")
    JiraUtils.updateMessageToUser(issue, "Deploying to " + newStatus )
	
	//Update messages
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(0))
    JiraUtils.updateMessageToUser(issue, "Deploy in progress, refresh to check progress")

    new Thread() {
        void run() {
            try {
                boolean isSuccess = JiraUtils.getDeployUtils(issue).deploy(newStatus)
                if (!isSuccess) {
                    log.error("promote/deploy " + issue.getSummary() + " to " + newStatus + " failed")
                }

                // if deployed to PRD, reset the retry flag
                if (RelStatus.PRD.equals(newStatus)) {
                    JiraUtils.updateCustomListValue(issue, CustomLists.REL_RETRY_FLAG, CustomLists.REL_RETRY_FLAG.getOptions().get(0))
                }

                if (isSuccess) {
					log.info("Deploy completed successfully")
                    JiraUtils.updateMessageToUser(issue, "Successfully deployed to " + newStatus)
                }
            } catch (Exception e) {
                log.error("Deploy script error", e)
                JiraUtils.updateMessageToUser(issue, "Deploy Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
            } finally {
                JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(1))
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
