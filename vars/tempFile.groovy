#!/usr/bin/env groovy

import java.nio.file.*

def call(String... args) {
    def list = (args as List)
    return call(path: list[0], extension: list[1])
}

def call(Map arg=[:]) {
    String tempDir = pwd(temp: true)
    def argv = [ path: tempDir,
                 extension: '.tmp',
                 kind: 'absolute',
                 deleteOnExit: false ] << arg.findAll{it.value != null}

    def path = Paths.get(argv.path.toString())
    def directory = path
    def filename = ""
    if ( !path.toFile().isDirectory() ) {
        directory =  path.parent ?: Paths.get( tempDir )
        filename = path.fileName.toString()
    }

    if (!Files.exists(directory)) {
        Files.createDirectories(directory)
    }

    def temp = Files.createTempFile(directory, filename, argv.extension.toString())
    if (argv.deleteOnExit) {
        temp.toFile().deleteOnExit()
    }

    if ('absolute' == argv.kind) {
        temp = temp.toAbsolutePath()
    } else if ('real' == argv.kind) {
        temp = temp.toRealPath()
    }

    return temp.toString()
}
