package pipelinelibrary

import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import hudson.plugins.filesystem_scm.FSSCM

final NAME              = '{{spec.name}}' ?: '{{metadata.name}}' ?: null
final VERSION           = '{{spec.version}}' ?: null
final IMPLICIT          = '{{spec.implicit}}'.toBoolean() ?: false
final OVERRIDE_VERSION  = '{{spec.allowVersionOverride}}'.toBoolean() ?: null
final INCLUDE_CHANGESET = '{{spec.includeInChangesets}}'.toBoolean() ?: null

final PATH              = '{{spec.retrievalMethod?.fileSystem?.path}}' ?: null
final CLEAR_WORKSPACE   = '{{spec.retrievalMethod?.fileSystem?.clearWorkspace}}' ?: null
final COPY_HIDDEN       = '{{spec.retrievalMethod?.fileSystem?.copyHidden}}' ?: null

if (GlobalLibraries.get().libraries.find {it.name == NAME}) {
    println "Status: ERROR - library ${NAME} already exists! Moving on"
    println 'Status: CONVERGED <EOF>'
    return
}

println "Create pipeline library ${NAME}"

def scm = new FSSCM(PATH, CLEAR_WORKSPACE, COPY_HIDDEN, null)
def lib = new LibraryConfiguration(NAME, new SCMRetriever(scm))
lib.defaultVersion = VERSION
lib.implicit = IMPLICIT
lib.allowVersionOverride = OVERRIDE_VERSION
lib.includeInChangesets = INCLUDE_CHANGESET

GlobalLibraries.get().libraries << lib

println 'Status: CONVERGED <EOF>'
