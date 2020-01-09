package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check that the current code base == production base line
 */
try {
    log.debug("start " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    def prdBaseLine = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    def currentCodebase = JiraUtils.getCustomFieldString(issue, CustomRelText.CURRENT_CODEBASE)
    log.debug("Production Baseline: " + prdBaseLine + System.getProperty("line.separator") + "Current Codebase: " +
        currentCodebase)
    if (prdBaseLine != null && !prdBaseLine.equalsIgnoreCase(currentCodebase)) {
        throw new InvalidInputException("Production Baseline has changed! Please perform the code merge with current Production Baseline: " + prdBaseLine)
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator:" + System.getProperty("line.separator") + e.toString())
} finally {
    log.debug("end")
}
