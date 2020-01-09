package rmf.app.postfunctions

import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.utils.JiraProperties
import rmf.utils.JiraUtils

Logger log = Logger.getLogger(this.class.getTypeName())


/**
 * Upon creation of a new "Application" issue, handle SCM and Nexus passwords
 */
log.debug("Creating new Application, Release: " + issue.key)
try {
    def props = JiraProperties.instance.properties
    boolean isSuccess = false
    JiraProperties.initIpAddress()
    String appName = issue.getSummary()
    String scmHostname = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_HOSTNAME)
    String passwordContent = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_USER_PASSWORD)
    String scmType = JiraUtils.getCustomFieldString(issue, CustomLists.APP_SCM_TYPE)

    if (scmType.equals(CustomLists.APP_SCM_TYPE.getOptions().get(0))) {
        log.trace("SVN Utils")
        // Handle SCM password field
        def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_USER_PASSWORD)

        if (pass?.trim()) { // check that the field contains non-whitespace characters
            // Reset amt_scm_Password password entry to blank
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_USER_PASSWORD, "")

            //"Encrypt" password and save in pid field
            def obsPass = JiraUtils.obfuscate(pass)
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_USER_PID, obsPass)

            // Create development & release repository
            isSuccess = JiraUtils.getScmUtils(issue).createRepos(scmHostname, appName, obsPass)
        }
    } else {
        log.trace("GITLab Utils")
        def pass = JiraUtils.getCustomFieldString(issue, CustomAppText.SCM_ACCESS_TOKEN)

        if (pass?.trim()) { // check that the field contains non-whitespace characters
            // Reset amt_scm_Password password entry to blank
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_ACCESS_TOKEN, "")

            //"Encrypt" password and save in pid field
            def obsPass = JiraUtils.obfuscate(pass)
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_USER_PID, obsPass)
            isSuccess = JiraUtils.getScmUtils(issue).createRepos(scmHostname, appName, obsPass)
    }
    }
    
    if (isSuccess) {
        // TODO improve appending of access control
        // Append access control to svn authz file
        // JiraUtils.getScmUtils(issue).appendAccessControl(appName)

        // Initialize a list of available SIT/UAT servers for guarding entry to SIT/UAT
        JiraUtils.getDeployUtils(issue).initHostsAvailability()

        // Create workspace BIN & SCM
        JiraUtils.createWorkspace(issue, appName)

        // Update Nexus Repo custom field
        String nexusRepo = props[JiraProperties.PROTOCOL] + JiraProperties.getIpAddress() + props[JiraProperties.NEXUS_URL]
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_URL, nexusRepo)

        // Auto populate build path
        String buildUser = JiraUtils.getCustomFieldString(issue, CustomAppText.ENV_BUILD_USER)
        String buildFolder = props[JiraProperties.DEPLOYMENT_PATH] + buildUser + "/build"
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.ENV_BUILD_FOLDER, buildFolder)

        // Auto populate deployment folder & backup folder (SIT/UAT/PRD)
        String deploySITUser = JiraUtils.getCustomFieldString(issue, CustomAppText.ENV_SIT_USER)
        String deploySITFolder = props[JiraProperties.DEPLOYMENT_PATH] + deploySITUser + "/" + appName
        String deploySITBackupFolder = deploySITFolder + "_backup"
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.ENV_TARGET, deploySITFolder)
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.ENV_BACKUP, deploySITBackupFolder)

        String deployUATUser = JiraUtils.getCustomFieldString(issue, CustomAppText.ENV_UAT_USER)
        String deployUATFolder = props[JiraProperties.DEPLOYMENT_PATH] + deployUATUser + "/" + appName
        String deployUATBackupFolder = deployUATFolder + "_backup"
        //TODO create deployment/bkup folder UAT field in Jira

        String deployPRDUser = JiraUtils.getCustomFieldString(issue, CustomAppText.ENV_PRD_USER)
        String deployPRDFolder = props[JiraProperties.DEPLOYMENT_PATH] + deployPRDUser + "/" + appName
        String deployPRDBackupFolder = deployPRDFolder + "_backup"
        //TODO create deployment/bkup folder PRD field in Jira

        // Handle NEXUS user password field
        pass = JiraUtils.getCustomFieldString(issue, CustomAppText.NEXUS_USER_PASSWORD)
        if (pass?.trim()) { // check that the field contains non-whitespace characters

            // Reset password entry to blank
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_USER_PASSWORD, "")

            //"Encrypt" password and save in pid field
            def obsNexusPass = JiraUtils.obfuscate(pass)
            JiraUtils.updateCustomFieldValue(issue, CustomAppText.NEXUS_USER_PID, obsNexusPass)

        } else { // ignore if the field only contains whitespace
            log.trace("NEXUS user password field was empty")
        }

        JiraUtils.updateCustomFieldValue(issue, CustomAppText.L1DX_SERVER, JiraProperties.getIpAddress())
        JiraUtils.updateCustomFieldValue(issue, CustomAppText.SCM_HOSTNAME, scmHostname)
    } else { // ignore if the field only contains whitespace
        log.trace("scm password field was empty")
    }

    
} catch (Exception e) {
    log.error("script error\n{}", e)
    throw e
} finally {
    log.debug("end")
}
