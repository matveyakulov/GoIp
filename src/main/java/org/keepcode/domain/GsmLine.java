package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;
import org.keepcode.enums.LineStatus;

import static org.keepcode.enums.LineStatus.*;

public class GsmLine {

  private final int port;
  private final String password;
  private final LineStatus status;

  public GsmLine(int port, @NotNull String password, @NotNull String status) {
    this.port = port;
    this.password = password;
    this.status = getLineStatus(status);
  }

  @NotNull
  private LineStatus getLineStatus(@NotNull String status) {
    switch (status) {
      case "LOGIN":
        return ACTIVE;
      case "LOGOUT":
        return UN_ACTIVE;
      default:
        return UNKNOWN;
    }
  }

  public int getPort() {
    return port;
  }

  @NotNull
  public String getPassword() {
    return password;
  }

  @NotNull
  public LineStatus getStatus() {
    return status;
  }
}
