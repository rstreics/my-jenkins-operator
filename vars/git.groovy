/**
 * Git build routines
 */

/**
 * @return name of the remote (origin by default).
 */
def remote() {
    if (scm.userRemoteConfigs) {
        return scm.userRemoteConfigs[0].url
    }
    return sh(script: 'git config --get remote.origin.url', returnStdout: true).trim()
}

def branch() {
    def branch
    if (scm.branches) {
        def t = scm.branches[0].name as String
        branch = t.split('/').last()
    } else {
        branch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
        if (branch == 'HEAD') {
            branch = 'master'
        }
    }
    return branch
}


def getRemote() {
    remote()
}

def getBranch() {
    branch()
}

def commitHash(args=[:]) {
    final arv = [short: true] << args
    if (argv.short) {
        return sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
    }
    return sh(script: 'git rev-parse --long HEAD', returnStdout: true).trim()
}