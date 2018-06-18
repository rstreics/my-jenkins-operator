package githuborg

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import java.util.logging.Logger
import org.jenkinsci.plugins.github.*
import org.jenkinsci.plugins.github.config.*
import jenkins.branch.*
import org.jenkinsci.plugins.github_branch_source.*

def log = Logger.getLogger(this.class.name)

final NAME           = '{{metadata.name}}' ?: null
final API_URL        = '{{spec.apiUrl}}' ?: null
final ORGANIZATION   = '{{spec.organization}}' ?: NAME
final CREDENTIALS_ID = '{{spec.credentialsId}}' ?: null
final SCAN_CREDS_ID  = '{{spec.scanCredentialsId}}' ?: 'SAME'
final DESCRIPTION    = '{{spec.description}}' ?: "${ORGANIZATION}, Github Organization"
final SCHEDULE_BUILD = '{{spec.scheduleBuild}}' ?: true

def ofs = Jenkins.get().getAllItems(OrganizationFolder)
def found =  ofs.find { it.name == ORGANIZATION }
if (found) {
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
}

Jenkins.get().save()

println 'Status: CONVERGED <EOF>'
