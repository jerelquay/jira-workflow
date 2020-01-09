package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Check and ensure that everything is ready for build:
 * 1) code merge is marked as reviewed (if code merge is required)
 * 2) including successful pulling of code from SCM
 */
try {
    log.debug("start " + issue.key)
    // check if code merge is required
    def appIssue = JiraUtils.getAppIssue(issue)
    def prdBaseLine = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    def currentCodebase = JiraUtils.getCustomFieldString(issue, CustomRelText.CURRENT_CODEBASE)
    if (prdBaseLine != null && currentCodebase != prdBaseLine) {
        def isCodeMergeValidated = JiraUtils.getCustomFieldString(issue, CustomLists.REL_IS_CODE_MERGE_VALIDATED)
        if (CustomLists.REL_IS_CODE_MERGE_VALIDATED.getOptions().get(1) != isCodeMergeValidated) {
            // if anything but "Yes"
            throw new InvalidInputException("Code merge needs to be reviewed and validated first")
        }
    }

    // check code scan results
    String scanResult = JiraUtils.getCustomFieldString(issue, CustomLists.REL_SCAN_RESULT)
    switch (scanResult) {
        case CustomLists.REL_SCAN_RESULT.getOptions().get(0):
            // code scanning still in progress, wait for results
            throw new InvalidInputException("Code scanning still in progress, please wait for scan to complete (refresh)")
            break
        case CustomLists.REL_SCAN_RESULT.getOptions().get(1):
            // code scanning passed, proceed
            // do nothing
            break
        case CustomLists.REL_SCAN_RESULT.getOptions().get(2):
            // code scanning failed, rework or override
            boolean isCommentEmpty = (transientVars["comment"]).toString().equalsIgnoreCase("null")
            def overrideValues = JiraUtils.getCustomFieldValues(issue, CustomLists.REL_SCAN_RESULT_OVERRIDE)
            boolean isOverrideChecked = overrideValues.contains(CustomLists.REL_SCAN_RESULT_OVERRIDE.getOptions().get(0))

            if (!isOverrideChecked || isCommentEmpty) {
                throw new InvalidInputException("Code scanning failed, send code for rework OR key in comment and override ")
            }

            break
        default:
            log.warn("Unhandled code scan results: " + scanResult)
            break
    }

} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}