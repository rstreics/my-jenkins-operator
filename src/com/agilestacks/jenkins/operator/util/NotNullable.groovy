package com.agilestacks.jenkins.operator.util

trait NotNullable {

    def wrapper = new NotNullableWrapper()

    Object getProperty(String propertyName) {
        wrapper.getProperty(propertyName)
    }

    void setProperty(String propertyName, Object newValue) {
        wrapper.setProperty(propertyName, newValue)
    }

    private static class NotNullableWrapper {
        def properties = new Properties()

        Object getProperty(String propertyName) {
            def prop = properties.get(propertyName)
            if (!prop) {
                prop = new NotNullableWrapper()
                this.setProperty(propertyName, prop)
            }
            return prop
        }

        void setProperty(String propertyName, Object newValue) {
            properties.put(propertyName, newValue)
        }
    }
}
