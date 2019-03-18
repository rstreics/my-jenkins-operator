package githuborg

import jenkins.model.Jenkins
import jenkins.branch.OrganizationFolder
import com.cloudbees.jenkins.GitHubWebHook
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator
import jenkins.branch.BranchIndexingCause

final NAME           = '{{spec.name}}' ?: '{{metadata.name}}' ?: null
final API_URL        = '{{spec.apiUrl}}' ?: null
final ORGANIZATION   = '{{spec.organization}}' ?: NAME
final CREDENTIALS_ID = '{{spec.credentialsId}}' ?: null
final SCAN_CREDS_ID  = '{{spec.scanCredentialsId}}' ?: 'SAME'
final DESCRIPTION    = '{{spec.description}}' ?: "${ORGANIZATION}, Github Organization"
final SCHEDULE_BUILD = '{{spec.scheduleBuild}}' ?: true

def ofs = Jenkins.get().getAllItems(OrganizationFolder)
if ( ofs.find { it.name == ORGANIZATION } ) {
    println "GitHub ${NAME} already registered! Moving on"
    println 'Status: CONVERGED <EOF>'
    return
}

println "Apply Github org folder ${ORGANIZATION}"

def nav = new GitHubSCMNavigator(API_URL, ORGANIZATION, CREDENTIALS_ID, SCAN_CREDS_ID)
def gh = Jenkins.get().createProject(OrganizationFolder, ORGANIZATION)
gh.description = DESCRIPTION
gh.navigators << nav

if (SCHEDULE_BUILD) {
    def cause = new BranchIndexingCause()
    gh.scheduleBuild(cause)
}

GitHubWebHook.get().reRegisterAllHooks()
Jenkins.get().save()

println 'Status: CONVERGED <EOF>'
