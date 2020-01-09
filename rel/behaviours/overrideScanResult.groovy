package rmf.rel.behaviours

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def overrideField = getFieldByName("Override Scan Result").getValue()
def commentField = getFieldById("comment")

if (overrideField == "Override") {
    commentField.setRequired(true)
} else {
    commentField.setRequired(false)
}
