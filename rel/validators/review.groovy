package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
final String[] requiredShellFiles = ["do_build.sh", "do_deploy.sh", "do_promote.sh", "do_revert.sh", "getScanResults.sh", "getScanResultsDetailed.sh", "runScan.sh"]

/**
 * Don'e allow to progress to review if code merge is required and isCodeMerged flag has not been set to "Yes".
 */
try {

    log.debug("start for issue: " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)
    def releaseIssue = issue;
    def prdBaseLine = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
    def currentCodebase = JiraUtils.getCustomFieldString(issue, CustomRelText.CURRENT_CODEBASE)


    // no matter which state we're in, check if code merge is required. If so, has code merge been marked as ready for review?
    if (prdBaseLine != null && prdBaseLine != currentCodebase) {
        def isCodeMerged = JiraUtils.getCustomFieldString(issue, CustomLists.REL_IS_CODE_MERGED)
        if (isCodeMerged == null || isCodeMerged == CustomLists.REL_IS_CODE_MERGED.getOptions().get(0)) {
            String message = "Production Baseline has changed (" + prdBaseLine + "). Please perform the code merge before sending for review."
            JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, CustomLists.REL_IS_CODE_MERGED.getOptions().get(0))
            JiraUtils.updateMessageToUser(issue, message)
            throw new InvalidInputException(message)
        }
    } else {
        JiraUtils.updateCustomListValue(issue, CustomLists.REL_IS_CODE_MERGED, null)
    }

    String currentStatus = issue.getStatus().getName()
    log.trace("CurrentStatus: " + currentStatus)

    if (RelStatus.WIP.equals(currentStatus) || RelStatus.REWORK.equals(currentStatus)) {
        // Validate that the SCM dev branch exists, so that we can tag a minor release later
        def scmUtils = JiraUtils.getScmUtils(issue)
	    boolean isDevBranchExist = scmUtils.validateDevBranchExists()	
        if (!isDevBranchExist) {
            // TODO :renable after figuring out why this is being invoked even when isDevBranchExist is true
            String message = "SCM Dev branch is missing, check SCM settings in: " + appIssue
            log.error(message)
            JiraUtils.updateMessageToUser(issue, message)
            throw new InvalidInputException(message)
        }

        // check that amt_workpath_scm (CustomAppText.JIRA_SCM_WORKPATH) has been set properly
        String scmWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
        File scmWorkPathFile = new File(scmWorkpath)
        log.trace("Current SCM Workpath is " + scmWorkpath + ", and does it exist? " + scmWorkPathFile.exists())
        if (scmWorkpath == null) {
            String message = "SCM Workpath is missing, check " + CustomAppText.JIRA_SCM_WORKPATH + " settings in: " + appIssue
            JiraUtils.updateMessageToUser(issue, message)
            throw new InvalidInputException(message)
        } else if (!scmWorkPathFile.exists() && !scmWorkPathFile.mkdirs()) {
            String message = "SCM Workpath " + scmWorkpath + "does not exist and could not be created, change " + CustomAppText.JIRA_SCM_WORKPATH + " settings in: " + appIssue + " or check the parent folder permissions"
            JiraUtils.updateMessageToUser(issue, message)
            throw new InvalidInputException(message)
        }

        // Check that automate directory exists
        if (JiraUtils.getScmUtils(issue).automateFilePathExists() == true) {
            // Ensure the list of necessary files are present in the automate folder
            List<String> outputFiles = JiraUtils.getScmUtils(issue).listAutomateFiles()
            List<String> requiredFiles = new ArrayList<String>() {{
                for (int i=0;i<requiredShellFiles.size();i++) {
                    add(requiredShellFiles[i])
                }
            }}
            log.trace(requiredShellFiles)
            requiredFiles.removeAll(outputFiles)

            if (requiredFiles.size() != 0) {
                String message = "Following files are missing in your repo: " + requiredFiles
                JiraUtils.updateMessageToUser(issue, message)
                log.trace(message)
                throw new InvalidInputException(message)
            }
        } else {
            String message = "Automate folder is missing in your repo"
            JiraUtils.updateMessageToUser(issue, message)
            log.trace(message)
            throw new InvalidInputException(message)
        }

    } else if (RelStatus.PEND_PRD.equals(currentStatus)) {
        // check the retry flag
        def retryFlag = JiraUtils.getCustomFieldString(issue, CustomLists.REL_RETRY_FLAG)
        if (CustomLists.REL_RETRY_FLAG.getOptions().get(1) == retryFlag) {
            throw new InvalidInputException("Not allowed to send for review; Deploy to PRD, then rollback to restore PRD environment")
        }
    } else {
        log.warn("v/review.groovy does not explicitly handle the status: " + currentStatus)
    }


} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}