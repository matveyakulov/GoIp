package org.keepcode.helpstring;

public class CommandStrings {

  public static final String SEND_SVR_REBOOT_DEV = "svr_reboot_dev %d %s";
  public static final String SEND_SVR_REBOOT_MODULE = "svr_reboot_module %d %s";
  public static final String SEND_GET_GSM_NUM = "get_gsm_num %d %s";
  public static final String SEND_SET_GSM_NUM = "set_gsm_num %d %d %s";
  public static final String SEND_USSD = "USSD %d %s %s";
  public static final String SEND_PASSWORD = "PASSWORD %d %s\n";
  public static final String SEND_MSG = "MSG %d %d %s\n";
  public static final String SEND_MSG_COMMAND = "SEND %d %d %s\n";
  public static final String SEND_DONE = "DONE %d\n";
  public static final String REG_STATUS_MSG_ANSWER = "reg:%s;status:%d;";
  public static final String RECEIVE_OK_MSG_ANSWER = "RECEIVE %s OK\n";
  public static final String STATE_OK_MSG_ANSWER = "STATE %s OK\n";
}
