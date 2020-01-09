package rmf.rel.behaviours

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript
import org.apache.log4j.Logger

@BaseScript FieldBehaviours fieldBehaviours

def log = Logger.getLogger("rmf.behaviour.CR_isCodeMergeValidated")

def isCodeMergeReviewedField = getFieldByName("CR_isCodeMergeValidated")
String currentStatus = underlyingIssue.getStatus().getName()
log.info("currentStatus: " + currentStatus)

if (isCodeMergeReviewedField != null) {
    if (currentStatus != "Review") {
        isCodeMergeReviewedField.setHidden(true)
    } else { // edit in review state
        String codebase = getFieldByName("CR_codebase")?.getValue()
        log.info("Codebase: " + codebase)
        def pblCurrentField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("CR_pblCurrent")?.first()
        def pblCurrent = underlyingIssue.getCustomFieldValue(pblCurrentField)
        log.info("pblCurrent: " + pblCurrent)
        
        if (pblCurrent == null || codebase == pblCurrent) {
            isCodeMergeReviewedField.setHidden(true)
        } else {
            isCodeMergeReviewedField.setRequired(true)
            if (isCodeMergeReviewedField.value == null) {
                def underlyingField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("CR_isCodeMergeValidated")?.first()
                def fieldConfig = underlyingField.getRelevantConfig(getIssueContext())
                def options = ComponentAccessor.getOptionsManager().getOptions(fieldConfig)
                def option = options.find {it.value == "No"}
                isCodeMergeReviewedField.setFormValue(option.optionId)
            }

        }
    } 
}

// handle scan result override button (hide unless in review state and scan results failed)
def overrideField = getFieldByName("Override Scan Result")
def codeScanResult = getFieldByName("Scan Result")?.getValue()

if (codeScanResult == "PASS") {
    overrideField.setHidden(true)
} else {
    overrideField.setHidden(false)
}
