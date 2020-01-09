package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomRelPermissions
import rmf.app.AppStatus
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Validate that user is allowed to create releases
 */
try {
    log.debug("start validating creation of new release")
    def appIssue = JiraUtils.getAppIssue(issue)

    if (appIssue.getProjectId() != issue.getProjectId()) {
        throw new InvalidInputException("Invalid Application issue selected: not from the same project")
    } else if (!appIssue.getStatus().getName().equalsIgnoreCase(AppStatus.OPEN.getName())) {
        throw new InvalidInputException("Selected Application issue's status is not Open")
    }

    if (!JiraUtils.checkCustomPermission(appIssue, CustomRelPermissions.CREATE)) {
        throw new InvalidInputException("Not allowed to create releases, permission required: " +
            CustomRelPermissions.CREATE)
    }

} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}