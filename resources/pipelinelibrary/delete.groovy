package pipelinelibrary

import org.jenkinsci.plugins.workflow.libs.GlobalLibraries

final NAME = '{{spec.name}}' ?: '{{metadata.name}}' ?: null

println "Removing pipeline library ${NAME}"

GlobalLibraries.get().libraries.removeAl { it.name == NAME }

println 'Status: CONVERGED <EOF>'
