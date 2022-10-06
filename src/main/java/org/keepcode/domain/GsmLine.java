package org.keepcode.domain;

public class GsmLine {

  private final String password;
  private final String status;

  public GsmLine(String password, String status) {
    this.password = password;
    this.status = status;
  }

  public String getPassword() {
    return password;
  }

  public String getStatus() {
    return status;
  }
}
