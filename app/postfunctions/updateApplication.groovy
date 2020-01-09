package rmf.app.postfunctions

import org.apache.log4j.Logger
import rmf.utils.AnsibleUtils

Logger log = Logger.getLogger(this.class.getTypeName())


/**
 * Upon creation of a new "Application" issue, handle SCM and Nexus passwords
 */
log.debug("Updating Application, Release: " + issue.key)
try {
    new AnsibleUtils(issue).updateAvailableServers()

} catch (Exception e) {
    log.error("script error\n{}", e)
    throw e
} finally {
    log.debug("end")
}
