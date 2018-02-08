!/usr/bin/env groovy

import java.nio.file.*

def directory(String directory) {
    return Files.createTempDirectory(directory).toAbsolutePath()
}

def file(Map kv = [:], arg) {
    def argv = [ baseDir: pwd(),
                 extension: '.tmp' ]
    if (arg instanceof Map) {
        argv << arg
    } else {
        argv.directory = arg
    }

    return Files.createTempFile(argv.directory, argv.extension).toAbsolutePath()
}

def call(Map kv = [:], arg) {
    def argv = [ baseDir: pwd(),
                 extension: '.tmp',
                 tempDir:   null ]
    if (arg instanceof Map) {
        argv << arg
    } else {
        argv.directory = arg
    }

    if (argv.tempDir) {
        def dir  = Paths.get(argv.baseDir, argv.tempDir)
        argv.dir = directory( dir )
    }

    return file(argv)
}
