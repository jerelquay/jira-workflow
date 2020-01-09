package rmf.rel.postfunctions

import org.apache.log4j.Logger
import rmf.utils.JiraUtils
import rmf.utils.NexusUtils

Logger log = Logger.getLogger(this.class.getTypeName())
/**
 * Upon closing / rejecting, remove all minor release tags from SCM (except latest one)
 */
try {
    log.debug("start " + issue.key)

    new Thread() {
        void run() {
            try {
                boolean isSCMTrimSuccess = JiraUtils.getScmUtils(issue).trimTags()
                boolean isNexusTrimSuccess = NexusUtils.trimTags(issue)
                if (isNexusTrimSuccess) {
                    if (isSCMTrimSuccess) {
                        log.trace("trim tags success")
                        JiraUtils.updateMessageToUser(issue, "")
                    } else {
                        log.error("trim SCM tags " + issue.key + " failed")
                        JiraUtils.updateMessageToUser(issue, "Trim tags from SCM failed")
                    }
                } else {
                    if (isSCMTrimSuccess) {
                        log.error("trim Nexus tags " + issue.key + " failed")
                        JiraUtils.updateMessageToUser(issue, "Trim tags from Nexus failed")
                    } else {
                        log.error("trim SCM and Nexus tags " + issue.key + " failed")
                        JiraUtils.updateMessageToUser(issue, "Trim tags from SCM and Nexus failed")
                    }
                }
            } catch (Exception e) {
                log.error("trim tags error", e)
                JiraUtils.updateMessageToUser(issue, "trimTags Error, please inform an administrator." + System.getProperty("line.separator") + e.toString())
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
