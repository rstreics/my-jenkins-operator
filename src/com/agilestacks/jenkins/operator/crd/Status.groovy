package com.agilestacks.jenkins.operator.crd

trait Status {

  abstract Map getSpec()

  Code getStatus() {
    String code = this.status?.code
    return code ? Code.valueOf( code.toUpperCase() ) : Code.UNDEFINED
  }

  void setStatus(Code code) {
    spec.status.code = code
  }

  static enum Code {
    PENDING,
    CONVERGED,
    FAILED,
    UNDEFINED
  }
}
