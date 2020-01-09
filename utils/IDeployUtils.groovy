package rmf.utils
/**
 * Functions to abstract the underlying deployer implementation (Ansible, OCP, etc)
 */
interface IDeployUtils {

    /**
     * Deploy to the environment corresponding to the release issue's current status
     * <b>IMPT!!</b> If run by a "POST-ACTION" script, the issue's current status depends on
     * whether the script is ordered <i>before</i> or <i>after</i> the "Set Issue Status" post-action.
     * @return true if deploy script returns without error
     */
    boolean deploy(String environment)

    /**
     * Remove / undeploy the issue's latest release from the environment corresponding to the current status
     * <b>IMPT!!</b> If run by a "POST-ACTION" script, the issue's current status depends on
     * whether the script is ordered <i>before</i> or <i>after</i> the "Set Issue Status" post-action.
     * @return true if the remove script returns without error
     */
    boolean undeploy(String environment, String transition)

    /**
     * Rollback production environment to current PBL
     * @return true if rollback was successful
     */
    boolean rollback()

    /**
     * Initialize available hosts
     */
    void initHostsAvailability()
}