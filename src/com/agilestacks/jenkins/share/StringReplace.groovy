#!/usr/bin/env groovy
package com.agilestacks.jenkins.share

import java.util.logging.Logger

class StringReplace implements Serializable {

    static final CURLY = /\$\{\s*([\w\.\-\_]+)\s*\}/
    static final MUSTACHE = /\{\{\s*([\w\.\-\_]+)\s*\}\}/
    final log = Logger.getLogger(this.class.name)

    String render(text, params=[:], pattern=CURLY) {
        text.replaceAll(pattern) { m, i ->
            if (m.class in Collection || m.class.array) {
                return params[ m[1] ] ?: m[0]
            }
            return params[i] ?: m
        }
    }

    String curly(String text,  def params=[:]) {
        render(text, params, CURLY)
    }

    String mustache(String text, def params=[:]) {
        render(text, params, MUSTACHE)
    }
}
