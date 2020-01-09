package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Block if build/deployment/revert/retry/rollback is still in progress
 */
try {
    log.trace("start " + issue.key)
    def inProgress = JiraUtils.getCustomFieldString(issue, CustomLists.REL_TASKS_IN_PROGRESS)
    if (inProgress == CustomLists.REL_TASKS_IN_PROGRESS.options.get(0)) {
        throw new InvalidInputException("Please wait and refresh, build/deployment/rollback tasks are still in progress")
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator " + e.toString())
} finally {
    log.trace("end")
}