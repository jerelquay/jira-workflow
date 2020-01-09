package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Validate that amt_tenant_PRD is empty or null
 */
try {
    log.debug("start " + issue.key)

    def appIssue = JiraUtils.getAppIssue(issue)

    // current release in mid of Production rollout
    //JiraUtils.updateCustomFieldValue(appIssue,CustomAppText.PRD_TENANT, "")
    String tenantPrd = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PRD_TENANT)
    tenantPrd = tenantPrd?.equalsIgnoreCase("null") ? "" : tenantPrd


    log.debug("tenantPrd: " + tenantPrd)
    if (tenantPrd?.trim()) { // if string has any content
        throw new InvalidInputException("Release \"" + tenantPrd + "\" is currently in production.")
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}