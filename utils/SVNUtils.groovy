package rmf.utils

import com.atlassian.jira.issue.Issue
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText
import rmf.utils.JiraProperties
import rmf.utils.JiraUtils

class SVNUtils implements ISCMUtils {
    private final Logger log = Logger.getLogger(SVNUtils.class.getTypeName())

    private final Issue releaseIssue
    private final Issue appIssue

    SVNUtils(Issue issue) {
        String issueType = issue?.getIssueType()?.getName()
        if (issueType.equals(JiraUtils.ISSUE_TYPE_REL)) {
            this.releaseIssue = issue
            this.appIssue = JiraUtils.getAppIssue(issue)
        } else {
            this.releaseIssue = null
            this.appIssue = issue
        }
    }

    @Override
    boolean validateScmDetails() {
        // check SCM has been set up properly
        def scmDevUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/dev"
        def scmRelUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/rel"
        def prdBaseline = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)

        def svncmd = ["svn", "list", scmRelUrl]
        boolean isSuccess = runSvnCmd(svncmd).getIsSuccess()

        if (!isSuccess) {
            log.warn("scm basic check 1/2 error")
        } else {

            svncmd = ["svn", "info", scmDevUrl]
            isSuccess = runSvnCmd(svncmd).getIsSuccess()

            if (!isSuccess) {
                log.warn("scm basic check 2/2 error")
            } else if (prdBaseline != null) {
                // validate PBL exists (if PBL is defined)
                svncmd = ["svn", "list", scmRelUrl + "/" + prdBaseline]
                isSuccess = runSvnCmd(svncmd).getIsSuccess()
            }
        }

        return isSuccess
    }

    @Override
    boolean validateDevBranchExists() {
        boolean isSuccess = false

        if (releaseIssue != null) {
            // check SCM has been set up properly
            def scmDevUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/dev"
            String scmDevFolder = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)

            if (scmDevFolder != null) {
                // perform SVN tag list
                isSuccess = runSvnCmd(["svn", "list", scmDevUrl + "/" + scmDevFolder])
            }
        }

	log.trace("validateDevBranchExists(): " + isSuccess)
        return isSuccess
    }

    @Override
    String branchDevelopment(String branchPath) {
        String scmUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL)
        def scmRelUrl = scmUrl + "/rel"
        def scmDevUrl = scmUrl + "/dev"
        String scmTemplateRepoUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_TEM_URL)
        String prdBaseline = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
        String pblRepoUrl = scmRelUrl + "/" + JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
        String devBranch = scmDevUrl + "/" + branchPath
        ArrayList<String> svncmd

        if (prdBaseline == null || prdBaseline.isEmpty()) {
            svncmd = ["svn", "copy", scmTemplateRepoUrl, devBranch, "-m", "Initialize Sub Development Repo by Jira " + devBranch]
        } else {
            svncmd = ["svn", "copy", pblRepoUrl, devBranch, "-m", "branch based on " + pblRepoUrl + " for development at: " + devBranch]
        }

        if (!runSvnCmd(svncmd).getIsSuccess()) {
            devBranch = null
        }

        return devBranch
    }

    @Override
    String tagMinorRelease() {
        String scanResults = null
        if (releaseIssue != null) {
            def props = JiraProperties.instance.properties
            def hostName = JiraUtils.getCustomFieldString(appIssue, CustomAppText.L1DX_SERVER)
            String sonarQubeURL = props[JiraProperties.PROTOCOL] + hostName + props[JiraProperties.SONARQUBE_URL]

            def scmRelUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/rel"
            def scmDevUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/dev"
            def scmDev = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
            def minorNumber = Integer.parseInt(JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.REL_NUM_MINOR)) + 1
            def releaseTag = scmDev + "_" + minorNumber
            def tagUrl = scmRelUrl + "/" + releaseTag

            def svncmd = ["svn", "copy", scmDevUrl + "/" + scmDev, tagUrl, "-m", "Tagged by Jira"]
            // tag
            if (runSvnCmd(svncmd).getIsSuccess()) {
                JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCM_REL, releaseTag)
                JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.REL_NUM_MINOR, Integer.toString(minorNumber))
                JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCM_REL_BRANCH_URL, tagUrl)

                // checkout
                String checkoutLocation = checkout(releaseTag)
                log.info("Checkout location: " + checkoutLocation)
                if (checkoutLocation != null) {
                    // send for scan
                    Tuple2<Boolean, String> result = JiraUtils.runCmd([checkoutLocation + "/automate/runScan.sh", scmDev, checkoutLocation, minorNumber])
                    sleep(5000)
                    if (result.get(0)) {
                        // extract scan results and update issue
                        def result2 = JiraUtils.runCmd([checkoutLocation + "/automate/getScanResults.sh", scmDev])
                        JiraUtils.updateCustomListValue(releaseIssue, CustomLists.REL_SCAN_RESULT, result2.get(1))
                        result2 = JiraUtils.runCmd([checkoutLocation + "/automate/getScanResultsDetailed.sh", scmDev])
                        JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCAN_RESULT_TABLE, result2.get(1))
                        JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SONARQUBE_URL, sonarQubeURL + scmDev)
                    } else {
                        // failed to run the scan
                        JiraUtils.updateCustomFieldValue(releaseIssue, CustomRelText.SCAN_RESULT_TABLE, "Failed to run scan")
                        JiraUtils.updateCustomListValue(releaseIssue, CustomLists.REL_SCAN_RESULT, CustomLists.REL_SCAN_RESULT.options.get(2))
                    }
                }
            }
        }

        return scanResults
    }

    /**
     *
     * @param scmRelUrl
     * @param releaseTag
     * @return checkout location
     */
    String checkout(String releaseTag) {
        boolean isSuccess = false

        final String scmRelUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/rel"
        String scmWorkspace = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
        String tempScmWorkspace = scmWorkspace + "/" + releaseTag

        // Clean up previous temporary workspace
        def scmd = ["rm", "-rf", tempScmWorkspace]
        def proc = scmd.execute()
        proc.waitFor()

        if (proc.exitValue() == 0) {
            // Pull code into the tempScmWorkspace
            log.trace("scmRelUrl: ${scmRelUrl}, releaseTag:${releaseTag}, tempScmWorkspace:${tempScmWorkspace}")
            isSuccess = runSvnCmd(["svn", "export", scmRelUrl + "/" + releaseTag, tempScmWorkspace])
            JiraUtils.runCmd(["chmod", "ugo+wx", "-R", tempScmWorkspace], scmWorkspace) // needed for ansible
        } else {
            log.error("cmd: " + scmd.join(" ") + System.getProperty("line.separator") + "\terror: " + proc.err.text)
        }

        return isSuccess ? tempScmWorkspace : null
    }

    @Override
    boolean removeCheckedoutCode() {
        boolean isSuccess = false
        String scmWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
        String releaseBranch = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
        if (!scmWorkpath?.isEmpty() && !releaseBranch?.isEmpty()) {
            def scmd = ["rm", "-rf", scmWorkpath + "/" + releaseBranch]
            isSuccess = JiraUtils.runCmd(scmd).get(0)
        }

        return isSuccess
    }

    @Override
    boolean trimTags() {
        boolean isSuccess = true

        if (releaseIssue == null) {
            isSuccess = false
        } else {
            final int minorNumber = Integer.parseInt(JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.REL_NUM_MINOR))
            def scmRelUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/rel"
            def scmDev = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
            String message = "clean up " + scmDev + " releases older than: " + minorNumber
            String scmPartialPath = scmRelUrl + "/" + scmDev + "_"

            for (int i = 1; i < minorNumber; i++) {
                // check that minorNumber exists (i.e. hasn'e been cleaned already)
                final String scmFullPath = scmPartialPath + i
                def svncmd = ["svn", "ls", scmFullPath]
                if (runSvnCmd(svncmd).getIsSuccess()) {
                    svncmd = ["svn", "delete", scmFullPath, "-m", message]
                    isSuccess = isSuccess && runSvnCmd(svncmd).getIsSuccess()
                }
            }
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

    private CommandLineObject runSvnCmd(List<String> svnCmd) {
        runSvnCmd(svnCmd, "")
    }

    // Method overloading
    private CommandLineObject runSvnCmd(List<String> svnCmd, String obsPass) {
        CommandLineObject commandLineObject = new CommandLineObject()
        commandLineObject.setIsSuccess(false)

        String scmUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_USER_ID)
        String obfuscated = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_USER_PID)
        if (obsPass != "") {
            obfuscated = obsPass
        }
        svnCmd = svnCmd + ["--non-interactive", "--trust-server-cert"]
        log.trace(svnCmd.join(" "))
        log.trace(scmUser + ":" + obfuscated + ", executing command: " + svnCmd.join(" "))

        if (scmUser != null && obfuscated != null) {
            String scmPasswd = JiraUtils.deobfuscate(obfuscated)

            def command = svnCmd + ["--username", scmUser, "--password", scmPasswd,"--non-interactive", "--trust-server-cert"]

            def proc = command.execute()
            proc.waitFor()

            if (proc.exitValue() == 0) {
                commandLineObject.setIsSuccess(true)
                commandLineObject.setOutput(proc.in.text)
                log.trace("SVN cmd success, start output:\n" + commandLineObject.getOutput())
                log.trace("SVN cmd success, end output.\n")
            } else {
                commandLineObject.setIsSuccess(false)
                commandLineObject.setOutput(proc.err.text)
                log.trace("SVN cmd failed: " + commandLineObject.getOutput())
            }
        }
        return commandLineObject
    }

    @Override
    List<String> listAutomateFiles() {
        def scmDev = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
        def scmDevUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/dev"
        def automateDirUrl = scmDevUrl + "/" + scmDev + "/automate"

        def svncmd = ["svn", "list", automateDirUrl]
        String output = runSvnCmd(svncmd).getOutput()
        List<String> files = output.split("\\r?\\n")

        return files
    }

    @Override
    boolean automateFilePathExists() {
        def scmDev = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH)
        def scmDevUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.SCM_URL) + "/dev"
        def automateDirUrl = scmDevUrl + "/" + scmDev + "/automate"

        def svncmd = ["svn", "list", automateDirUrl]

        return runSvnCmd(svncmd).getIsSuccess()
    }

    boolean checkCredentials(String hostName, String obsPass) {
        log.trace("Start checkCredentials() hostName:${} obsPass:${}")
        JiraProperties.initIpAddress()
        def props = JiraProperties.instance.properties
        def svnRepoUrl = props[JiraProperties.PROTOCOL] + hostName + props[JiraProperties.SVN_REPO_URL]
        def svncmd = ["svn", "list", svnRepoUrl]
        boolean output = runSvnCmd(svncmd,obsPass).getIsSuccess()
        // output = true
        log.trace("checkCredentials output: ${output}")
        log.trace("End checkCredentials()")
        return output
    }

    @Override
    boolean createRepos (String scmHostname, String appName, String obsPass) {
        def svncmd
        boolean isDevRepoSuccess = false
        boolean isRelRepoSuccess = false
        def props = JiraProperties.instance.properties
        def svnUrl = props[JiraProperties.PROTOCOL] + scmHostname + props[JiraProperties.SVN_REPO_URL]
        String repoUrl = svnUrl + appName
        String devRepoUrl = repoUrl + "/dev"
        String relRepoUrl = repoUrl + "/rel"
        String pbl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
        String pblRepoUrl = relRepoUrl + "/" + pbl
        String scmTemplateRepoUrl = svnUrl + props[JiraProperties.SCM_TEMPLATE_FILEPATH]

        boolean isSuccess = false

        try {
            // Create development repo
            svncmd = ["svn", "mkdir", devRepoUrl, "-m", "Initialize Development Repo by Jira", "--parents"]
            isDevRepoSuccess = runSvnCmd(svncmd, obsPass).getIsSuccess()
            // Create release repo
            svncmd = ["svn", "mkdir", relRepoUrl, "-m", "Initialize Release Repo by Jira"]
            isRelRepoSuccess = runSvnCmd(svncmd, obsPass).getIsSuccess()
            // Create release/pbl repo if PBL field is filled
            if (pbl != null && pbl.isEmpty() == false) {
                svncmd = ["svn", "copy", scmTemplateRepoUrl, pblRepoUrl, "-m", "Initialize Production Baseline Repo by Jira"]
                isRelRepoSuccess = runSvnCmd(svncmd, obsPass)
            }

            // Update custom field for Dev & Rel & Template Repo
            if (isDevRepoSuccess && isRelRepoSuccess) {
                JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.SCM_URL, repoUrl)
                JiraUtils.updateCustomFieldValue(appIssue, CustomAppText.SCM_TEM_URL, scmTemplateRepoUrl)
            }
            isSuccess = true

        } catch (Exception e) {
            log.trace("Exception at createRepo.\n{}", e)
        }

        return isSuccess
    }

    // TODO: Finalize the ACL & auth file in subversion
    /*@Override
    void appendAccessControl (String appName) {
        File file = new File("/home/devops/subversion-1.12.2-1/repository/conf/authz")
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))

        writer.newLine()
        writer.newLine()
        writer.write("[/" + appName + "/dev]")
        writer.newLine()
        writer.write("@admin = rw")
        writer.newLine()
        writer.write("@developer = rw")

        writer.newLine()
        writer.newLine()
        writer.write("[/" + appName + "/rel]")
        writer.newLine()
        writer.write("@admin = rw")
        writer.newLine()
        writer.write("@developer = rw")
        writer.close()
    }*/
}
