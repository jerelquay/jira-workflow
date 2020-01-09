package rmf.utils

import com.atlassian.jira.issue.Issue
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomRelText
import rmf.rel.RelStatus
import rmf.utils.JiraUtils

class AnsibleUtils implements IDeployUtils {
    private static final Logger log = Logger.getLogger(AnsibleUtils.class.getTypeName())
    private static final String NEWLINE = System.getProperty("line.separator")
    private static final String BUILD_SERVER = "[BuildServer]"
    private static final String USER = " ansible_user="
    public static final String HOSTS_INI_PATH = "/automate/hosts.ini"

    private final Issue releaseIssue
    private final Issue appIssue

    AnsibleUtils(Issue issue) {
        String issueType = issue?.getIssueType()?.getName()
        if (issueType.equals(JiraUtils.ISSUE_TYPE_REL)) {
            this.releaseIssue = issue
            this.appIssue = JiraUtils.getAppIssue(issue)
        } else {
            this.appIssue = issue
        }
    }

    // Initialize SIT/UAT server availability upon creating an application
    @Override
    void initHostsAvailability() {
        def sitArrayList = []
        def uatArrayList = []

        String[] sitServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_SIT).split(",")
        String[] uatServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_UAT).split(",")      

        for (String server : sitServers) {
            def sitHashMap = [:]
            sitHashMap.put("server", server.trim())
            sitHashMap.put("release","")
            sitArrayList.add(sitHashMap)
        }

        for (String server : uatServers) {
            def uatHashMap = [:]
            uatHashMap.put("server", server.trim())
            uatHashMap.put("release","")
            uatArrayList.add(uatHashMap)
        }

        String sitOutput = JsonOutput.toJson(sitArrayList)
        String uatOutput = JsonOutput.toJson(uatArrayList)

        JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_SIT_AVAILABILITY, sitOutput)
        JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_UAT_AVAILABILITY, uatOutput)

        log.trace("SIT JSONArray: " + sitOutput)
        log.trace("UAT JSONArray: " + uatOutput)
    }

    // TODO: SIT/UAT serialized zone in progress.
    boolean initHosts(String targetDirectory, String environment) {
        log.trace("initHosts(): Start")
        StringBuilder sb = new StringBuilder()
        def jsonSlurper = new JsonSlurper()

        boolean output = false

        // String builder to form the content of host.ini file based on environment
        if (environment == "build") {
            String buildUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BUILD_USER)
            String buildServer = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BUILD)
            sb.append(BUILD_SERVER).append(NEWLINE).append(buildServer).append(USER).append(buildUser).append(NEWLINE)
            output = true
        } else {
            log.trace("Environment: " + environment)
            if (environment == RelStatus.SIT.name) {
                String sitUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_SIT_USER)
                def availableSitServers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_SIT_AVAILABILITY))

                sb.append("[").append(RelStatus.SIT).append("]").append(NEWLINE)
                output = getAvailableServer(sb, availableSitServers, sitUser)
                log.trace("Available sit users: ")
                log.trace(availableSitServers)
                JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_SIT_AVAILABILITY, JsonOutput.toJson(availableSitServers))
                output = true
            }

            else if (environment == RelStatus.UAT.name) {
                String uatUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_UAT_USER)
                def availableUatServers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_UAT_AVAILABILITY))

                sb.append("[").append(RelStatus.UAT).append("]").append(NEWLINE)
                output = getAvailableServer(sb, availableUatServers, uatUser)
                JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_UAT_AVAILABILITY, JsonOutput.toJson(availableUatServers))
                output = true
            }

            else if (environment.equals(RelStatus.PRD.name)) {
                String prdUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_PRD_USER)
                String[] prdServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_PRD).split(",")
                sb.append("[").append(RelStatus.PRD).append("]").append(NEWLINE)
                for (String server : prdServers) {
                    sb.append(server).append(USER).append(prdUser).append(NEWLINE)
                }
                output = true
            }
        }

        if (output) {
            // Create a host.ini file and write the content into it
            File file = new File(targetDirectory + HOSTS_INI_PATH)
            boolean isTryDelete = file.exists()
            while (isTryDelete) {
                isTryDelete = !file.delete()
                log.trace("Delete existing file: " + file.toString())
            }
            file.createNewFile()
            log.trace("Start host.ini content.\n" + sb.toString())
            file.write(sb.toString())
            log.trace("End of host.ini content.\n")

        } 
        log.trace("initHosts(): End")
        return output
        
    }

    private boolean getAvailableServer(StringBuilder sb, List servers, String user, int noServers = 1) {
        // Check number of available servers
        log.trace("getAvailableServer() start")

        String releaseTag = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)

        for (sever in servers) {
            if (sever.release == "") {
                sever.release = releaseTag
                sb.append(sever.server).append(USER).append(user).append(NEWLINE)
                noServers--
            }
            if (noServers == 0) {
                break
            }
        }

        return true

        log.trace("getAvailableServer() end")
    }

    void updateAvailableServers() {
        log.trace("updateAvailableServers() Start")
        def jsonSlurper = new JsonSlurper()

        def environment = ["SIT","UAT"]
        String[] sitServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_SIT).split(",")
        String[] uatServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_UAT).split(",")
        def availableSitServers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_SIT_AVAILABILITY))
        def availableUatServers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_UAT_AVAILABILITY))

        environment.each {
            def customField = ""
            String server = ""
            String release = ""
            def envArrayList = []
            def availableServersArr = []
            def availableServers = ""
            def serverArray = ""
            boolean serverExists = false

            if (it == "SIT") {
                availableServers = availableSitServers
                serverArray = sitServers
            } else {
                availableServers = availableUatServers
                serverArray = uatServers
            }

            availableServers.each {  
                availableServersArr.add(it.server)
            }

            for (s in serverArray) {
                serverExists = availableServersArr.contains(s)
                if (!serverExists) {
                    server = s
                    release = ""
                } else {
                    availableServers.each{
                        if (it.server == s) {
                            log.trace(it.release)
                            log.trace(it.server)
                            release = it.release
                            server = it.server
                        }            
                    }
                }
                def hashMap = ["server":server,"release":release]
                envArrayList.add(hashMap)
            }

            String output = JsonOutput.toJson(envArrayList)
            if (it == "SIT"){
                JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_SIT_AVAILABILITY, output)
            } else {
                JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_UAT_AVAILABILITY, output)
            }
            log.trace("${it} JSONArray: " + output)
        }
        log.trace("updateAvailableServers() End")
    }

    /*
    void initHosts(String targetDirectory, String release) {
        String buildServer = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BUILD)
        String buildUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BUILD_USER)
        String[] sitServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_SIT).split(",")
        String sitUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_SIT_USER)
        String[] uatServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_UAT).split(",")
        String uatUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_UAT_USER)
        String[] prdServers = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_PRD).split(",")
        String prdUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_PRD_USER)

        StringBuilder sb = new StringBuilder()
        sb.append(BUILD_SERVER).append(NEWLINE).append(buildServer).append(USER).append(buildUser).append(NEWLINE)
        sb.append("[").append(RelStatus.SIT).append("]").append(NEWLINE)
        for (String server : sitServers) {
            sb.append(server).append(USER).append(sitUser).append(NEWLINE)
        }

        sb.append("[").append(RelStatus.UAT).append("]").append(NEWLINE)
        for (String server : uatServers) {
            sb.append(server).append(USER).append(uatUser).append(NEWLINE)
        }

        sb.append("[").append(RelStatus.PRD).append("]").append(NEWLINE)
        for (String server : prdServers) {
            sb.append(server).append(USER).append(prdUser).append(NEWLINE)
        }

        File file = new File(targetDirectory + HOSTS_INI_PATH)
        boolean isTryDelete = file.exists()
        while (isTryDelete) {
            log.trace("Deleting existing file: " + file.toString())
            isTryDelete = !file.delete()
        }

        file.createNewFile()
        file.write(sb.toString())
    }
    */

    @Override
    boolean deploy(String environment) {
        final String releaseTag = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
        return deployRelease(environment, releaseTag)
    }

    private boolean deployRelease(String environment, String releaseTag) {
        boolean isSuccess = true

        if (environment == null) {
            log.error("No Environment found")
        } else {
			String targetFolder = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_TARGET)
			String backupFolder = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BACKUP)

            final String workspace = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_BIN_WORKPATH) + "/" + releaseTag
            final String tempWorkspace = workspace + "/temp"
			final String scriptFile = "automate/do_deploy.sh"
            final String status = releaseIssue.getStatus().getName()
            try {
                JiraUtils.runCmd(["rm", "-rf", workspace])
                JiraUtils.runCmd(["mkdir", workspace])
                //TODO cater for other types of artifacts, such as container images
                List<String> downloadedFiles = NexusUtils.download(appIssue, releaseTag, workspace)
                for (String filePath : downloadedFiles) {
                    File file = new File(filePath)
                    if (file.exists()) {
                        if (file.isFile() && file.name.endsWith(".tar.gz")) {
                            JiraUtils.runCmd(["mkdir", tempWorkspace])
                            JiraUtils.runCmd(["tar", "-xvf", filePath], tempWorkspace)
                            initHosts(tempWorkspace,status)
                            Tuple2 output = JiraUtils.runCmd([scriptFile, targetFolder, backupFolder, filePath, environment], tempWorkspace)
                            log.trace("cmdSuccess: ${output.first}")
                            isSuccess = isSuccess && output.first
                            log.trace(isSuccess)
                        } else {
                            // TODO: deal with directories / other types of files
                        }
                    } else {
                        // file does not exist.. hmmm
                        log.error("Error while deploying, expected " + filePath + " to be downloaded but it is not present")
                    }
                }
            } catch (Exception e) {
                isSuccess = false
                throw e
            } finally {
                //JiraUtils.runCmd(["rm", "-rf", tempWorkspace])
                //JiraUtils.runCmd(["rm", "-rf", workspace])
            }
        }

        return isSuccess
    }

    @Override
    boolean undeploy(String environment, String transition) {
        log.trace(".undeploy() Start")
        final String targetFolder = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_TARGET)
        final String backupFolder = JiraUtils.getCustomFieldString(appIssue, CustomAppText.ENV_BACKUP)
        final String releaseTag = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
        String scriptFile = ""
        if (transition == "revert") {
            scriptFile = "automate/do_revert.sh"
        } else {
            scriptFile = "automate/do_promote.sh"
        }
        boolean isSuccess = true

        revertServer(releaseTag)

        if (environment == null) {
            log.error("Environment is null/empty")
        } else {
            final String workspace = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_BIN_WORKPATH) + "/" + releaseTag
            try {
                JiraUtils.runCmd(["rm", "-rf", workspace])
                JiraUtils.runCmd(["mkdir", workspace])
                //TODO cater for other types of artifacts, such as container images
                List<String> downloadedFiles = NexusUtils.download(appIssue, releaseTag, workspace)
                for (String filePath : downloadedFiles) {
                    File file = new File(filePath)
                    if (file.exists()) {
                        if (file.isFile() && file.name.endsWith(".tar.gz")) {
                            final String tempWorkspace = workspace + "/temp"
                            JiraUtils.runCmd(["mkdir", tempWorkspace])
                            JiraUtils.runCmd(["tar", "-xvf", filePath], tempWorkspace)
                            initHosts(tempWorkspace,releaseTag)
                            isSuccess = isSuccess && JiraUtils.runCmd([scriptFile, targetFolder, backupFolder, environment], tempWorkspace)
                            JiraUtils.runCmd(["rm", "-rf", tempWorkspace])
                        } else {
                            // TODO: deal with directories / other types of files
                        }
                    } else {
                        // file does not exist.. hmmm
                        log.error("Error while undeploying, expected " + filePath + " to be downloaded but it is not present")
                    }
                }
            } catch (Exception e) {
                isSuccess = false
                throw e
            } finally {
                JiraUtils.runCmd(["rm", "-rf", workspace])
            }
        }
        log.trace(".undeploy() End")
        return isSuccess
    }

    void revertServer(String releaseTag) {
        log.trace("In revertServer()")
        def appIssue = JiraUtils.getAppIssue(releaseIssue)
        final String status = releaseIssue.getStatus().getName()
        def jsonSlurper = new JsonSlurper()
        def servers

        log.trace("Release status: ${status}")

        if (status == RelStatus.SIT.name || status == RelStatus.PEND_SIT.name) {
            servers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_SIT_AVAILABILITY))
            loopServers(servers,releaseTag)
            JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_SIT_AVAILABILITY, JsonOutput.toJson(servers))

        } else if (status == RelStatus.UAT.name || status == RelStatus.PEND_UAT.name) {
            servers = jsonSlurper.parseText(JiraUtils.getCustomFieldValues(appIssue, CustomAppText.ENV_UAT_AVAILABILITY))
            loopServers(servers,releaseTag)
            JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.ENV_UAT_AVAILABILITY, JsonOutput.toJson(servers))
        } else {
            log.error("Invalid status. ")
        }

        log.trace("Result json: " + servers)
        log.trace("End revertServer()")

    }

    void loopServers(List servers,String releaseTag) {
        for (server in servers) {
            if (server.release == releaseTag) {
                server.release = ""
            }

        }
    }

    @Override
    boolean rollback() {
        return undeploy(RelStatus.PRD.name, "revert")
        // String productionBaseLine = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
        // if (productionBaseLine == null) {
        //    return true
        //} else {
        //    return deployRelease(RelStatus.PRD.name, productionBaseLine)
        //}
    }
}
