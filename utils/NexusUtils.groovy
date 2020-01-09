package rmf.utils


import com.atlassian.jira.issue.Issue
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import rmf.CustomAppText
import rmf.CustomLists
import rmf.CustomRelText

class NexusUtils {
    private static final Logger log = Logger.getLogger(NexusUtils.class)

    /**
     * Upload a single file (latest release corresponding to the specified release Issue
     * @param relIssue release Issue
     * @param folderPath folder containing files to be uploaded
     * @return (1) isSuccess (2.1) cmd output (2.2) cmd error stream output
     */
    static Tuple2<Boolean, List<String>> uploadRelease(Issue relIssue, String folderPath) {
        log.trace("Start uploadRelease: " + folderPath)
        def appIssue = JiraUtils.getAppIssue(relIssue)
        String nexusUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_URL)
        String nexusRepoType = JiraUtils.getCustomFieldString(appIssue, CustomLists.APP_NEXUS_REPO_TYPE)
        String nexusRepoName = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_REPO_NAME)
        String releaseTag = JiraUtils.getCustomFieldString(relIssue, CustomRelText.SCM_REL)

        Tuple2<Boolean, List<String>> results
        switch (nexusRepoType) {
            case CustomLists.APP_NEXUS_REPO_TYPE.options.get(0):
                // raw
                results = uploadRaw(appIssue, releaseTag, nexusUrl, nexusRepoName, folderPath)
                break
            case CustomLists.APP_NEXUS_REPO_TYPE.options.get(1):
                // maven2
                break
            default:
                log.warn("Unhandled nexus repo type: " + nexusRepoType)
                results = new Tuple2<Boolean, List<String>>(false, Collections.emptyList())
                break
        }

        if (log.isTraceEnabled()) {
            log.trace("End uploadRelease, isSuccess: " + results.get(0) + "\noutput_std: " + results.get(1).get(0) + "\noutput_err: " + results.get(1).get(1))
        }
        return results
    }

    /**
     * Note: does not handle very long results which require the use of continuation tokens expected usage should have only 1 result to be returned, if any
     * @param appIssue application issue
     * @param releaseTag e.g. <appname>_maj#_minor#
     * @return List of Nexus component ids that correspond to the release tag, empty list if nothing is found
     */
    static List<String> searchByRelease(Issue appIssue, String releaseTag) {
        log.trace("Start searchByRelease " + releaseTag)
        String nexusUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_URL)
        String nexusRepoName = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_REPO_NAME)

        Map components = searchFullByRelease(appIssue, nexusUrl, nexusRepoName, releaseTag)
        List<String> ids = new ArrayList<String>()

        if (components.containsKey("items")) {
            for (Map componentItem : (components.items as List<Map>)) {
                ids.add(componentItem.id as String)
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("End searchByRelease, ids: " + ids.join(", "))
        }
        return ids
    }

    static boolean deleteRelease(Issue appIssue, String releaseTag) {
        log.trace("Start deleteRelease " + releaseTag)
        boolean isSuccess = true
        final String nexusUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_URL)
        final List<String> componentIds = searchByRelease(appIssue, releaseTag)

        def cmd = ["curl", "-X", "DELETE", "", "-H", "accept: application/json"]
        for (String componentId : componentIds) {
            cmd[3] = nexusUrl + "/service/rest/v1/components/" + componentId
            isSuccess = runNexusCurlCmd(appIssue, cmd).get(0)
        }

        if (log.isTraceEnabled()) {
            log.trace("End deleteRelease, componentIds: " + componentIds.join(", "))
        }

        return isSuccess
    }

    /**
     * Downloads component assets stored in raw repositories (ignores other repo formats such as maven2)
     * @param appIssue
     * @param releaseTag
     * @param destinationDir
     * @return list of downloaded files (absolute paths)
     */
    static List<String> download(final Issue appIssue, final String releaseTag, final String destinationDir) {
        log.trace("Start download: " + releaseTag + " to " + destinationDir)
        final String nexusUrl = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_URL)
        final String nexusRepoName = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_REPO_NAME)
        def components = searchFullByRelease(appIssue, nexusUrl, nexusRepoName, releaseTag)

        List<String> downloadedFiles = new ArrayList<String>()
        boolean isSuccess = false
        if (components.containsKey("items")) {
            List<String> cmd = new ArrayList<String>()
            cmd.add("curl")

            for (Map componentItem : (components.items as List<Map>)) {
                if (componentItem.format == "raw") {
                    def assets = componentItem.assets
                    for (Map asset : assets) {
                        //assetsToDownload.add(new Tuple2(asset.path, asset.downloadUrl))
                        cmd.add(asset.downloadUrl as String)
                        cmd.add("-o")
                        // strip out the leading paths for now
                        String assetPath = asset.path as String
                        String absolutePath = destinationDir + "/" + assetPath.substring(assetPath.lastIndexOf("/") + 1)
                        downloadedFiles.add(absolutePath)
                        cmd.add(absolutePath)
                    }
                }
            }

            isSuccess = runNexusCurlCmd(appIssue, cmd)
        }

        if (log.isTraceEnabled()) {
            log.trace("End download, isSuccess: " + isSuccess + ", downloaded files: " + downloadedFiles.join(", "))
        }

        return downloadedFiles
    }

    /**
     *
     * @param appIssue
     * @param nexusUrl
     * @param nexusRepoName
     * @param releaseTag
     * @return
     */
    private static Map searchFullByRelease(Issue appIssue, String nexusUrl, String nexusRepoName, String releaseTag) {
        log.trace("Start searchFullByRelease from " + nexusRepoName + " repo, release: " + releaseTag)
        // need to put * in front of group because the group in raw repositories is "/releaseTag" while group in maven2 repositories is simply the releaseTag
        def cmd = ["curl", "-X", "GET", nexusUrl + "/service/rest/v1/search?repository=" + nexusRepoName + "&group=*" + releaseTag, "-H", "accept: application/json"]
        def searchResults = runNexusCurlCmd(appIssue, cmd)
        Map<String, Object> components

        if (searchResults.get(0)) {
            // success
            components = new JsonSlurper().parseText(searchResults.get(1).get(0)) as Map
            log.trace("End searchFullByRelease succeeded. Returned: " + searchResults.get(1).get(0))
        } else {
            components = new HashMap()
            log.error("End searchFullByRelease failed" + System.getProperty("line.separator") + "error: " + searchResults.get(1).get(1))
        }

        return components
    }

    private static Tuple2<Boolean, List<String>> uploadRaw(Issue appIssue, String releaseTag, String nexusUrl, String nexusRepoName, String folderPath) {
        log.trace("Start uploadRaw to " + nexusRepoName + " repo, release: " + releaseTag)
        List<String> cmd = new ArrayList()
        cmd.addAll(["curl", "-X", "POST", nexusUrl + "/service/rest/v1/components?repository=" + nexusRepoName, "-H", "accept: application/json", "-H", "Content-Type: multipart/form-data", "-F", "raw.directory=" + releaseTag])

        log.trace("Folder path: " + folderPath)

        File folder = new File(folderPath)
        int counter = 1
        for (File f : folder.listFiles()) {
            if (f.isFile()) {
                // "-F", "raw.asset1=@" + filePath, "-F", "raw.asset1.filename=" + fileName
                log.trace("File in loop: " + f.absolutePath)
                cmd.add("-F")
                cmd.add("raw.asset" + counter + "=@" + f.absolutePath)
                cmd.add("-F")
                cmd.add("raw.asset" + counter + ".filename=" + f.name)
                counter++
            } else {
                //shouldn'e have directories
                log.warn("Output folder should not have directories, ignoring: " + f.absolutePath)
            }
        }

        log.trace("End uploadRaw to " + nexusRepoName + " repo, release: " + releaseTag)
        return runNexusCurlCmd(appIssue, cmd)
    }

    static boolean trimTags(Issue releaseIssue) {
        boolean isSuccess = true
        if (releaseIssue == null) {
            isSuccess = false
            log.warn("releaseIssue should not be null when calling trimTags")
        } else if (releaseIssue.getIssueType().getName() != JiraUtils.ISSUE_TYPE_REL) {
            isSuccess = false
            log.warn("releaseIssue should be of type: " + JiraUtils.ISSUE_TYPE_REL + " when calling trimTags")
        } else {
            final int latestMinorNumber = Integer.parseInt(JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.REL_NUM_MINOR))
            final Issue appIssue = JiraUtils.getAppIssue(releaseIssue)
            final String partialTag = JiraUtils.getCustomFieldString(releaseIssue, CustomRelText.SCM_DEV_BRANCH) + "_"

            for (int minorNumber = 1; minorNumber < latestMinorNumber; minorNumber++) {
                isSuccess = isSuccess && deleteRelease(appIssue, partialTag + minorNumber)
            }
        }

        return isSuccess
    }

    /**
     *
     * @param appIssue
     * @param cmd
     * @return 1) success, 2) normal output, 3) error stream output
     */
    private static Tuple2<Boolean, List<String>> runNexusCurlCmd(Issue appIssue, List<String> cmd) {
        Boolean isSuccess = false
        log.debug("Start runNexusCurlCmd cmd: " + cmd.join(" "))

        String nexusUser = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_USER_ID)
        String obfuscated = JiraUtils.getCustomFieldString(appIssue, CustomAppText.NEXUS_USER_PID)
        String userAuth = nexusUser + ":" + JiraUtils.deobfuscate(obfuscated)

        cmd.add("-u")
        cmd.add(userAuth)
        cmd.add("-k")

        def proc = cmd.execute()
        proc.waitFor()
        List<String> output = new ArrayList<String>()
        output.add(proc.in.text)
        output.add(proc.err.text) // verbose output of curl gets sent to err stream
        if (proc.exitValue() == 0) {
            isSuccess = true
            log.trace("End runNexusCurlCmd success, output: " + output.get(0) + System.getProperty("line.separator") + "errOut: " + output.get(1))
        } else {
            log.trace(output.get(0))
            log.error("End runNexusCurlCmd failed, output: " + output.get(0) + System.getProperty("line.separator") + "errOut: " + output.get(1))
        }

        return new Tuple2<Boolean, List<String>>(isSuccess, output)
    }
}
