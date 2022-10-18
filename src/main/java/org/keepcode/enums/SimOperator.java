package org.keepcode.enums;

public enum SimOperator {

  MTC(1),
  MEGAFON(2),
  KUBGSM(13),
  TELE2(20),
  BEELINE28(28),
  BEELINE99(99);

  private final int code;

  SimOperator(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
