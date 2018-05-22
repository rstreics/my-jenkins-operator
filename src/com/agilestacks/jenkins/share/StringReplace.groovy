#!/usr/bin/env groovy
package com.agilestacks.jenkins.share

import java.util.logging.Logger

class StringReplace implements Serializable {

    static final CURLY = /\$\{\s*([\w\.\-\_]+)\s*\}/
    static final MUSTACHE = /\{\{\s*([\w\.\-\_]+)\s*\}\}/
    final log = Logger.getLogger(this.class.name)

    String render(text, params=[:], pattern=CURLY) {
        def args = flatten(params as Map)
        text.replaceAll(pattern) { m, i ->
            if (m.class in Collection || m.class.array) {
                return args[ m[1] ] ?: m[0]
            }
            return args[i] ?: m
        }
    }

    String eraseMustache(String text) {
        erase(text, MUSTACHE)
    }

    String eraseCurly(String text) {
        erase(text, CURLY)
    }

    String erase(String text, String pattern=CURLY) {
        text.replaceAll(pattern, '')
    }

    String curly(String text,  def params=[:]) {
        render(text, params, CURLY)
    }

    String mustache(String text, def params=[:]) {
        render(text, params, MUSTACHE)
    }

    def flatten(Map map=[:]) {
        return map.collectEntries { k, v ->
            v instanceof Map ?
                flatten(v).collectEntries { k1, v1 ->
                    [ ("${k}.${k1}".toString()) : v1 == null ? '' : v1 ]
                }
                : [ (k): v ]
        }
    }
}
