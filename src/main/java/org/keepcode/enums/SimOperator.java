package org.keepcode.enums;

public enum SimOperator {

  MTC,
  MEGAFON,
  KUBGSM,
  TELE2,
  BEELINE,
  UNKNOWN;

  public static SimOperator getOperatorByCode(int code){
    switch (code){
      case 1: return MTC;
      case 2: return MEGAFON;
      case 13: return KUBGSM;
      case 20: return TELE2;
      case 28:
      case 99: return BEELINE;
      default: return UNKNOWN;
    }
  }
}
