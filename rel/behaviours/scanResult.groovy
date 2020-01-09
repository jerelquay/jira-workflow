package rmf.rel.behaviours

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours
def overrideField = getFieldByName("Override Scan Result")
def codeScanResult = getFieldByName("Scan Result").getValue()

if (codeScanResult == "PASS") {
    overrideField.setHidden(true)
} else {
    overrideField.setHidden(false)
}
