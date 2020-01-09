package rmf.rel.behaviours

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript
import org.apache.log4j.Logger

@BaseScript FieldBehaviours fieldBehaviours

def log = Logger.getLogger("rmf.behaviour.CR_isCodeMerged")

def isCodeMergedField = getFieldByName("CR_isCodeMerged")
def currentStatus = underlyingIssue.getStatus().getName()

if (isCodeMergedField != null) {
    if (currentStatus == "Work In Progress" || currentStatus == "Re-Work_CodeMerge") {
        def crMessageField = getFieldByName("CR_message")
        isCodeMergedField.setReadOnly(false)
        String codebase = getFieldByName("CR_codebase")?.getValue()
        def pblCurrentField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("CR_pblCurrent")?.first()
        def pblCurrent = underlyingIssue.getCustomFieldValue(pblCurrentField)

        if (pblCurrent == null || codebase == pblCurrent) {
            // no need for code merge
            isCodeMergedField.setHidden(true)
        } else {
            // If isCodeMerged is not set by user yet, set isCodeMerged to "No" <- indicates need todo  code merge
            if (isCodeMergedField.value == null) {
                def underlyingField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("CR_isCodeMerged")?.first()
                def fieldConfig = underlyingField.getRelevantConfig(getIssueContext())
                def options = ComponentAccessor.getOptionsManager().getOptions(fieldConfig)
                def option = options.find {it.value == "No"}
                
                isCodeMergedField.setFormValue(option.optionId)
                crMessageField.setFormValue("PBL has shifted, code merge is needed")
            } else if (isCodeMergedField.value == "No") {
                crMessageField.setFormValue("PBL has shifted, code merge is needed")
            } else if (isCodeMergedField.value == "Yes") {
                crMessageField.setFormValue("Need to review code merge to new PBL")
            }
            
            isCodeMergedField.setRequired(true)
        }
    } else if (currentStatus == "Review") {
        isCodeMergedField.setReadOnly(true)
        String codebase = getFieldByName("CR_codebase")?.getValue()
        def pblCurrentField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("CR_pblCurrent")?.first()
        def pblCurrent = underlyingIssue.getCustomFieldValue(pblCurrentField)
        
        if (pblCurrent == null || codebase == pblCurrent) {
            // no need for code merge
            isCodeMergedField.setHidden(true)
        }
    } else {
        isCodeMergedField.setHidden(true)
    }
}