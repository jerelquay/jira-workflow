package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Rollback production
 */
try {
    log.debug("start " + issue.key)
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_TASKS_IN_PROGRESS, CustomLists.REL_TASKS_IN_PROGRESS.options.get(0))
    JiraUtils.updateMessageToUser(issue, "Rollback in progress, refresh to check progress")

    new Thread() {
        void run() {
            try {
                boolean isSuccess = JiraUtils.getDeployUtils(issue).rollback()
                if (!isSuccess) {
                    log.error("rollback " + issue.key + " failed")
                    JiraUtils.updateMessageToUser(issue, "Rollback failed")
                }
                if (isSuccess) {
                    log.trace("Rollback completed successfully")
                    JiraUtils.updateMessageToUser(issue, "Successfully rollback")
                }
            } catch (Exception e) {
                log.error("rollback error", e)
                JiraUtils.updateMessageToUser(issue, "Rollback Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
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
