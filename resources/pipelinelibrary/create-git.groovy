package pipelinelibrary

import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import jenkins.plugins.git.GitSCMSource
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries

final NAME              = '{{spec.name}}' ?: '{{metadata.name}}' ?: null
final VERSION           = '{{spec.version}}' ?: null
final IMPLICIT          = '{{spec.implicit}}'.toBoolean() ?: false
final INCLUDE_CHANGESET = '{{spec.includeInChangesets}}'.toBoolean() ?: false
final OVERRIDE_VERSION  = '{{spec.allowVersionOverride}}' ?: null
final REPO_URL          = '{{spec.retrievalMethod.git.repositoryUrl}}' ?: null
final CREDENTIALS_ID    = '{{spec.retrievalMethod.git.credentialsId}}' ?: null

final REMOTE            = '{{spec.retrievalMethod.git.remote}}' ?: null
final REF_SPEC          = '{{spec.retrievalMethod.git.refSpec}}'
final INCLUDES          = '{{spec.retrievalMethod.git.includes}}' ?: null
final EXCLUDES          = '{{spec.retrievalMethod.git.excludes}}' ?: null

if (GlobalLibraries.get().libraries.find {it.name == NAME}) {
    println "Status: Pipeline library ${NAME} already exists! Moving on"
    println 'Status: CONVERGED <EOF>'
    return
}

println "Applying pipeline library ${NAME} ${CREDENTIALS_ID}"

def scm = new GitSCMSource( UUID.randomUUID().toString(),
    REPO_URL,
    CREDENTIALS_ID,
    REMOTE,
    REF_SPEC,
    INCLUDES,
    EXCLUDES,
    true )

def lib = new LibraryConfiguration(NAME, new SCMSourceRetriever(scm))
lib.defaultVersion = VERSION
lib.implicit = IMPLICIT
lib.allowVersionOverride = OVERRIDE_VERSION
lib.includeInChangesets = INCLUDE_CHANGESET

GlobalLibraries.get().libraries << lib

println 'Status: CONVERGED <EOF>'
