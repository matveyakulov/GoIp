package org.keepcode.domain;

import java.util.Objects;

public class GsmLine {

  //final!
  //id в String?
  private String id;
  private String password;
  //статус можно перечислением
  private String status;

  public GsmLine(String id, String password, String status) {
    this.id = id;
    this.password = password;
    this.status = status;
  }

  public String getId() {
    return id;
  }

  public String getPassword() {
    return password;
  }

  public String getStatus() {
    return status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GsmLine gsmLine = (GsmLine) o;
    return id.equals(gsmLine.id) && password.equals(gsmLine.password) && status.equals(gsmLine.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, password, status);
  }
}
