#!/usr/bin/env groovy
import com.cloudbees.hudson.plugins.folder.*
import hudson.model.*
import hudson.plugins.git.*
import io.fabric8.kubernetes.client.*
import jenkins.model.*
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.job.*

final DEFAULT_BRANCHSPEC =  "*/master"
final DEFAULT_JENKINSFILE = "Jenkinsfile"

allFolders = Jenkins.instance.getAllItems(Folder)

def folderName(folder) {
  if (folder.getParent() instanceof Jenkins) {
    return folder.displayName;
  }
  return folderName(folder.parent) + '/' + folder.displayName
}

def createFolder(String name) {
  def exist = allFolders.find{folderName(it) == name}
  if (exist) {
    return exist;
  }

  def parts = name.split('/')
  def parent = Jenkins.instance
  if (parts.size() > 1) {
    def parentFolder = parts[0..-2].join('/')
    parent = createFolder(parentFolder)
  }
  return parent.createProject(Folder, parts[-1])
}

def jenk = Jenkins.instance

def allJobs = jenk.getAllItems(AbstractProject)

def client = new DefaultKubernetesClient(new ConfigBuilder().build())
def jobs = []
if (client.masterUrl && client.namespace) {
  jobs = client
    .configMaps()
    .withLabels([
      'project': 'jenkins',
      'qualifier': 'pipeline-scm'
    ])
    .list()
    .items
    .grep { it.data }
    .grep { allJobs.find {job -> job.name == it.metadata.name } == null }
    .collect {
      def repos = GitSCM.createRepoList(it.data.repositoryUrl, it.data.credentialsId)
      def branchSpec = new BranchSpec( it.data.branchSpec ?: DEFAULT_BRANCHSPEC)
      def scm = new GitSCM( repos, [branchSpec], false, [], null, null, [] )
      def flowDefinition = new CpsScmFlowDefinition(scm, it.data.pipeline ?: DEFAULT_JENKINSFILE)

      def parent = jenk
      println it.data.folder
      if (it.data.folder) {
        parent = createFolder( it.data.folder )
      }
      def job = new WorkflowJob(parent, it.metadata.name)
      job.definition = flowDefinition

      return job
    }
}
jenk.reload()

jobs.each { it.scheduleBuild2(0) }
