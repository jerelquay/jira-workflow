package rmf.rel.postfunctions

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Check and update the generic message field if need to inform user to perform code merging with Production Base Line
 */
try {
    log.trace("start " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    def prdBaseLine = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    def currentCodebase = JiraUtils.getCustomFieldString(issue, CustomRelText.CURRENT_CODEBASE)

    if (prdBaseLine != null && !prdBaseLine.equals(currentCodebase)) {
        JiraUtils.updateMessageToUser(issue, "Production Baseline has changed! Please perform the code merge with current Production Baseline: " + prdBaseLine)
        JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, CustomLists.REL_IS_CODE_MERGED.getOptions().get(0))
    } else {
        JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, null)
    }

    // Update CR_scanResult & CR_scanResultTable
    JiraUtils.updateMessageToUser(issue, "Sent for rework")
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_SCAN_RESULT, "-")
    JiraUtils.updateCustomFieldValue(issue, CustomRelText.SCAN_RESULT_TABLE, "-")
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGE_VALIDATED, null)
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.trace("end")
}
