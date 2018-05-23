package pipeline

import jenkins.model.*
import hudson.model.*

final NAME = '{{metadata.name}}' ?: null

def allJobs = Jenkins.get().getAllItems(Job)
def found = allJobs.find { job -> job.name == NAME }
if (found) {
    println "Deleting job: ${NAME}"
    found.delete()
    Jenkins.get().reload()
} else {
    throw new IllegalArgumentException("Cannot find job: ${NAME}")
}

println 'Status: CONVERGED <EOF>'
