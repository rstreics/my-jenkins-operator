import jenkins.branch.OrganizationFolder
import jenkins.model.CauseOfInterruption
import jenkins.model.Jenkins

final ORG = '{{spec.organization}}' ?: null

def findUnfinishedBuilds(current, results=[]) {
    if (current.hasProperty('builds')) {
        results += current.builds.findAll { it.building }
    }

    if (current.hasProperty('items')) {
        current.items.each {
            findUnfinishedBuilds(it, results)
        }
    }
    return results
}

final cause = new CauseOfInterruption() { String shortDescription = "Interrupted by Operator" }
final orgFolder = Jenkins.get().getAllItems(OrganizationFolder).find { it.name == ORG }
if (!orgFolder) {
    println "Organization folder ${ORG} not found!"
    println 'Status: CONVERGED <EOF>'
    return
}


final unfinishedBuilds = findUnfinishedBuilds(orgFolder)
if (unfinishedBuilds) {
    println "Cancelling all builds for ${ORG}"
    unfinishedBuilds.each {
        it.executor.interrupt(Result.ABORTED, cause)
    }
    print "Waiting 60 sec to settle down "

    // break statement is only allowed inside loops or switches
    for (_ in 0..60) {
        sleep(1000)
        print '.'
        final building = findUnfinishedBuilds(orgFolder).find{ it.building }
        if (!building) {
            break
        }
    }
    println 'Done'
}

orgFolder.delete()
println 'Status: CONVERGED <EOF>'
