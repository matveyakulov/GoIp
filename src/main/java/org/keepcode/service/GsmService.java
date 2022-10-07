package org.keepcode.service;

import org.keepcode.domain.GsmLine;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.factory.InetAddressFactory;
import org.keepcode.util.PropUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keepcode.util.CommandStrings.*;
import static org.keepcode.util.MessageUtil.*;
import static org.keepcode.writer.FileWriter.write;

public class GsmService {

  private static final int RECEIVE_PORT;
  private static final int SEND_PORT;

  private static final Map<Integer, GsmLine> gsmLineMap;

  private static final String ERROR_MSG = "ERROR";
  private static final String END_SYMBOL = ";";

  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\+?\\d+");

  private static final Pattern LINE_NUM_PATTERN = Pattern.compile("\\d+");

  private static final Pattern START_END_PATTERN = Pattern.compile(":.+;");

  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  static {
    RECEIVE_PORT = PropUtil.getReceivePort();
    SEND_PORT = PropUtil.getDefaultSendPort();
    gsmLineMap = new ConcurrentHashMap<>();
  }

  public static String reboot(int line, String password) {
    return sendCommandAndGetAnswer(SVR_REBOOT_DEV, line, password);
  }

  public static String numberInfo(int line, String password) {
    return sendCommandAndGetAnswer(GET_GSM_NUM, line, password);
  }

  public static String lineReboot(int line, String password) {
    return sendCommandAndGetAnswer(SVR_REBOOT_MODULE, line, password);
  }

  public static String sendUssd(int line, String ussd, String password) {
    return sendCommandAndGetAnswer(USSD, line, password, ussd);
  }

  public static String setGsmNum(String num, int line, String password) {
    return sendCommandAndGetAnswer(SET_GSM_NUM, line, num, password);
  }

  private static String sendCommandAndGetAnswer(String command, int line, String... params) {
    int sendId = getSendId();
    StringBuilder commandBuilder = new StringBuilder(command);
    commandBuilder.append(" ").append(sendId);
    for (String param : params) {
      commandBuilder.append(" ").append(param);
    }
    try (DatagramSocket clientSocket = DatagramSocketFactory.getSocket()) {
      clientSocket.send(getSendingPacket(commandBuilder.toString(), getPort(line)));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      return getAfterWord(String.valueOf(sendId), new String(receivingPacket.getData()).trim());
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return ERROR_MSG;
    }
  }

  private static DatagramPacket getSendingPacket(String command, int port) throws UnknownHostException {
    InetAddress IPAddress = InetAddressFactory.getAddress();
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, IPAddress, port);
  }

  private static int getSendId() {
    return (int) (System.currentTimeMillis() % 1e07);
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        String receivedData = new String(receivingPacket.getData()).trim();
        String lineId = getStringFrom("id:", receivedData);
        try {
          String prefix = receivedData.substring(0, receivedData.indexOf(":"));
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

  private static void handleReceiveCall(String receivedData, String lineId) throws Exception {
    int receivePort = getLineNum(lineId);
    String phone = getNumber(receivedData);
    write(String.format(RECEIVE_CALL_MSG, phone, receivePort));
    String str = String.format(STATE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  private static String getNumber(String text) throws Exception {
    return match(NUMBER_PATTERN, text);
  }

  private static void handleReceiveMsg(String receivedData, String lineId) throws Exception {
    int receivePort = getLineNum(lineId);
    String msg = getAfterWord("msg:", receivedData);
    write(String.format(RECEIVE_SMS_MSG, msg, receivePort));
    String str = String.format(RECEIVE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  private static String getAfterWord(String word, String text) {
    return text.substring(text.lastIndexOf(word) + word.length());
  }

  private static void handleKeepAlive(String receivedData) throws Exception {
    String lineId = getStringFrom("id:", receivedData);
    int lineNum = getLineNum(lineId);
    String password = getStringFrom("pass:", receivedData);
    String status = getStringFrom("gsm_status:", receivedData);
    int ansStatus = 0;
    if (gsmLineMap.get(lineNum) != null && !gsmLineMap.get(lineNum).getPassword().equals(password)) {
      ansStatus = -1;
    }
    gsmLineMap.put(lineNum, new GsmLine(password, status));
    String answer = String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus);
    sendAnswer(answer, lineNum);
  }

  private static int getLineNum(String lineId) throws Exception {
    return Integer.parseInt(match(LINE_NUM_PATTERN, lineId));
  }

  private static String getStringFrom(String start, String text) {
    int indexStart = text.indexOf(start) + start.length();
    return text.substring(indexStart, text.indexOf(END_SYMBOL, indexStart));
  }

  private static String parseSendId(String text) throws Exception {
    return match(START_END_PATTERN, text);
  }

  public static String match(Pattern pattern, String text) throws Exception {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group();
    } else {
      throw new Exception(String.format("В тексте %s ничего не найдено по паттерну %s", text, pattern.pattern()));
    }
  }

  private static void sendAnswer(String answer, int lineNum) {
    byte[] answerBytes = answer.getBytes();
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = new DatagramPacket(answerBytes, answerBytes.length,
        InetAddressFactory.getAddress(), getPort(lineNum));
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private static int getPort(int port) {
    return (SEND_PORT / 10) * 10 + port;
  }

  public static Map<Integer, GsmLine> getGsmLineMap() {
    return gsmLineMap;
  }
}
