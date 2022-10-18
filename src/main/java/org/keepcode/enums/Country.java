package org.keepcode.enums;

public enum Country {

  RUSSIA(250);

  private final int code;

  Country(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
