package rmf.rel.validators

import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger

Logger log = Logger.getLogger(this.class.getTypeName())
try {
    log.debug("start " + issue.key)

    boolean isCommentEmpty = (transientVars["comment"]).toString().equalsIgnoreCase("null")
    if (isCommentEmpty) {
        throw new InvalidInputException("Comment field required")
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}