import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript
import com.atlassian.jira.component.*

@BaseScript FieldBehaviours fieldBehaviours

def targetField = getFieldByName("Source Control Management")
def targetFieldValue = targetField.getValue()
def svnFieldName = getFieldByName("SCM User")
def svnFieldPassword = getFieldByName("SCM Password")
def gitFieldAccessToken = getFieldByName("SCM Access Token")
def gitFieldTemplateRepo = getFieldByName("SCM Template Path")

if (targetFieldValue == "GIT") {
    svnFieldName.setRequired(false)
    svnFieldPassword.setRequired(false)
    svnFieldName.setHidden(true)
    svnFieldPassword.setHidden(true)
    gitFieldAccessToken.setHidden(false)
    gitFieldAccessToken.setRequired(true)
    gitFieldTemplateRepo.setHidden(false)
    gitFieldTemplateRepo.setRequired(true)
} else {
    svnFieldName.setRequired(true)
    svnFieldPassword.setRequired(true)
    svnFieldName.setHidden(false)
    svnFieldPassword.setHidden(false)
    gitFieldAccessToken.setHidden(true)
    gitFieldAccessToken.setRequired(false)
    gitFieldTemplateRepo.setHidden(true)
    gitFieldTemplateRepo.setRequired(false)
}
