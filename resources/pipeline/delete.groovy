package pipeline

import jenkins.model.*
import hudson.model.*

final NAME = '{{metadata.name}}' ?: null

def allJobs = Jenkins.get().getAllItems(Job)
def found = allJobs.find { job -> job.name == NAME } as Job<Job, Run>
if (found) {
    println "Deleting job: ${NAME}"
    found.
        builds.
        findAll { it.building }.
        each {
            def cause = new CauseOfInterruption() {
                String shortDescription = "Interrupted by Operator"
            }
            it.executor.interrupt(Result.ABORTED, cause)
        }
    print 'Giving up to 60 sec to cool down'
    for (i in 0..60) {
        def building = found.builds.find {it.building}
        if (!building) {
            break
        }
        sleep 1000
        print '.'
    }
    println 'Done'

    found.delete()
    Jenkins.get().reload()
} else {
    throw new IllegalArgumentException("Cannot find job: ${NAME}")
}

println 'Status: CONVERGED <EOF>'
