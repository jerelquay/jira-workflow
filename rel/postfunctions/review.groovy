package rmf.rel.postfunctions


import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.ISCMUtils
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Tag a release and send for review
 */
try {
    log.debug("start for " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    def prdBaseLine = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    def currentCodebase = JiraUtils.getCustomFieldString(issue, CustomRelText.CURRENT_CODEBASE)

    // no matter which state we're in, prompt if user has to review a code merge (to new baseline)
    if (prdBaseLine != currentCodebase) {
        String message = "Need to review that code has merged to new Production Baseline: " + prdBaseLine
        JiraUtils.updateMessageToUser(issue, message)
    } else {
        JiraUtils.updateMessageToUser(issue, "")
    }

    // tag a minor release for review, checkout, scan and extract results in a new thread because it can be slow
    ISCMUtils scmUtils = JiraUtils.getScmUtils(issue)
    JiraUtils.updateCustomListValue(issue, CustomLists.REL_SCAN_RESULT, CustomLists.REL_SCAN_RESULT.options.get(0))
    JiraUtils.updateCustomFieldValue(issue, CustomRelText.SCAN_RESULT_TABLE, "Scanning in progress")
    new Thread() {
        void run() {
            try {
                scmUtils.tagMinorRelease()
            } catch (Exception e) {
                log.error("tag minor release error", e)
                JiraUtils.updateMessageToUser(issue, "Tag and scan error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
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
