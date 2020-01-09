package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Block if build/deployment/revert/retry/rollback is still in progress
 */
try {
    def jsonSlurper = new JsonSlurper()
    def appIssue = JiraUtils.getAppIssue(issue)
    final String status = issue.getStatus().getName()
    def availableServers

    log.trace("Current status: " + status)

    if (status == RelStatus.PEND_SIT.name) {
        availableServers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_SIT_AVAILABILITY))
    } else if (status == RelStatus.PEND_UAT.name) {
        availableServers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_UAT_AVAILABILITY))
    } else {
        throw new InvalidInputException("Invalid input. ")
    }

    log.trace("Available servers: " + availableServers)

    int freeServers = 0

    availableServers.each {
        log.trace("Release: " + it.release)
        if (it.release == "") {
            freeServers++
        }
    }

    if (freeServers == 0) {
        throw new InvalidInputException("There are no free servers available.")
    }

} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("There are no available servers for " + e.toString())
} finally {
    log.trace("end")
}