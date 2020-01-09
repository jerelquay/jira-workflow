package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.utils.ISCMUtils
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Check that the dev branch exists in SCM
 */
try {
    log.debug("start for issue: " + issue.key)
    ISCMUtils scmUtils = JiraUtils.getScmUtils(issue)

    if (!scmUtils.validateDevBranchExists()) {
        throw new InvalidInputException("scm development branch for issue " + issue.key + " " + issue + " cannot be accessed")
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}