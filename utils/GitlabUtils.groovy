package rmf.utils

import com.atlassian.jira.issue.Issue
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.rel.RelStatus

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


class GitlabUtils implements ISCMUtils{
    private static final Logger log = Logger.getLogger(GitlabUtils.class)

    private final Issue releaseIssue
    private final Issue appIssue

    Properties props = JiraProperties.instance.properties

    GitlabUtils(Issue issue) {
        String issueType = issue?.getIssueType()?.getName()
        if (issueType.equals(JiraUtils.ISSUE_TYPE_REL)) {
            this.releaseIssue = issue
            this.appIssue = JiraUtils.getAppIssue(issue)
        } else {
            this.releaseIssue = null
            this.appIssue = issue
        }
        trustAll()
    }

    static void trustAll(){
        // Create a trust manager that does not validate certificate chains
        def trustAllCerts = [
                new X509TrustManager(){
                    public X509Certificate[] getAcceptedIssuers() {return null;}
                    public void checkClientTrusted( X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted( X509Certificate[] certs, String authType) {}
                }
        ] as TrustManager[]

        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }


    // Get all projects
    void getAllProject() {
        log.trace("getProjects() Start")
        String address = getGitlabProjectApiUrl() + "?simple=true"
        def request = sendRequest(address, "GET")

        def getRC = request.getResponseCode();
        log.trace(getRC)
        if(getRC.equals(200)) {
            String output = request.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            log.trace(object)

            object.each { project ->
                println("${project.id} ${project.name}")
            }
        }
        log.trace("getProjects() End")
    }

    // Get a project
    int getProject(String appName, String accessToken) {
        log.trace("getProjects() Start")
        String url = getGitlabProjectApiUrl() + "/devops%2F" + appName

        def response = sendRequest(url, "GET", accessToken)
        def responseCode = response.getResponseCode()

        if(responseCode.equals(200)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            log.trace(object)
        }

        log.trace("getProjects() End")
        return responseCode
    }


    /****************************** REPOSITORY ******************************/
    // Create repository
    @Override
    boolean createRepos(String scmHostname, String appName, String obsAccessToken) {
        log.trace("createRepos: Start")
        def isSuccess = false

        String url = props[JiraProperties.SCM_PROTOCOL] + scmHostname + props[JiraProperties.GIT_REPO_URL] +  "projects"  + "?name=${appName}"

        def response = sendRequest(url,"POST",obsAccessToken)
        if (response.getResponseCode().equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)

            String projectId = object.id.toString()
            String scmUrl = object.web_url.toString()
            JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.GITLAB_PROJECTID, projectId)
            JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.SCM_URL, scmUrl)

            protectBranch(scmHostname, projectId, "rel-*",obsAccessToken)

            log.debug(object)

            String sshUrl = object.ssh_url_to_repo.toString()
            JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.GITLAB_SSH_URL, sshUrl)

            pushScmTemplate(sshUrl)
            isSuccess = true
        }

        log.trace("createRepos: End")
        return isSuccess
    }

    boolean pushScmTemplate(String sshUrl) {
        boolean isSuccess = false

        String workingDirectory = props[JiraProperties.WORKSPACE_SCMTEMPLATE]

        isSuccess = JiraUtils.runCmd(["bash", "movetemplate.sh", sshUrl], workingDirectory).get(0)

        log.trace("pushScmTemplate end " + isSuccess)
        return isSuccess
    }

    // TODO Remove repository
    boolean deleteRepo() {
        boolean isSuccess = false
        String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID)
        String address = getGitlabProjectApiUrl() + "/" + projectId
        def request = sendRequest(address,"DELETE")

        def response = request.getResponseCode()
        log.trace(response)
        if (response.getResponseCode().equals(202)) {
            isSuccess = true
        }
        return isSuccess
    }

    // TODO Share project with group
    void shareProject(int projectId, int groupId=8, int groupAccess=30) {
        String address = getGitlabProjectApiUrl() + "/${projectId}/share?group_id=${groupId}&group_access=${groupAccess}"
        def request = sendRequest(address,"POST")
    }
    /****************************** REPOSITORY ******************************/


    /****************************** BRANCHES ******************************/
    void createBranches(String newBranch, String fromBranch) {
        String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID)
        String url = getGitlabProjectApiUrl() + "/${projectId}/repository/branches?branch=${newBranch}&ref=${fromBranch}"

        def response = sendRequest(url,"POST")
        log.trace(response.getResponseCode())
    }

    void protectBranch(String scmHostname, String projectId, String branch, String token) {
        log.trace("protectBranch: Start")
        String url = props[JiraProperties.SCM_PROTOCOL] + scmHostname + props[JiraProperties.GIT_REPO_URL] + "${projectId}/protected_branches?name=${branch}&push_access_level=40"

        def response = sendRequest(url,"POST",token)
        if (response.getResponseCode().equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            log.debug(object)
        }

        log.trace("protectBranch: End")
    }

    int mergeBranches(String projectId, String fromBranch, String title, boolean removeSource=false, String toBranch="master") {
        log.trace("mergeBranches start")

        // Create merge request URL
        String url = getGitlabProjectApiUrl() + "/${projectId}/merge_requests?source_branch=${fromBranch}&target_branch=${toBranch}&title=${title}&remove_source_branch=${removeSource}"
        def response = sendRequest(url,"POST")
        def responseCode = response.getResponseCode()

        if (responseCode.equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)

            int iid = object.iid

            // Approve merge request URL
            url = getGitlabProjectApiUrl() + "/${projectId}/merge_requests/${iid}/merge?remove_source_branch=${removeSource}"
            response = sendRequest(url,"PUT")
            responseCode = response.getResponseCode()
        }

        log.trace("mergeBranches end")
        return responseCode
    }

    boolean validateDevBranchExists() {
        log.trace("validateDevBranchExists start")
        boolean isSuccess = false
        try {
            String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID).toString();
            String url = getGitlabProjectApiUrl() + "/${projectId}/repository/branches"
            def response = sendRequest(url, "GET")
            int responseCode = response.getResponseCode()

            if(responseCode.equals(200)) {
                isSuccess = true
            }
        } catch (Exception e) {
            log.trace("Exception at validateDevBranchExists.\n{}", e)
        }

        log.trace("validateDevBranchExists end")
        return isSuccess
    }

    String branchDevelopment(String branchName) {
        log.trace("branchdevelopment start")
        String scmUrl =  JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL)
        String devBranchName = "dev-" + branchName
        String instruction

        createBranches(devBranchName,"master")

        String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID).toString();
        protectTags(projectId, "tag-${branchName}*")

        String devBranchUrl = scmUrl + "/tree/" + devBranchName

        if (scmUrl.contains("https")) {
            instruction = "git -c http.sslVerify=false clone " +  scmUrl + " --branch " + devBranchName + " " + branchName
        } else {
            instruction = "git clone " +  scmUrl + " --branch " + devBranchName + devBranchName
        }
        JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCM_INSTRUCTION, instruction)

        log.trace("branchdevelopment end")
        return devBranchUrl
    }
    /****************************** BRANCHES ******************************/


    /****************************** TAGS ******************************/
    boolean protectTags(String projectId, String tagName) {
        log.trace("protectTag: Start")
        boolean isSuccess = false
        String url = getGitlabProjectApiUrl() + "/${projectId}/protected_tags?name=${tagName}&create_access_level=40"

        def response = sendRequest(url,"POST")
        if (response.getResponseCode().equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            isSuccess = true
        }
        log.trace("protectTag: End")
        return isSuccess
    }

    boolean unprotectTags(String projectId, String tagName) {
        log.trace("unprotectTags start")
        boolean isSuccess = false
        String url = getGitlabProjectApiUrl() + "/${projectId}/protected_tags/${tagName}"

        def response = sendRequest(url,"DELETE")
        if (response.getResponseCode().equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            isSuccess = true
        }
        log.trace("unprotectTags: End")
        return isSuccess
    }

    int deleteTags(String projectId, String tagName) {
        log.trace("deleteTags start")

        String url = getGitlabProjectApiUrl() + "/${projectId}/repository/tags/${tagName}"

        def response = sendRequest(url,"DELETE")
        def responseCode = response.getResponseCode()
        if (responseCode.equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            log.trace(object)
        }

        log.trace("deleteTags: End")
        return responseCode
    }

    // Create tag : return true if tag is created successfully else returns false and log error message
    boolean createTags(String tagName, String fromBranch, String tagMessage) {
        log.trace("createTags start tagName:${tagName} fromBranch:${fromBranch} tagMessage:${tagMessage}")

        boolean isSuccess = false
        String trimTagMessage = tagMessage.replaceAll("\\s", "%20")
        String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID).toString();

        String url = getGitlabProjectApiUrl() + "/${projectId}/repository/tags?tag_name=${tagName}&ref=${fromBranch}&message=${trimTagMessage}"

        try {
            def response = sendRequest(url,"POST")
            def responseCode = response.getResponseCode()

            if (responseCode.equals(201)) {
                log.trace("${responseCode.toString()}: Tag successfully created")
                isSuccess = true
            } else{
                String output = response.getErrorStream().getText()
                def jsonSlurper = new JsonSlurper()
                def object = jsonSlurper.parseText(output)
                log.trace("ERROR: from GITLab " + object["message"].toString() +", Please contact administrator.")
                isSuccess = false
            }

        } catch (Throwable e){
            log.trace ("Error createTags: ${e}")
            isSuccess= false
        }
        log.trace("createTags end")
        return isSuccess
    }
    /****************************** TAGS ******************************/


    /****************************** GROUPS & MEMBERS******************************/
    void getGroups() {
        String address = this.group_url
        def request = sendRequest(address, "GET")

        def getRC = request.getResponseCode()
        println(getRC);
        if(getRC.equals(200)) {
            String output = request.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            object.each {
                println("${it.id} ${it.name}")
            }
        }
    }

    // Create groups
    void createGroups(String groupName, String groupPath) {
        String address = this.group_url + "?name=${groupName}&path=${groupPath}"
        sendRequest(address,"POST")
    }

    void getMembers(int groupId) {
        String address = this.group_url + "/${groupId}/members"
        def request = sendRequest(address, "GET")

        def getRC = request.getResponseCode();
        println(getRC);
        if(getRC.equals(200)) {
            String output = request.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)

            object.each { member ->
                println("${member.id} ${member.name}")
            }
        }

    }

    // Add member to group
    void addMemberToGroup(int groupId, int userID, int access_level=30) {
        String address = this.group_url + "/${groupId}/members?user_id=${userID}&access_level=${access_level}"
        sendRequest(address,"POST")
    }
    /****************************** GROUPS & MEMBERS******************************/


    boolean validateScmDetails() {
        String address = getGitlabProjectApiUrl() + "?simple=true"
        def request = sendRequest(address, "GET")
        def getRC = request.getResponseCode()
        return getRC.equals(200)
    }

    int testGitlabConnection (String hostName, String accessToken) {
        log.trace("testGitlabConnection: Start")
        def responseCode
        try {
            String url = props[JiraProperties.SCM_PROTOCOL] + hostName + props[JiraProperties.GIT_REPO_URL] +  "projects"
            def response = sendRequest(url, "GET", accessToken)
            responseCode = response.getResponseCode()
        } catch (Exception e) {
            log.trace(e)
            responseCode = 404
        }
        log.trace("testGitlabConnection: End " + responseCode)
        return responseCode
    }

    @Override
    String tagMinorRelease() {
        log.trace("tagMinorRelease start")
        String scanResults = null

        if (releaseIssue != null) {
            boolean isSuccess = false

            try {
                def hostName = JiraUtils.getCustomFieldString(appIssue, CustomAppText.L1DX_SERVER)
                String sonarQubeURL = props[JiraProperties.PROTOCOL] + hostName + props[JiraProperties.SONARQUBE_URL]

                String scmDevBranchName = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
                int minorNumber = Integer.parseInt(JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.REL_NUM_MINOR)) + 1
                String scmUrl =  JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL)
                String releaseTagName = scmDevBranchName + "_" + minorNumber.toString()
                String scmTagName = "tag-" + releaseTagName

                String message = "Tag created from " + scmDevBranchName + " branch by JIRA."
                isSuccess = createTags(scmTagName, "dev-" + scmDevBranchName, message)

                if(isSuccess) {
                    JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCM_REL, releaseTagName)
                    JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.REL_NUM_MINOR, Integer.toString(minorNumber))
                    JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCM_REL_BRANCH_URL, scmUrl + "/tree/" + scmTagName)

                    //Checkout the code locally
                    String checkoutLocation = checkout(releaseTagName)

                      if (checkoutLocation != null) {
                        // send for scan
                        Tuple2<Boolean, String> result = JiraUtils.runCmd([checkoutLocation + "/automate/runScan.sh", scmDevBranchName, checkoutLocation, minorNumber])
                        sleep(5000)
                        if (result.get(0)) {
                            // extract scan results and update issue
                            def result2 = JiraUtils.runCmd([checkoutLocation + "/automate/getScanResults.sh", scmDevBranchName])
                            JiraUtils.updateCustomListValue(releaseIssue, CustomLists.REL_SCAN_RESULT, result2.get(1))
                            result2 = JiraUtils.runCmd([checkoutLocation + "/automate/getScanResultsDetailed.sh", scmDevBranchName])
                            JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCAN_RESULT_TABLE, result2.get(1))
                            JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SONARQUBE_URL, sonarQubeURL + scmDevBranchName)
                        } else {
                            // failed to run the scan
                            JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCAN_RESULT_TABLE, "Failed to run scan")
                            JiraUtils.updateCustomListValue(releaseIssue, CustomLists.REL_SCAN_RESULT, CustomLists.REL_SCAN_RESULT.options.get(2))
                        }
                    }

               }
//                    JiraUtils.updateMessageToUser(releaseIssue, tagMessage)
            } catch(Exception e){
                log.trace("Error: ", e)
            }
        }

        return scanResults
    }

    // TODO: Include this method in tagMinorRelease
    // Create release from tag
    void createRelease(String projectId, String tagName, String description) {
        log.trace("createRelease Start")
        String trimDescription = description.replaceAll("\\s", "%20");
        trimDescription = trimDescription.replaceAll("%20%20", "%0A")
        String url = getGitlabProjectApiUrl() + "/${projectId}/repository/tags/${tagName}/release?description=${trimDescription}"
        def response = sendRequest(url,"POST")
        def responseCode = response.getResponseCode()

        if (responseCode.equals(201)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
        }
        log.trace("createRelease End")
    }

    // GIT CLONE repo url/tag name
    String checkout (String releaseTag) {
        boolean isSuccess = false
        String scmDevBranchName = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
        String scmSshUrl =  JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_SSH_URL)
        String scmWorkspace = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
        String tempScmWorkspace = scmWorkspace + "/" + releaseTag

        // Clean up previous temporary workspace
        def cmd = ["rm", "-rf", tempScmWorkspace]
        def process = cmd.execute()
        process.waitFor()

        if (process.exitValue() == 0) {
            // Clone code into the tempScmWorkspace
            log.trace("checkout start scmUrl:${scmSshUrl}, scmDevBranchName:${scmDevBranchName}, scmWorkspace:${scmWorkspace}")
            isSuccess = JiraUtils.runCmd(["git", "clone", scmSshUrl, "--branch", "dev-" + scmDevBranchName, releaseTag], scmWorkspace).get(0)
            JiraUtils.runCmd(["chmod", "ugo+wx", "-R", tempScmWorkspace], scmWorkspace) // needed for ansible
        } else {
            log.error("cmd: " + cmd.join(" ") + System.getProperty("line.separator") + "\terror: " + proc.err.text)
        }

        return isSuccess ? tempScmWorkspace : null
    }

    //TODO
    boolean removeCheckedoutCode() {
        boolean isSuccess = false
        String scmWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
        String releaseBranch = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
        if (!scmWorkpath?.isEmpty() && !releaseBranch?.isEmpty()) {
            def scmd = ["rm", "-rf", scmWorkpath + "/" + releaseBranch]
            isSuccess = JiraUtils.runCmd(scmd).get(0)
        } else {
            if (scmWorkpath?.isEmpty()){
                log.error("removecheckedoutcode -> scmWorkpath is empty!")
            }
            if (releaseBranch?.isEmpty()){
                log.error("removecheckedoutcode -> releaseBranch is empty!")
            }
        }

        String status = releaseIssue.getStatus().getName()

        if (status == RelStatus.PRD.name) {
            String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID).toString()
            String minorNumber = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.REL_NUM_MINOR)
            String devBranchName = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
            String description = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.DESCRIPTION)
            String fromBranch = "dev-" + devBranchName
            mergeBranches(projectId, fromBranch, fromBranch)
            createRelease(projectId, "tag-${devBranchName}_${minorNumber}", description )
        }

        return isSuccess
    }


    @Override
    boolean trimTags () {
        log.trace("trimTags start")
        boolean isSuccess = false

        if (releaseIssue == null) {
            isSuccess = false
        } else {
            final int minorNumber = Integer.parseInt(JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.REL_NUM_MINOR))
            def devBranchName = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
            log.trace("trimtags() clean up " + devBranchName + " branch with releases smaller than: " + minorNumber)
            String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID)

            unprotectTags(projectId,"tag-${devBranchName}*")
            protectTags(projectId,"tag-${devBranchName}_${minorNumber}")

            for (int i = 1; i < minorNumber; i++) {
                // check that minorNumber exists (i.e. has not been cleaned already)
                deleteTags(projectId, "tag-${devBranchName}_${i.toString()}")
            }
            isSuccess = true
        }

        log.trace("trimTags end")
        return isSuccess
    }

    @Override
    List<String> listAutomateFiles () {
        List<String> files = new ArrayList<>()
        String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID)
        String devBranchName = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
        String url = getGitlabProjectApiUrl() + "/${projectId}/repository/tree?path=automate&ref=dev-${devBranchName}"
        log.trace(url)

        def response = sendRequest(url, "GET")
        if (response.getResponseCode().equals(200)) {
            String output = response.getInputStream().getText()
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(output)
            object.each { obj ->
                files.add(obj.name)
            }
        }

        return files
    }

    @Override
    boolean automateFilePathExists() {
        boolean isSuccess = false
        String projectId = JiraUtils.getCustomFieldString(appIssue, CustomAppText.GITLAB_PROJECTID)
        String devBranchName = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
        String url = getGitlabProjectApiUrl() + "/${projectId}/repository/tree?path=automate&ref=dev-${devBranchName}"
        log.trace(url)

        def response = sendRequest(url, "GET")
        if (response.getResponseCode().equals(200)) {
            isSuccess = true
        } else {
            isSuccess = false
        }

        return isSuccess
    }

    private class CommandLineObject {
        String output;
        boolean isSuccess;

        String getOutput() {
            return output
        }

        void setOutput(String output) {
            this.output = output
        }

        boolean getIsSuccess() {
            return isSuccess
        }

        void setIsSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess
        }
    }

    // Method overloading
    private CommandLineObject runGitCmd(List<String> gitCmd) {
        CommandLineObject commandLineObject = new CommandLineObject()
        commandLineObject.setIsSuccess(false)

        log.trace("GIT running CMD: ${gitCmd.join(' ')}")

        def command = gitCmd

        def proc = command.execute()
        proc.waitFor()

        if (proc.exitValue() == 0) {
            commandLineObject.setIsSuccess(true)
            commandLineObject.setOutput(proc.in.text)
            log.trace("GIT cmd success, start output:\n" + commandLineObject.getOutput())
            log.trace("GIT cmd success, end output.\n")
        } else {
            commandLineObject.setIsSuccess(false)
            commandLineObject.setOutput(proc.err.text)
            log.trace("GIT cmd failed: " + commandLineObject.getOutput())
        }

        return commandLineObject
    }

    def sendRequest(String url, String method) {
        sendRequest(url, method, "")
    }

    // Method Overloading
    def sendRequest(String url, String method, String obsAccessToken) {

        String token = obsAccessToken ? obsAccessToken : JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_USER_PID)

        HttpURLConnection req = new URL(url).openConnection()
        switch (method) {
            case "POST":
                req.setRequestMethod("POST")
                break
            case "DELETE":
                req.setRequestMethod("DELETE")
                break
            case "PUT":
                req.setRequestMethod("PUT")
                break
            default:
                break
        }

        req.setRequestProperty("PRIVATE-TOKEN",JiraUtils.deobfuscate(token) )
        log.trace ("sendRequest [${method}] Response: (${req.getResponseCode().toString()}) url: ${url} ")
        return req
    }

    String getGitlabProjectApiUrl() {
        return props[JiraProperties.SCM_PROTOCOL] + JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_HOSTNAME) + props[JiraProperties.GIT_REPO_URL] + "projects"
    }
}
