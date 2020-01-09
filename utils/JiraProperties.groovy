package rmf.utils

import com.onresolve.scriptrunner.runner.ScriptRunner
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import groovy.util.logging.Log4j

import java.nio.file.Files

@Singleton
@Log4j
class JiraProperties {
    final String configFileName = 'rmf/config.properties'

    static final String SVN_REPO_URL = "SVN_REPO_URL"
    static final String GIT_REPO_URL = "GIT_REPO_URL"
    static final String WORKSPACE_BIN = "WORKSPACE_BIN"
    static final String WORKSPACE_SCM = "WORKSPACE_SCM"
    static final String WORKSPACE_SCMTEMPLATE = "WORKSPACE_SCMTEMPLATE"
    static final String DEPLOYMENT_PATH = "DEPLOYMENT_PATH"
    static final String SCM_PROTOCOL = "SCM_PROTOCOL"
    static final String PROTOCOL = "PROTOCOL"
    static final String NEXUS_URL = "NEXUS_URL"
    static final String SONARQUBE_URL = "SONARQUBE_URL"
    static final String SCM_TEMPLATE_FILEPATH = "SCM_TEMPLATE_FILEPATH"
    static String ipAddress

    public Properties getProperties() {
        def scriptRoots = ScriptRunnerImpl.getPluginComponent(ScriptRunner).getRootsForDisplay()?.split(", ")?.toList()
        //def scriptRoots = ['a', 'b']
        File propertiesFile = null
        for (root in scriptRoots) {
            propertiesFile = new File("$root/$configFileName")
            if (Files.isReadable(propertiesFile.toPath())) {
                log.info "Found ${propertiesFile.toPath()}"
                break
            }
        }
        // TODO: what if propertiesFile == null?
        Properties properties = new Properties()
        propertiesFile.withInputStream {
            properties.load(it)
        }
        properties
    }

    static initIpAddress() {
        log.trace("initIpAddress start")
        String output = "hostname -I".execute().text
        int lastIndex = output.indexOf(" ")
        ipAddress = output.substring(0, lastIndex)
        log.trace("initIpAddress end ${ipAddress.toString()}")
    }

    static String getIpAddress() {
        return ipAddress
    }
}
