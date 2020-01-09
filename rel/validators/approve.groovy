package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Upon approval of a release,
 * - validate SVN info and operations
 */
try {
    log.debug("start for issue: " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    if (appIssue == null) {
        throw new InvalidInputException("No associated application issue for this release. Something has gone terribly wrong")
    }

    // check SCM has been set up properly
    def scmUtils = JiraUtils.getScmUtils(issue)
    boolean isSuccess = scmUtils.validateScmDetails()

    if (!isSuccess) {
        log.error("Invalid SCM settings in Application Issue: " + appIssue.key)
        throw new InvalidInputException("Invalid SCM settings in Application Issue: " + appIssue.key)
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}