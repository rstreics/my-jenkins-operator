package com.agilestacks.jenkins.operator.crd

trait Status {

  abstract Map getSpec()

  Code getStatus() {
    String code = spec.status?.code
    return code ? Code.valueOf( code.toUpperCase() ) : Code.UNDEFINED
  }

  void setStatus(Code c) {
      spec.status = spec.status ?: [:]
      spec.status.code = c
  }

  static enum Code {
    PENDING,
    CONVERGED,
    FAILED,
    UNDEFINED
  }
}
