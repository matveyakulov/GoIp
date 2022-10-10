package org.keepcode.domain;

import org.keepcode.enums.LineStatus;
import org.keepcode.util.PropUtil;

import static org.keepcode.enums.LineStatus.*;

public class GsmLine {

  private final int port;
  private final String password;
  private final LineStatus status;

  public GsmLine(int lineNum, String password, String status) {
    this.port = (PropUtil.getDefaultSendPort() / 10) * 10 + lineNum;
    this.password = password;
    this.status = getLineStatus(status);
  }

  private LineStatus getLineStatus(String status) {
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

  public String getPassword() {
    return password;
  }

  public LineStatus getStatus() {
    return status;
  }
}
