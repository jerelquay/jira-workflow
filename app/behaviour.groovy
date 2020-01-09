package rmf.app

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def targetField = getFieldByName("Source Control Management")
def targetFieldValue = targetField.getValue()
def svnFieldName = getFieldByName("SCM User")
def svnFieldPassword = getFieldByName("SCM Password")

if (targetFieldValue == "GIT") {
	svnFieldName.setHidden(true)
	svnFieldPassword.setHidden(true)
	svnFieldName.setRequired(false)
	svnFieldPassword.setRequired(false)
} else {
	svnFieldName.setHidden(false)
	svnFieldPassword.setHidden(false)
	svnFieldName.setRequired(true)
	svnFieldPassword.setRequired(true)
}
