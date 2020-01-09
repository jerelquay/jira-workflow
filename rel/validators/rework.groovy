package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomLists
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Don'e allow to progress to rework if retry flag has been set to "Yes".
 */
try {

    def appIssue = JiraUtils.getAppIssue(issue)
    String currentStatus = issue.getStatus().getName()

    if (RelStatus.PEND_PRD.equals(currentStatus)) {
        // check the retry flag
        def retryFlag = JiraUtils.getCustomFieldString(issue, CustomLists.REL_RETRY_FLAG)
        if (CustomLists.REL_RETRY_FLAG.getOptions().get(1) == retryFlag) {
            throw new InvalidInputException("Not allowed to send for rework; Deploy to PRD, then rollback to restore PRD environment")
        }
    } else {
        log.warn("v/rework.groovy does not explicitly handle the status: " + currentStatus)
    }


} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}