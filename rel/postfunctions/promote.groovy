package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Upon exiting the stage, remove that state's OCP environment
 */
try {
    log.debug("start " + issue.key)
    final def currentState = issue.getStatus().getName()
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(0))

    new Thread() {
        void run() {
            try {
                boolean isSuccess = JiraUtils.getDeployUtils(issue).undeploy(currentState, "promote")

                if (!isSuccess) {
                    log.info("Promote " + issue.getSummary() + "from " + currentState + " environment failed")
                }

                // if revert from PRD to PEND_PRD (i.e. retry), set the retry flag
                if (RelStatus.PRD.equals(currentState)) {
                    JiraUtils.updateCustomListValue(issue, CustomLists.REL_RETRY_FLAG, CustomLists.REL_RETRY_FLAG.getOptions().get(1))
                }

                if (isSuccess) {
                    log.info("Promote successfully " + issue.getSummary() + "from " + currentState)
                }
            } catch (Exception e) {
                log.error("Promote script error", e)
                JiraUtils.updateMessageToUser(issue, "Promote error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
            } finally {
                JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(1))
            }
        }
    }.start()
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Promote error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}
