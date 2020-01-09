package rmf.utils
/**
 * SCM functions to abstract the underlying version control implementation (SVN, Git, etc)
 */
interface ISCMUtils {

    /**
     * Validate that the SCM can be connected to with the established credentials. Otherwise, the application issue needs to be updated with valid SCM information
     * @return
     */
    boolean validateScmDetails()

    /**
     * Validate that the dev branch exists
     * @return
     */
    boolean validateDevBranchExists()

    /**
     * Create a new development branch based on current PBL at the specified path
     * @param branchPath - relative to CustomAppText.SCM_DEV_URL
     * @return
     */
    String branchDevelopment(String branchPath)

    /**
     * Tag the development branch as a new minor version, checkout, scan and return results
     * @return extracted scan results
     */
    String tagMinorRelease()

    /**
     * Checkout code (without repository meta data) based on specific tag
     * @param scmRelUrl
     * @param releaseTag
     * @return checkout location
     */
    String checkout(String releaseTag)

    /**
     * Remove the code that was checked-out
     * @return true if executed successfully
     */
    boolean removeCheckedoutCode()

    /**
     * Remove all minor releases for an associated releaseIssue except the latest one
     * @return
     */
    boolean trimTags()

    /**
     * List files that exist in the url provided
     * @return
     */
    List<String> listAutomateFiles()

    /**
     * Using url to check if file path exists in SVN
     * @return
     */
    boolean automateFilePathExists()

    /**
     * Create Rel Repo in SVN
     * @return
     */
    boolean createRepos(String scmHostname, String appName, String obsPass)

    /**
     * TODO: Uncomment once ACL & Auth File is finalize
     * Append access control to svn authz file
     * @return
     */
    //void appendAccessControl(String appName)

}