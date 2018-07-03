package com.agilestacks.jenkins.share

import java.nio.file.Files
import java.nio.file.Paths

@Deprecated
class TempFile {
    static String create(String basedir='.', String ext='tmp', boolean deleteOnExit=true) {
        def path = Paths.get(basedir)
        def directory = path
        def filename = ""
        if ( !path.toFile().directory ) {
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

        return temp.toString()
    }
}
