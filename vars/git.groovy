/**
 * Git build routines
 */

import hudson.util.*
import hudson.model.*

/**
 * @return name of the current branch. Also stores in BRANCH_NAME env variable
 */
def branch() {
    Run build = $build()
    final log = Logger.getLogger('git')
    final env = build.getEnvironment(new LogTaskListener(log, Level.INFO))

    if (env.BRANCH_NAME) {
        return env.BRANCH_NAME
    }

    final branch = sh(returnStdout: true,
                      script: 'git rev-parse --abbrev-ref HEAD').trim()

    env.BRANCH_NAME = branch
    return branch
}

/**
 * @return name of the remote (origin by default). Also stores in GIT_REMOTE_{{NAME}} env variable
 */
def remoteUrl(String remote='origin') {
    Run build = $build()
    final log = Logger.getLogger('git')
    final env = build.getEnvironment(new LogTaskListener(log, Level.INFO))
    if (env."GIT_REMOTE_${remote.toUpperCase()}") {
        return env."GIT_REMOTE_${remote.toUpperCase()}"
    }


    final origin = sh(returnStdout: true,
                      script: "git config --get remote.${remote}.url").trim()
    env."GIT_REMOTE_${remote.toUpperCase()}" = origin
    return origin
}
