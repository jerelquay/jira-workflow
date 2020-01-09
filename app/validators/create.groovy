package rmf.app.validators


import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())

/**
 * Validate fields for creation of application
 * Summary will be set into app short name, which must be unique, less than 16 chars and cannot contain spaces
 *
 */
try {
    // Check name of application
    log.debug("start validating creation of new application issue: " + issue.toString())
    String appName = issue.getSummary().trim()

    if (appName.matches("(.*)\\s(.*)")) {
        throw new InvalidInputException("Summary will be used as short application name. This cannot contain spaces.")
    } else if (appName.length() >= 16) {
        throw new InvalidInputException("Summary will be used as short application name. This must be fewer than 16 characters.")
    } else {
        // check that app name is unique
        def allAppNames = JiraUtils.getAppShortNames(issue)

        if (allAppNames.contains(appName)) {
            throw new InvalidInputException("(Short) application name already exists, please use a different name " +
                "(summary).")
        }
    }

    // Check release
    String releaseNumber = JiraUtils.getCustomFieldString(issue, CustomAppText.REL_NUM_LATEST)
    if (releaseNumber == null || releaseNumber.isEmpty()) {
        throw new InvalidInputException(CustomAppText.REL_NUM_LATEST.toString() + " is required.")
    } else {
        int releaseNumInt
        try {
            releaseNumInt = Integer.parseInt(releaseNumber)
        } catch (Exception e) {
            throw new InvalidInputException(CustomAppText.REL_NUM_LATEST.toString() + " must be an integer, greater than or equal to 0.")
        }

        if (releaseNumInt < 0) {
            throw new InvalidInputException(CustomAppText.REL_NUM_LATEST.toString() + " must be an integer, greater than or equal to 0.")
        }
    }

    // Check SVN/GITLAB Credentials or Access
    String pass = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_USER_PASSWORD)
    String scmHostname = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_HOSTNAME)
    String scmType = JiraUtils.getCustomFieldString(issue, CustomLists.APP_SCM_TYPE)
    if (scmType.equals(CustomLists.APP_SCM_TYPE.getOptions().get(0))) {
        def obsPass = JiraUtils.obfuscate(pass)
        
        // Check whether credentials are correct
        boolean isValidCredentials = JiraUtils.getScmUtils(issue).checkCredentials(scmHostname ,obsPass)
        if(!isValidCredentials) {
            throw new InvalidInputException("Credentials are invalid. Please check credentials.")
        }
    } else {
        def accessToken = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_ACCESS_TOKEN)
        accessToken =JiraUtils.obfuscate(accessToken)
        int responseCode = JiraUtils.getScmUtils(issue).testGitlabConnection(scmHostname, accessToken)
        if (responseCode.equals(401)) {
            throw new InvalidInputException("Invalid Access Token")
        } else if (responseCode.equals(404)) {
            throw new InvalidInputException("Gitlab Server Not Found")
        } else if (responseCode.equals(301)) {
            throw new InvalidInputException("(301)Moved Permanently")
        } else if (responseCode.equals(200)) {
            responseCode = JiraUtils.getScmUtils(issue).getProject(appName, accessToken)
            if (responseCode.equals(200)) {
                throw new InvalidInputException("The application name \"" + appName + "\" exists in Gitlab")
            }
        }
    }
} catch (InvalidInputException e) {
    throw e
} catch (Exception e) {
    log.error("script error", e)
    throw new InvalidInputException("Script error please inform an administrator")
} finally {
    log.debug("end")
}