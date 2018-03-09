#!/usr/bin/env groovy
import jenkins.model.*
import org.jenkinsci.plugins.workflow.libs.*
import hudson.plugins.filesystem_scm.*

def sharedLibsHome = new File(System.getenv('SHARED_LIBS') ?: '/shared-libs')
if (!sharedLibsHome.exists() || !sharedLibsHome.directory) {
  println "${sharedLibsHome.path} shoudl exist and be a directory"
  return
}

def oldLibs = GlobalLibraries.get().libraries
def oldLibNames = oldLibs.collect { it.name }

def newLibs = sharedLibsHome
        .listFiles()
        .grep { !(it.name in oldLibNames) }
        .collect { dir ->
  println "Register shared library: ${dir.name}@master from ${dir.path}"
  def scm = new FSSCM(dir.path, false, false, null)
  def lib = new LibraryConfiguration(dir.name, new SCMRetriever(scm))
  if (dir == 'default') {
    lib.implicit = true
  } else {
    lib.implicit = false
  }
  lib.defaultVersion = 'master'
  lib.allowVersionOverride = true
  lib
}

GlobalLibraries.get().libraries = (oldLibs + newLibs)
