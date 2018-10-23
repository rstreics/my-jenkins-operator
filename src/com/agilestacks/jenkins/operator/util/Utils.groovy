package com.agilestacks.jenkins.operator.util

class Utils {

    static Map deepMerge(Map... maps) {
        Map result

        if (maps.length == 0) {
            result = [:]
        } else if (maps.length == 1) {
            result = maps[0]
        } else {
            result = [:]
            maps.each { map ->
                map.each { k, v ->
                    result[k] = result[k] instanceof Map ? deepMerge(result[k], v) : v
                }
            }
        }

        result
    }
}
