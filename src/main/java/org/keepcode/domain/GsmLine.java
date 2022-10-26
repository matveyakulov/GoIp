package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.enums.LineStatus;

public class GsmLine {

  private final int port;
  private final String password;
  private final LineStatus status;

  private final long imsi;

  private final String operator;
  private final Long phoneNum;

  public GsmLine(int port, @NotNull String password, @NotNull LineStatus status, long imsi,
                 @NotNull String operator, @Nullable Long phoneNum) {
    this.port = port;
    this.password = password;
    this.status = status;
    this.imsi = imsi;
    this.operator = operator;
    this.phoneNum = phoneNum;
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

  public long getImsi() {
    return imsi;
  }

  @NotNull
  public String getOperator() {
    return operator;
  }

  @Nullable
  public Long getPhoneNum() {
    return phoneNum;
  }
}
