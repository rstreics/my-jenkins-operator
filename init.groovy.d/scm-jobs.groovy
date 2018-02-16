#!/usr/bin/env groovy

import io.fabric8.kubernetes.client.*
import jenkins.model.*
import hudson.model.*
import java.util.logging.Logger
import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.job.*

final DEFAULT_BRANCHSPEC =  "*/master"
final DEFAULT_JENKINSFILE = "Jenkinsfile"

def jenk = Jenkins.instance

def allJobs = Jenkins.instance.getAllItems(AbstractProject.class)

def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (client.masterUrl && client.namespace) {
  client.
    configMaps().
    withLabels([
      'project': 'jenkins',
      'qualifier': 'scm-config'
    ]).
    list().
    items.
    grep { it.data }.
    grep { allJobs.find {job -> job.name == it.metadata.name } == null }.
    each {
      def repos = GitSCM.createRepoList(it.data.repositoryUrl, it.data.credentialsId)
      def branchSpec = new BranchSpec( it.data.branchSpec ?: DEFAULT_BRANCHSPEC)
      def scm = new GitSCM( repos, [branchSpec], false, [], null, null, [] )

      def flowDefinition = new CpsScmFlowDefinition(scm, it.data.pipeline ?: DEFAULT_JENKINSFILE)
      def job = new WorkflowJob(jenk, it.metadata.name)
      job.definition = flowDefinition
    }
}
jenk.reload()
