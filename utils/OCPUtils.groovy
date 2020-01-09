//package rmf.utils
//
//import com.atlassian.jira.issue.Issue
//import org.apache.log4j.Logger
//import rmf.CustomAppText
//import rmf.CustomRelText
//
//class OCPUtils implements IDeployUtils {
//    private final Logger log = Logger.getLogger(OCPUtils.class.getTypeName())
//
//    private final Issue releaseIssue
//    private final Issue appIssue
//
//    OCPUtils(Issue issue) {
//        String issueType = issue?.getIssueType()?.getName()
//        if (issueType.equals(JiraUtils.ISSUE_TYPE_REL)) {
//            this.releaseIssue = issue
//            this.appIssue = JiraUtils.getAppIssue(issue)
//        } else {
//            this.appIssue = issue
//        }
//    }
//
//    boolean build(String srcPath) {
//
//        String releaseBranch = JiraUtils.getCustomFieldString(issue, CustomRelText.SCM_REL)
//        String ocpWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_BIN_WORKPATH)
//        String ocpAuth = JiraUtils.getOcpAuth(appIssue)
//
//        def scmd = ["/bin/bash", srcPath + "/" + releaseBranch + "/" + ocpWorkpath + "/prepare_for_build.sh", releaseBranch, ocpAuth]
//        log.debug("OCP Build command: " + scmd)
//
//        def proc = scmd.execute()
//        proc.waitFor()
//        if (proc.exitValue() == 0) {
//            log.debug("OCP Build command success, output: " + proc.in.text)
//            JiraUtils.updateMessageToUser(issue, "")
//        } else {
//            log.error("OCP Build command error: " + scmd + System.getProperty("line.separator") + "\tError: " + proc.err
//                    .text)
//            JiraUtils.updateMessageToUser(issue, "Script Error, please inform an administrator." + System.getProperty("line.separator") + proc.err.text)
//        }
//
//        return true
//    }
//
//    boolean checkTag() {
//        String ocpAuth = JiraUtils.getOcpAuth(appIssue)
//
//        String ocpWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_OCP_WORKPATH)
//        String scmWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
//        String releaseBranch = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
//
//        def scmd = scmWorkpath + "/" + releaseBranch + "/" + ocpWorkpath + "/check-tag.sh " + releaseBranch + " " + ocpAuth
//        log.debug("cmd: " + scmd)
//        def proc = scmd.execute()
//        proc.waitForOrKill(100000)
//        return proc.exitValue() == 0
//    }
//
//    @Override
//    boolean deploy() {
//        boolean isSuccess = false
//        if (releaseIssue != null) {
//            String scmWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
//            String scmReleasePath = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
//            String stage = releaseIssue.getStatus().getName()
//
//            if (!scmWorkpath?.isEmpty() && !scmReleasePath?.isEmpty()) {
//                String ocpWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_BIN_WORKPATH)
//                cleanWorkspace(scmWorkpath, scmReleasePath, ocpWorkpath)
//
//                String ocpScript = scmWorkpath + "/" + scmReleasePath + "/" + ocpWorkpath + "/promote.sh"
//                String ocpAuth = getOcpAuth(appIssue)
//                isSuccess = runOcpScript(releaseIssue, ocpScript, scmReleasePath, stage, ocpAuth)
//            }
//        }
//
//        return isSuccess
//    }
//
//    @Override
//    boolean remove() {
//        boolean isSuccess = false
//        if (releaseIssue != null) {
//            String scmWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_SCM_WORKPATH)
//            String scmReleasePath = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_REL)
//            String stage = releaseIssue.getStatus().getName()
//
//            if (!scmWorkpath.isEmpty() && !scmReleasePath.isEmpty()) {
//                String ocpWorkpath = JiraUtils.getCustomFieldString(appIssue, CustomAppText.JIRA_BIN_WORKPATH)
//                String ocpScript = scmWorkpath + "/" + scmReleasePath + "/" + ocpWorkpath + "/remove.sh"
//                String ocpAuth = getOcpAuth(appIssue)
//                isSuccess = runOcpScript(releaseIssue, ocpScript, scmReleasePath, stage, ocpAuth)
//            }
//        }
//
//        return isSuccess
//    }
//
//    @Override
//    boolean rollback() {
//        return false
//    }
//
//    /**
//     * As part of the promotion process, clean up the release workspace
//     * @param scmWorkpath
//     * @param scmReleasePath
//     * @param ocpWorkpath
//     */
//    private void cleanWorkspace(String scmWorkpath, String scmReleasePath, String ocpWorkpath) {
//
//        String origDir = scmWorkpath + "/" + scmReleasePath
//        String tempDir = origDir + "_temp"
//        String ocpDir = origDir + "/" + ocpWorkpath
//
//        // 1. move ./ocp folder to temp folder
//        def lcmd = "mkdir " + tempDir
//        lcmd.execute().text
//        lcmd = "mv " + ocpDir + " " + tempDir
//        lcmd.execute().text
//
//        // 2. rm -rf release_tag folder
//        lcmd = "rm -rf " + origDir
//        lcmd.execute().text
//
//        // 3. create release_tag folder and move ./ocp folder back here
//        lcmd = "mkdir " + origDir
//        lcmd.execute().text
//        lcmd = "mv " + tempDir + "/" + ocpWorkpath + " " + origDir
//        lcmd.execute().text
//
//        // 4. rm -rf temp folder
//        lcmd = "rm -rf " + tempDir
//        lcmd.execute().text
//    }
//
//    /**
//     * Execute the specified OCP script
//     * @param issue
//     * @param ocpScript
//     * @param scmReleasePath
//     * @param stage - ("sit", "uat", "stg", "prd")
//     * @param ocpAuth - authentication string for OCP
//     * @return true if successful
//     */
//    private boolean runOcpScript(Issue issue, String ocpScript, String scmReleasePath, String stage, String
//            ocpAuth) {
//        boolean isSuccess = false
//
//        try {
//            // execute OCP script
//            def scmd = "/bin/bash " + ocpScript + " " + scmReleasePath + " " + stage + " " + ocpAuth
//            log.debug("OCP cmd: " + scmd)
//
//            def proc = scmd.execute()
//            proc.waitFor()
//
//            if (proc.exitValue() == 0) {
//                def output = proc.in.text
//                log.debug("OCP cmd success, output: " + output)
//                JiraUtils.updateMessageToUser(issue, "")
//                isSuccess = true
//            } else {
//                log.error("OCP cmd: " + scmd + System.getProperty("line.separator") + "error: " + proc.err.text)
//                JiraUtils.updateMessageToUser(issue, "Error running script, please inform an administrator." + System
//                        .getProperty("line.separator") + "\tcmd: " + scmd
//                        + System.getProperty("line.separator") + "\tError: " + proc.err.text)
//            }
//        } catch (Exception e) {
//            log.error("Script error", e)
//            JiraUtils.updateMessageToUser(issue, "Error running script, please inform an administrator." + System.getProperty
//                    ("line.separator") + "\e" + e.toString())
//        }
//
//        return isSuccess
//    }
//
//
//    /**
//     * construct OCP userid/password pair
//     * @param appIssue
//     * @return OCP Authentication String
//     */
//    static String getOcpAuth(Issue appIssue) {
//        String adminToken1 = JiraUtils.getCustomFieldString(appIssue, CustomAppText.OCP_ADMIN_TOKEN)
//        String adminToken2 = JiraUtils.getCustomFieldString(appIssue, CustomAppText.OCP_ADMIN_TOKEN2)
//        String clusterToken1 = JiraUtils.getCustomFieldString(appIssue, CustomAppText.OCP_TOKEN)
//        String clusterToken2 = JiraUtils.getCustomFieldString(appIssue, CustomAppText.OCP_TOKEN2)
//        return clusterToken1 + " " + adminToken1 + " " + clusterToken2 + " " + adminToken2
//    }
//
//}
