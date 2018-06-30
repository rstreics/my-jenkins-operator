package com.agilestacks.jenkins.share

import java.nio.file.Files
import java.nio.file.Path

class Temp {
    static Path createTempFile(Path path, String ext='tmp', boolean deleteOnExit=true) {
        def directory = path
        def filename = ""
        if ( !path.toFile().isDirectory() ) {
            directory =  path.parent
            filename = path.fileName.toString()
        }

        if (!Files.exists(directory)) {
            Files.createDirectories(directory)
        }

        def temp = Files.createTempFile(directory, filename, ext)
        if (deleteOnExit) {
            temp.toFile().deleteOnExit()
        }

        return temp
    }
}
