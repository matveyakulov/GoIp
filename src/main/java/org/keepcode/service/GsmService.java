package org.keepcode.service;

import org.jetbrains.annotations.NotNull;
import org.keepcode.domain.GsmLine;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.factory.InetAddressFactory;
import org.keepcode.util.PropUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static final Pattern ID_PATTERN = Pattern.compile("id:(?<id>\\w+);");

  private static final Pattern PASS_PATTERN = Pattern.compile("pass:(?<pass>.+);");

  private static final Pattern GSM_STATUS_PATTERN = Pattern.compile("gsm_status:(?<gsmstatus>\\w+);");

  private static final Pattern MSG_PATTERN = Pattern.compile("msg:(?<msg>\\w+);");

  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  @NotNull
  public static String reboot(int line, @NotNull String password) {
    String command = String.format(SVR_REBOOT_DEV, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  @NotNull
  public static String numberInfo(int line, @NotNull String password) {
    String command = String.format(GET_GSM_NUM, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  @NotNull
  public static String lineReboot(int line, @NotNull String password) {
    String command = String.format(SVR_REBOOT_MODULE, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  @NotNull
  public static String sendUssd(int line, @NotNull String ussd, @NotNull String password) {
    String command = String.format(USSD, getSendId(), password, ussd);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  @NotNull
  public static String setGsmNum(int line, @NotNull String num, @NotNull String password) {
    String command = String.format(SET_GSM_NUM, getSendId(), num, password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(line).getPort());
  }

  @NotNull
  private static String sendCommandAndGetAnswer(@NotNull String command, int port) {
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

  @NotNull
  private static DatagramPacket getSendingPacket(@NotNull String command, int port) throws UnknownHostException {
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, InetAddressFactory.getAddress(), port);
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        try {
          DatagramPacket receivingPacket = getReceivingPacket();
          clientSocket.receive(receivingPacket);
          String receivedData = getAnswerFromPacket(receivingPacket);
          String lineId = matchPattern(ID_PATTERN, receivedData, "id");
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

  @NotNull
  private static DatagramPacket getReceivingPacket() {
    byte[] receivingDataBuffer = new byte[RECEIVED_DATA_BUFFER_SIZE];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  @NotNull
  private static String getAnswerFromPacket(@NotNull DatagramPacket receivingPacket) {
    byte[] array = new byte[receivingPacket.getLength()];
    System.arraycopy(receivingPacket.getData(), receivingPacket.getOffset(), array, 0, receivingPacket.getLength());
    return new String(array);
  }

  @NotNull
  private static void handleReceiveCall(@NotNull String receivedData, @NotNull String lineId) throws Exception {
    int receivePort = getLineNum(lineId);
    write(String.format(RECEIVE_CALL_MSG, getNumber(receivedData), receivePort));
    String str = String.format(STATE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  @NotNull
  private static String getNumber(@NotNull String text) throws Exception {
    return matchPattern(PHONE_NUMBER_PATTERN, text);
  }

  private static void handleReceiveMsg(@NotNull String receivedData, @NotNull String lineId) throws Exception {
    int receivePort = getLineNum(lineId);
    String msg = matchPattern(MSG_PATTERN, receivedData, "msg");
    write(String.format(RECEIVE_SMS_MSG, msg, receivePort));
    String str = String.format(RECEIVE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  @NotNull
  private static String getAfterWord(String word, @NotNull String text) {
    return text.substring(text.lastIndexOf(word) + word.length());
  }

  private static void handleKeepAlive(@NotNull String receivedData) throws Exception {
    String lineId = matchPattern(ID_PATTERN, receivedData, "id");
    int lineNum = Integer.parseInt(matchPattern(NUMBER_PATTERN, lineId));
    String password = matchPattern(PASS_PATTERN, receivedData, "pass");
    int ansStatus = 0;
    if (gsmLineMap.get(lineNum) != null && !gsmLineMap.get(lineNum).getPassword().equals(password)) {
      ansStatus = -1;
    }
    gsmLineMap.put(lineNum, new GsmLine(lineNum, password, matchPattern(GSM_STATUS_PATTERN, receivedData, "gsmstatus")));
    String answer = String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus);
    sendAnswer(answer, lineNum);
  }

  private static int getLineNum(@NotNull String lineId) throws Exception {
    return Integer.parseInt(matchPattern(NUMBER_PATTERN, lineId));
  }

  @NotNull
  private static String parseSendId(@NotNull String text) throws Exception {
    return matchPattern(NUMBER_PATTERN, text);
  }

  private static void sendAnswer(@NotNull String answer, int port) {
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = getSendingPacket(answer, port);
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      System.out.println(e.getCause().getMessage());
    }
  }

  @NotNull
  public static String matchPattern(@NotNull Pattern pattern, @NotNull String text) throws Exception {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group();
    } else {
      throw new Exception(String.format("В тексте %s ничего не найдено по паттерну %s", text, pattern.pattern()));
    }
  }

  @NotNull
  public static String matchPattern(@NotNull Pattern pattern, @NotNull String text, @NotNull String group) throws Exception {
    try {
      Matcher matcher = pattern.matcher(text);
      matcher.find();
      return matcher.group(group);
    } catch (Exception e) {
      throw new Exception(String.format("В тексте %s ничего не найдено по паттерну %s с группой %s", text, pattern.pattern(), group));
    }
  }

  @NotNull
  public static Map<Integer, GsmLine> getGsmLineMap() {
    return gsmLineMap;
  }
}
