package org.keepcode.service;

import org.keepcode.domain.GsmLine;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.util.PropUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keepcode.factory.InetAddressFactory.getAddress;
import static org.keepcode.util.CommandStrings.*;
import static org.keepcode.util.MessageUtil.*;
import static org.keepcode.writer.FileWriter.write;

public class GsmService {

  private static final int RECEIVE_PORT = PropUtil.getReceivePort();

  private static final Map<Integer, GsmLine> gsmLineMap = new ConcurrentHashMap<>();

  private static final String ERROR_MSG = "ERROR";

  private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\+?\\d+");

  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

  private static final Pattern FIRST_WORD_PATTERN = Pattern.compile("(?<first>^\\w+):");

  private static final Pattern PARAMETERS_PATTERN = Pattern.compile("req:(?<req>\\w*);.*id:(?<id>\\w*);.*" +
    ".*pass:(?<pass>\\w*);.*gsm_status:(?<gsmstatus>\\w*);");

  private static final Pattern MSG_PATTERN = Pattern.compile("msg:(?<msg>\\w+);");
  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  public static String reboot(int line, String password) {
    String command = String.format(SVR_REBOOT_DEV, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  public static String numberInfo(int line, String password) {
    String command = String.format(GET_GSM_NUM, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  public static String lineReboot(int line, String password) {
    String command = String.format(SVR_REBOOT_MODULE, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  public static String sendUssd(int line, String ussd, String password) {
    String command = String.format(USSD, getSendId(), password, ussd);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  public static String setGsmNum(int line, String num, String password) {
    String command = String.format(SET_GSM_NUM, getSendId(), num, password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  private static String sendCommandAndGetAnswer(String command, int port) {
    try (DatagramSocket clientSocket = DatagramSocketFactory.getSocket()) {
      clientSocket.send(getSendingPacket(command, port));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      return getAfterWord(parseSendId(command), getAnswerFromPacket(receivingPacket));
    } catch (Exception e) {
      System.out.println(e.getCause().getMessage());
      return ERROR_MSG;
    }
  }

  private static int getSendId() {
    return (int) (System.currentTimeMillis() % 1e07);
  }

  private static DatagramPacket getSendingPacket(String command, int port) throws UnknownHostException {
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, getAddress(), port);
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        try {
          DatagramPacket receivingPacket = getReceivingPacket();
          clientSocket.receive(receivingPacket);
          String receivedData = getAnswerFromPacket(receivingPacket);
          String lineId = matchPattern(PARAMETERS_PATTERN, receivedData, "id");
          String prefix = matchPattern(FIRST_WORD_PATTERN, receivedData, "first");
          switch (prefix) {
            case "req":
              handleKeepAlive(receivedData);
              break;
            case "RECEIVE":
              handleReceiveMsg(receivedData, lineId);
              break;
            case "STATE":
              handleReceiveCall(receivedData, lineId);
          }
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  private static DatagramPacket getReceivingPacket() {
    byte[] receivingDataBuffer = new byte[RECEIVED_DATA_BUFFER_SIZE];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  private static String getAnswerFromPacket(DatagramPacket receivingPacket) {
    byte[] array = new byte[receivingPacket.getLength()];
    System.arraycopy(receivingPacket.getData(), receivingPacket.getOffset(), array, 0, receivingPacket.getLength());
    return new String(array);
  }

  private static void handleReceiveCall(String receivedData, String lineId) throws Exception {
    int receivePort = getLineNum(lineId);
    write(String.format(RECEIVE_CALL_MSG, getNumber(receivedData), receivePort));
    String str = String.format(STATE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  private static String getNumber(String text) throws Exception {
    return matchPattern(PHONE_NUMBER_PATTERN, text);
  }

  private static void handleReceiveMsg(String receivedData, String lineId) throws Exception {
    int receivePort = getLineNum(lineId);
    String msg = matchPattern(MSG_PATTERN, receivedData, "msg");
    write(String.format(RECEIVE_SMS_MSG, msg, receivePort));
    String str = String.format(RECEIVE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  private static String getAfterWord(String word, String text) {
    return text.substring(text.lastIndexOf(word) + word.length());
  }

  private static void handleKeepAlive(String receivedData) throws Exception {
    String lineId = matchPattern(PARAMETERS_PATTERN, receivedData, "id");
    int lineNum = Integer.parseInt(matchPattern(NUMBER_PATTERN, lineId));
    String password = matchPattern(PARAMETERS_PATTERN, receivedData, "pass");
    int ansStatus = 0;
    if (gsmLineMap.get(lineNum) != null && !gsmLineMap.get(lineNum).getPassword().equals(password)) {
      ansStatus = -1;
    }
    gsmLineMap.put(lineNum, new GsmLine(lineNum, password, matchPattern(PARAMETERS_PATTERN, receivedData, "gsmstatus")));
    String answer = String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus);
    sendAnswer(answer, lineNum);
  }

  private static int getLineNum(String lineId) throws Exception {
    return Integer.parseInt(matchPattern(NUMBER_PATTERN, lineId));
  }

  private static String parseSendId(String text) throws Exception {
    return matchPattern(NUMBER_PATTERN, text);
  }

  private static void sendAnswer(String answer, int port) {
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = getSendingPacket(answer, port);
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      System.out.println(e.getCause().getMessage());
    }
  }

  public static String matchPattern(Pattern pattern, String text) throws Exception {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group();
    } else {
      throw new Exception(String.format("В тексте %s ничего не найдено по паттерну %s", text, pattern.pattern()));
    }
  }

  public static String matchPattern(Pattern pattern, String text, String group) throws Exception {
    try {
      Matcher matcher = pattern.matcher(text);
      matcher.find();
      return matcher.group(group);
    } catch (Exception e) {
      throw new Exception(String.format("В тексте %s ничего не найдено по паттерну %s с группой %s", text, pattern.pattern(), group));
    }
  }

  public static Map<Integer, GsmLine> getGsmLineMap() {
    return gsmLineMap;
  }
}
