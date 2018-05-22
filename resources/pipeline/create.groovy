package pipelines

import jenkins.model.*
import hudson.model.*
import java.util.logging.Logger
import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.job.*
import com.cloudbees.hudson.plugins.folder.*

final NAME           = '{{metadata.name}}' ?: null
final URL            = '{{spec.repositoryUrl}}' ?: null
final BRANCH_SPEC    = '{{spec.branchSpec}}' ?: '*/master'
final JENKINSFILE    = '{{spec.pipeline}}' ?: 'Jenkinsfile'
final CREDENTIALS_ID = '{{spec.credentialsId}}' ?: null
final FOLDER         = '{{spec.folder}}' ?: null

def folderName(def folder) {
    if (folder.getParent() instanceof Jenkins) {
        return folder.displayName;
    }
    return folderName(folder.parent) + '/' + folder.displayName
}

def createFolder(String name) {
    def allFolders = Jenkins.get().getAllItems(com.cloudbees.hudson.plugins.folder.Folder)
    def exist = allFolders.find { folderName(it) == name }
    if (exist) {
        return exist
    }

    def parts = name.split('/')
    def parent = Jenkins.get()
    if (parts.size() > 1) {
        def parentFolder = parts[0..-2].join('/')
        parent = createFolder(parentFolder)
    }
    return parent.createProject(com.cloudbees.hudson.plugins.folder.Folder, parts[-1])
}

def allJobs = Jenkins.get().getAllItems(AbstractProject)
def found = allJobs.find { job -> job.name == NAME }
if (!found) {
    def parent = Jenkins.get()
    if ( !FOLDER ) {
        parent = createFolder(FOLDER)
    }

    def repos = GitSCM.createRepoList(URL, CREDENTIALS_ID)
    def branchSpec = new BranchSpec( BRANCH_SPEC )
    def scm = new GitSCM(repos, [branchSpec], false, [], null, null, [])
    def job = new WorkflowJob(parent, NAME )
    job.definition = new CpsScmFlowDefinition(scm, JENKINSFILE )
}
Jenkins.get().reload()

println 'Status: CONVERGED <EOF>'
