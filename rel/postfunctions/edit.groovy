package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Upon editing the release issue, update the summary appropriately, and prompt user to perform code merge if PBL has shifted
 */
try {
    log.debug("start for " + issue.key)

    String currentStatus = issue.getStatus().getName()
    log.info("currentStatus: " + currentStatus + " equals? " + RelStatus.REVIEW.equals(currentStatus))
    def codeBase = JiraUtils.getCustomFieldString(issue, CustomRelText.CURRENT_CODEBASE)
    def appIssue = JiraUtils.getAppIssue(issue)
    def pblCurrent = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)

    if (RelStatus.WIP.equals(currentStatus) || RelStatus.REWORK.equals(currentStatus)) {
        if (pblCurrent != codeBase) {
            def isCodeMerged = JiraUtils.getCustomFieldString(issue, CustomLists.REL_IS_CODE_MERGED)

            switch (isCodeMerged) {
                case (null): // not set yet
                    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, CustomLists.REL_IS_CODE_MERGED.options.get(0))
                    String message = "Production Baseline has changed! Please perform the code merge with " + pblCurrent
                    JiraUtils.updateMessageToUser(issue, message)
                    break
                case (CustomLists.REL_IS_CODE_MERGED.options.get(1)): // code reported as merged
                    String message = "Need to review the code merge to new PBL: " + pblCurrent
                    JiraUtils.updateMessageToUser(issue, message)
                    break
                default:
                    // do nothing
                    break

            }
        }

    } else if (RelStatus.REVIEW.equals(currentStatus)) {
        if (pblCurrent != codeBase) {
            def isCodeMergeReviewed = JiraUtils.getCustomFieldString(issue, CustomLists.REL_IS_CODE_MERGE_VALIDATED)

            switch (isCodeMergeReviewed) {
                case (null): // not set yet
                    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGE_VALIDATED, CustomLists.REL_IS_CODE_MERGE_VALIDATED.options.get(0))
                    String message = "Please review code merge to new PBL: " + pblCurrent
                    JiraUtils.updateMessageToUser(issue, message)
                    break
                case (CustomLists.REL_IS_CODE_MERGE_VALIDATED.options.get(1)): // claim that review has been performed
                    JiraUtils.updateCustomFieldValue(issue, CustomRelText.CURRENT_CODEBASE, pblCurrent)
                    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, null)
                    JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGE_VALIDATED, null)
                    String message = "Current code base updated to " + pblCurrent
                    log.info(message)
                    JiraUtils.updateMessageToUser(issue, message)
                    break
                default:
                    // do nothing
                    break

            }
        }

    } else if (RelStatus.OPEN.equals(currentStatus)) {
        // no actions

    } else {
        log.warn("p/edit.groovy does not explicitly handle the status: " + currentStatus)
    }
} catch (Exception e) {
    log.error("script error", e)
    JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
} finally {
    JiraUtils.updateReleaseSummary(issue)
    log.debug("end")
}
