package pipelines

import jenkins.model.*
import hudson.model.*
import groovy.json.JsonSlurper
import java.util.logging.Logger
import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.job.*
import com.cloudbees.hudson.plugins.folder.*

final NAME           = '{{spec.name}}' ?: '{{metadata.name}}' ?: null
final URL            = '{{spec.repositoryUrl}}' ?: null
final BRANCH_SPEC    = '{{spec.branchSpec}}' ?: null
final JENKINSFILE    = '{{spec.pipeline}}' ?: null
final CREDENTIALS_ID = '{{spec.credentialsId}}' ?: null
final FOLDER         = '{{spec.folder}}' ?: null
final START_BUILD    = '{{spec.startBuild}}'.toBoolean()
final ORIGIN         = '{{spec.origin}}' ?: 'Agile Stacks Superhub'
final PARAMS_BASE64  = '''{{paramsBase64}}'''.trim()

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
    println "Creating folder: ${parts[-1]}"
    return parent.createProject(com.cloudbees.hudson.plugins.folder.Folder, parts[-1])
}

def allJobs = Jenkins.get().getAllItems(AbstractProject)
def found = allJobs.find { job -> job.name == NAME }
if (!found) {
    def parent = Jenkins.get()
    if ( FOLDER ) {
        parent = createFolder(FOLDER)
    }

    final pipeline = [FOLDER, NAME].findAll {it}.join('/')
    println "Creating pipeline: ${pipeline}"
    final repos = GitSCM.createRepoList(URL, CREDENTIALS_ID)
    final branchSpec = new BranchSpec( BRANCH_SPEC )
    final scm = new GitSCM(repos, [branchSpec], false, [], null, null, [])
    final job = new WorkflowJob(parent, NAME)
    if (PARAMS_BASE64) {
        final params = new JsonSlurper().parse(PARAMS_BASE64.decodeBase64())
        final paramDefs = params.collect {
            def type = it.type.trim() ?: 'string'
            println "Add parameter ${type}:${it.name}"
            if (type == 'string') {
                return new StringParameterDefinition(it.name, it.defaultValue, it.description)
            }
            if (type == 'boolean') {
                return new BooleanParameterDefinition(it.name, it.defaultValue ?: false, it.description)
            }
            throw new RuntimeException("Unsupported parameter type: ${type}")
        }
        job.addProperty( new ParametersDefinitionProperty( paramDefs ) )

    }
    job.definition = new CpsScmFlowDefinition(scm, JENKINSFILE )
    Jenkins.get().reload()
    if (START_BUILD) {
        if (!job.inQueue) {
            final afterSecs = 5
            final cause = new Cause.RemoteCause(ORIGIN, "Started automatically by ${ORIGIN}")
            final action = new CauseAction(cause)
            job.scheduleBuild2(afterSecs, action)
            println "${pipeline}: automated build process"
        }
    } else {
        println "${pipeline}: skipping automated build due to user setting"
    }
} else {
    println '${pipeline} already exists! Moving on'
}

println 'Status: CONVERGED <EOF>'
