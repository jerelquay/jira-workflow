package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomRelText
import rmf.utils.JiraUtils
import rmf.utils.NexusUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * check that the latest minor release exists in Nexus
 */
try {
    log.debug("start " + issue.key)
    def appIssue = JiraUtils.getAppIssue(issue)
    def releaseTag = JiraUtils.getCustomFieldString(issue, CustomRelText.SCM_REL)
    List<String> componentIds = NexusUtils.searchByRelease(appIssue, releaseTag)

    if (componentIds.size() > 0) {
        log.debug("Release exists in Nexus")
    } else {
        log.debug("Release does not exist in Nexus")
        throw new InvalidInputException("Release " + releaseTag + " does not exist in Nexus")
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator " + e.toString())
} finally {
    log.debug("end")
}