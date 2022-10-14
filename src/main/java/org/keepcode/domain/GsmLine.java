package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;
import org.keepcode.enums.LineStatus;

import static org.keepcode.enums.LineStatus.ACTIVE;
import static org.keepcode.enums.LineStatus.UNKNOWN;
import static org.keepcode.enums.LineStatus.UN_ACTIVE;

public class GsmLine {

  private final int port;
  private final String password;
  private final LineStatus status;

  private final String imsi;

  private final String operator;
  private final String num;

  public GsmLine(int port, @NotNull String password, @NotNull String status, @NotNull String imsi,
                 @NotNull String operator, @NotNull String num) {
    this.port = port;
    this.password = password;
    this.status = getLineStatus(status);
    this.imsi = imsi;
    this.operator = operator;
    this.num = num;
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

  @NotNull
  public String getImsi() {
    return imsi;
  }

  @NotNull
  public String getOperator() {
    return operator;
  }

  @NotNull
  public String getNum() {
    return num;
  }
}
