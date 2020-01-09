package rmf.rel.f


import rmf.CustomAppText
import rmf.utils.JiraUtils

def appIssue = JiraUtils.getAppIssue(issue)
return JiraUtils.getCustomFieldString(appIssue, CustomAppText.PBL_CURRENT)
