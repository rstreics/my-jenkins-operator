package pipelines

import jenkins.model.*
import hudson.model.*

final NAME = '{{metadata.name}}' ?: null

def allJobs = Jenkins.get().getAllItems(AbstractProject)
def found = allJobs.find { job -> job.name == NAME }
if (found) {
    found.delete()
} else {
    throw new IllegalArgumentException("Cannot find job with the name ${NAME}")
}
Jenkins.get().reload()

println 'Status: CONVERGED <EOF>'
