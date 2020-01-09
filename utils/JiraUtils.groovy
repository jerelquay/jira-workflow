package rmf.utils

import com.atlassian.crowd.embedded.api.Group
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.*
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.ICustomField
import rmf.utils.GitlabUtils
import rmf.utils.JiraProperties
import rmf.utils.SVNUtils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JiraUtils {
    public static final String ISSUE_TYPE_APP = "Application"
    public static final String ISSUE_TYPE_REL = "Release"

    private static final CustomFieldManager CFM = ComponentAccessor.getCustomFieldManager()
    private static final OptionsManager OM = ComponentAccessor.getOptionsManager()
    private static final IssueManager IM = ComponentAccessor.getIssueManager()
    private static final Logger log = Logger.getLogger(JiraUtils.class)


    static List<MutableIssue> getAppIssues(Issue issue) {
        List<MutableIssue> appIssues = new ArrayList<Issue>()
        List<MutableIssue> allIssues = IM.getIssueObjects(IM.getIssueIdsForProject(issue.getProjectId()))

        for (MutableIssue projIssue : allIssues) {
            if (projIssue.getIssueType().getName().equals(ISSUE_TYPE_APP)) {
                appIssues.add(projIssue)
            }
        }

        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder()
            for (MutableIssue appIssue : appIssues) {
                sb.append(appIssue.getSummary())
                sb.append(System.getProperty("line.separator"))
            }

            //log.trace("getAppIssues list: " + sb.toString())
        }
        return appIssues
    }

    static List<String> getAppShortNames(Issue issue) {
        List<String> appNames = new ArrayList<String>()
        List<Issue> appIssues = getAppIssues(issue)

        for (Issue projIssue : appIssues) {
            appNames.add(projIssue.getSummary())
        }

        if (log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder()
            for (String name : appNames) {
                sb.append(name)
                sb.append(System.getProperty("line.separator"))
            }

            log.trace("getAppShortNames list: " + sb.toString())
        }

        log.trace("getAppShortNames end")
        return appNames
    }

    /**
     * Retrieve the application issue associated with the currentIssue (i.e. Release)
     * @param currentIssue - release
     * @return the associated application issue (if it exists), null otherwise
     */
    static MutableIssue getAppIssue(Issue currentIssue) {
        Issue appIssue = null

        if (currentIssue != null) {
            def issueType = currentIssue.getIssueType().getName()

            if (issueType.equalsIgnoreCase(ISSUE_TYPE_APP)) {
                appIssue = currentIssue
            } else {
                def appKey = getCustomFieldString(currentIssue, CustomRelText.APP_KEY)

                if (appKey == null) {
                    def cfo = CFM.getCustomFieldObjectsByName(CustomLists.OPEN_APPS.getFieldName()).first()
                    appIssue = currentIssue.getCustomFieldValue(cfo) as MutableIssue
                } else {
                    appIssue = IM.getIssueObject(appKey)
                }
            }
        }

        //log.trace("getAppIssue app issue's key: " + appIssue?.key)
        return appIssue
    }

    /**
     * Update the specified issue's summary (name)
     * @param issue
     * @param summary
     */
    static void updateSummary(Issue issue, String summary) {
        if (issue != null) {
            issue.setSummary(summary)
            // def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
            //IM.updateIssue(user, issue, UpdateIssueRequest.builder().eventDispatchOption(EventDispatchOption.ISSUE_UPDATED).sendMail(false).build())
        }
    }

    /**
     * Update the specified release issue's name
     * @param releaseIssue
     */
    static void updateReleaseSummary(Issue releaseIssue) {
        if (releaseIssue != null && releaseIssue.getIssueType().getName().equals(ISSUE_TYPE_REL)) {

            String currentStatus = releaseIssue.getStatus().getName()
            String scmDev = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
            if (scmDev == null) {
                def appIssue = JiraUtils.getAppIssue(releaseIssue)
                scmDev = appIssue.getSummary()
            }
            String releaseType = JiraUtils.getCustomFieldString(releaseIssue, CustomLists.REL_RELEASE_TYPE)
            String deploymentType = JiraUtils.getCustomFieldString(releaseIssue, CustomLists.REL_DEPLOY_TYPE)
            String trimmedSummary = JiraUtils.trimSummary(releaseType, deploymentType)
            releaseIssue.setSummary(scmDev + " : " + currentStatus + " (" + trimmedSummary + ") ")
            // def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
            //IM.updateIssue(user, issue, UpdateIssueRequest.builder().eventDispatchOption(EventDispatchOption.ISSUE_UPDATED).sendMail(false).build())
        }
    }

    /**
     * Obtain the string value from a custom field
     * @param issue - issue containing the field to be queried
     * @param field - custom field to query
     * @return value of the field (if it exists), null otherwise
     */
    static String getCustomFieldString(Issue issue, ICustomField field) {
        String val = null
        if (issue != null) {
            def cfoList = CFM.getCustomFieldObjectsByName(field.getFieldName())
            if (cfoList.size() > 0) {
                def origVal = issue.getCustomFieldValue(cfoList.first())
                val = getStringValue(origVal)
            }
        }

        //log.trace("getCustomFieldString value of " + field + " for " + issue?.key + ": '" + val + "'")
        return val
    }

    /**
     * Obtain string values from a custom field
     * @param issue - issue containing the field to be queried
     * @param field - custom field to query
     * @return value of the field (if it exists), null otherwise
     */
    static List<String> getCustomFieldValues(Issue issue, ICustomField field) {
        List<String> vals = new ArrayList<String>()
        if (issue != null) {
            def cfoList = CFM.getCustomFieldObjectsByName(field.getFieldName())
            // log.trace("getCustomFieldValues list size: " + cfoList.size())

            if (cfoList.size() > 0) {
                def origVal = issue.getCustomFieldValue(cfoList.first())
                // log.trace("getCustomFieldValues type: " + origVal.getClass().getTypeName())
                if (origVal instanceof List) {
                    for (Object obj : origVal as List) {
                        def strVal = getStringValue(obj)
                        if (strVal != null) {
                            vals.add(strVal)
                        }
                    }
                } else {
                    def strVal = getStringValue(origVal)
                    if (strVal != null) {
                        vals.add(strVal)
                    }
                }
            }
        }

        //log.trace("getCustomFieldValues end values of " + field + " for " + issue?.key + ": '" + vals.join(", ") + "'")
        return vals
    }

    private static String getStringValue(Object val) {
        String result
        if (val != null) {
            if (val instanceof Option) {
                result = (val as Option).getValue()
            } else {
                result = val.toString()
            }

            if (result.equalsIgnoreCase("null")) {
                result = null
            }
        }

        return result
    }

    /**
     * Update the field (in the specified issue) to a new value
     * @param issue - issue to be updated
     * @param field - Custom field
     * @param newValue - new value to update to
     * @return true if field was updated successfully. False otherwise
     */
    static boolean updateCustomFieldValue(Issue issue, ICustomField field, Object newValue) {

        boolean isSuccessfullyUpdated = false

        if (issue != null && field != null) {
            def cfoList = CFM.getCustomFieldObjectsByName(field.getFieldName())
            // log.trace("updateCustomFieldValue list size: " + cfoList.size())

            if (cfoList.size() > 0) {
                def cfo = cfoList.first()
                cfo.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(cfo), newValue), new
                        DefaultIssueChangeHolder())
                isSuccessfullyUpdated = true
                log.trace("updateCustomFieldValue " + cfo.fieldName + " successfully updated to '" + newValue + "'.")
            }
        }

        if (!isSuccessfullyUpdated) {
            log.warn("updateCustomFieldValue for " + issue?.key + ", field: " + field + ", to value: '" + newValue +
                    "'. Unsuccessful")
        }
        return isSuccessfullyUpdated
    }

    /**
     * Add a new option to the specified select list
     * @param issue - issue which contains the list
     * @param listField - Custom Field which contains a list
     * @param optionVal - string value of option to be selected
     * @return true if set successfully, false otherwise
     */
    static boolean updateCustomListValue(Issue issue, ICustomField listField, String optionVal) {
        boolean isSuccess = false
        def cfo = CFM.getCustomFieldObjectsByName(listField.getFieldName()).first()
        if (cfo != null) {
            def fieldConfig = cfo.getRelevantConfig(issue)

            if (fieldConfig != null) {
                Options currentOptions = OM.getOptions(fieldConfig)
                def oldValue = issue.getCustomFieldValue(cfo)
                if (optionVal == null) {
                    cfo.updateValue(null, issue, new ModifiedValue(oldValue, null), new DefaultIssueChangeHolder())
                } else {
                    def optionToSelect = currentOptions.getOptionForValue(optionVal, null)
                    cfo.updateValue(null, issue, new ModifiedValue(oldValue, optionToSelect), new DefaultIssueChangeHolder())
                }

                isSuccess = true
            }
        }

        if (isSuccess) {
            log.trace("updateCustomListValue success" + listField.getFieldName() + " successfully updated to '" + optionVal + "'.")
        } else {
            log.warn("updateCustomListValue Failed to update " + listField.getFieldName() + " to '" + optionVal + "'.")
        }

        return isSuccess
    }

    /**
     * Check if current user is part of the group allowed to perform the action/state transition
     * @param appIssue - application issue
     * @param field - Custom field which contains permission group(s)
     * @return true if user has permission
     */
    static boolean checkCustomPermission(Issue appIssue, ICustomField field) {
        def userUtil = ComponentAccessor.getUserUtil()
        def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

        // def grpName = getCustomFieldString(appIssue, field);
        def groupNames = getPermissionGroups(appIssue, field)

        boolean isGrant = userUtil.getGroupNamesForUser(currentUser.name).intersect(groupNames).size() > 0
        //log.trace("checkCustomPermission, is permission granted? " + isGrant)
        return isGrant
    }

    private static List<String> getPermissionGroups(Issue appIssue, ICustomField field) {
        def cfo = CFM.getCustomFieldObjectsByName(field.getFieldName())?.first()
        def groupNames = new ArrayList<String>()
        def groupsWithPermission = (java.util.List) appIssue.getCustomFieldValue(cfo)

        if (groupsWithPermission != null && groupsWithPermission.size() > 0) {
            for (Object group : groupsWithPermission) {
                def groupName = ((Group) group).getName()
                groupNames.add(groupName)
            }
        }

        return groupNames
    }


    /**
     * Terrible, fake obfuscation here
     * @param input - string to be obfuscated
     * @return obfuscated string
     */
    static String obfuscate(String input) {

        def inputLen = input.length()
        def delimiter = ':' as char
        StringBuilder sb = new StringBuilder(inputLen * 3)
        sb.append(inputLen)
        sb.append(delimiter)
        for (int i = inputLen - 1; i >= 0; i--) {
            //sb.append((int) input.charAt(i))
            sb.append(Integer.toHexString((int) input.charAt(i)))
            sb.append(delimiter)
        }

        sb.deleteCharAt(sb.length() - 1)
        def output = sb.toString()
        //log.trace("obfuscate, output is " + output)
        return output
    }

    /**
     * Terrible, fake obfuscation here
     * @param input - string to be obfuscated
     * @return obfuscated string
     */
    static String deobfuscate(String input) {
        String[] tokens = input.tokenize(":")
        int numChars = tokens.length - 1 //will ignore the first token
        StringBuilder sb = new StringBuilder(numChars)
        for (int i = numChars; i > 0; i--) {
            //log.trace("processing: " + tokens[i])
            int item = Integer.valueOf(tokens[i], 16)
            sb.append((char) item)
        }

        def output = sb.toString()
        //log.trace("deobfuscate, output is " + output)

        return output
    }

    /**
     * Update the message field (works for either Application issue or Release issue)
     * @param issue on which to display the message
     * @param message to be displayed to user
     */
    static void updateMessageToUser(Issue issue, String message) {
        def issueType = issue.getIssueType().getName()
        def msgField = issueType.equalsIgnoreCase(ISSUE_TYPE_APP) ? CustomAppText.ERROR_MSG : CustomRelText.MESSAGE
        LocalDateTime now = LocalDateTime.now()
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        String actualMessage = message.isEmpty() ? message : now.format(formatter) + ": " + message
        updateCustomFieldValue(issue, msgField, actualMessage)
    }

    /**
     * Get utilities/helper for executing SCM tasks
     * @param issue (app or release issue)
     * @return
     */
    static ISCMUtils getScmUtils(Issue issue) {
        log.trace("getScmUtils: " + issue)
        ISCMUtils scmUtils = null
        if (issue != null) {

            def appIssue = getAppIssue(issue)
            String scmType = getCustomFieldString(appIssue, CustomLists.APP_SCM_TYPE)
            if (scmType == CustomLists.APP_SCM_TYPE.getOptions().get(0)) {
                scmUtils = new SVNUtils(issue)
            } else if (scmType == CustomLists.APP_SCM_TYPE.getOptions().get(1)) {
                scmUtils = new GitlabUtils(issue)
            } else {
                log.error("Unhandled SCM type: " + scmType)
            }
        }
        return scmUtils
    }

    /**
     * Get utilities/helper for executing deployment related tasks
     * @param issue (app or release issue)
     * @return
     */
    static IDeployUtils getDeployUtils(Issue issue) {
        // TODO: check if using Ansible or OCP or ??
        return issue == null ? null : new AnsibleUtils(issue)
    }

    /**
     * Execute specified command in the commandline
     * @param cmd
     * @param optional working directory
     * @return 1)true if command exited normally 2) output from the command
     */
    static Tuple2<Boolean, String> runCmd(List<String> cmd, String workingDirectory = null) {
        boolean isSuccess = false
        String output
        log.trace(".runCmd(): " + cmd.join(" "))
        Process proc
        if (workingDirectory == null) {
            proc = cmd.execute()
        } else {
            ProcessBuilder pb = new ProcessBuilder(cmd)
            pb.directory(new File(workingDirectory))
            log.trace("working directory: " + workingDirectory)
            pb.environment() // WTF: if we don'e check this environment, ansible errors will happen
            //if (log.isTraceEnabled()) {
            //    log.trace(JsonOutput.prettyPrint(JsonOutput.toJson(pb.environment())))
            //}

            proc = pb.start()
        }

        proc.waitFor()

        if (proc.exitValue() == 0) {
            isSuccess = true
            output = proc.in.text
            log.trace("Run cmd success, ouput: " + output)
        } else {
            output = proc.in.text
            log.error("Run cmd fail, error: " + cmd.join(" ") + System.getProperty("line.separator") + "\terrorOut: " + output)
        }

        return new Tuple2<Boolean, String>(isSuccess, output)
    }

    static void createWorkspace(Issue issue, String appName) {
        log.trace("Creating workspace. appName: " + appName)
        def props = JiraProperties.instance.properties
        String workspace_scm = props[JiraProperties.WORKSPACE_SCM]
        String workspace_bin = props[JiraProperties.WORKSPACE_BIN]

        try {
            JiraUtils.runCmd(["mkdir", workspace_scm])
            JiraUtils.runCmd(["mkdir", workspace_bin])
        } catch (Exception e) {
            log.trace(e)
        } finally {
            // Update custom field for SCM & Bin workspace
            if (JiraUtils.runCmd(["mkdir", workspace_scm + appName]))
                JiraUtils.updateCustomFieldValue(issue, CustomAppText.JIRA_SCM_WORKPATH, workspace_scm + appName)
            if (JiraUtils.runCmd(["mkdir", workspace_bin + appName]))
                JiraUtils.updateCustomFieldValue(issue, CustomAppText.JIRA_BIN_WORKPATH, workspace_bin + appName)
        }
    }

    static String trimSummary(String rType, String dType) {
        log.trace("trimSummary start releaseType: ${rType} deploymentType: ${dType}")
        String releaseType
        String deploymentType
        String trimmedSummary

        if (rType.equals(CustomLists.REL_RELEASE_TYPE.getOptions().get(0))) {
            releaseType = "M" // Major
        } else {
            releaseType = "m" // Minor
        }
        if (dType.equals(CustomLists.REL_DEPLOY_TYPE.getOptions().get(0))) {
            deploymentType = "S" // Standard
        } else if (dType.equals(CustomLists.REL_DEPLOY_TYPE.getOptions().get(1))) {
            deploymentType = "HF-SIT"
        } else {
            deploymentType = "HF-UAT"
        }
        trimmedSummary = releaseType + ", " + deploymentType

        log.trace("trimSummary end trimmedSummary: ${trimmedSummary}")
        return trimmedSummary
    }
}
