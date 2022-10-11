package org.keepcode.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.domain.GsmLine;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.factory.InetAddressFactory;
import org.keepcode.util.PropUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keepcode.util.CommandStrings.GET_GSM_NUM;
import static org.keepcode.util.CommandStrings.SET_GSM_NUM;
import static org.keepcode.util.CommandStrings.SVR_REBOOT_DEV;
import static org.keepcode.util.CommandStrings.SVR_REBOOT_MODULE;
import static org.keepcode.util.CommandStrings.USSD;
import static org.keepcode.util.MessageUtil.RECEIVE_CALL_MSG;
import static org.keepcode.util.MessageUtil.RECEIVE_OK_MSG;
import static org.keepcode.util.MessageUtil.RECEIVE_SMS_MSG;
import static org.keepcode.util.MessageUtil.REG_STATUS_MSG;
import static org.keepcode.util.MessageUtil.STATE_OK_MSG;
import static org.keepcode.writer.FileWriter.write;

public class GsmService {

  private static final int RECEIVE_PORT = PropUtil.getReceivePort();

  private static final Map<String, GsmLine> gsmLineMap = new ConcurrentHashMap<>();

  private static final String ERROR_MSG = "ERROR";

  private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\+?\\d+");

  private static final Pattern SEND_ID_PATTERN = Pattern.compile("(?<sendId>\\d+)");

  private static final Pattern FIRST_WORD_PATTERN = Pattern.compile("(?<first>^\\w+):");

  private static final Pattern KEEP_ALIVE_PARAM_PATTERN = Pattern.compile("id:(?<id>\\w+);.*pass:(?<pass>.+);.*" +
    "gsm_status:(?<gsmStatus>\\w*);");

  private static final Pattern MSG_PATTERN = Pattern.compile("msg:(?<msg>\\w+);");

  private static final Pattern AFTER_SEND_ID_PATTERN = Pattern.compile("\\d+ (?<answer>.+)");

  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  @NotNull
  public static String reboot(@NotNull String lineId, @NotNull String password) {
    String command = String.format(SVR_REBOOT_DEV, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String numberInfo(@NotNull String lineId, @NotNull String password) {
    String command = String.format(GET_GSM_NUM, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String lineReboot(@NotNull String lineId, @NotNull String password) {
    String command = String.format(SVR_REBOOT_MODULE, getSendId(), password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String sendUssd(@NotNull String lineId, @NotNull String ussd, @NotNull String password) {
    String command = String.format(USSD, getSendId(), password, ussd);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String setGsmNum(@NotNull String lineId, @NotNull String num, @NotNull String password) {
    String command = String.format(SET_GSM_NUM, getSendId(), num, password);
    return sendCommandAndGetAnswer(command, gsmLineMap.get(lineId).getPort());
  }

  private static int getSendId() {
    return (int) (System.currentTimeMillis() % 1e07);
  }

  @NotNull
  private static String sendCommandAndGetAnswer(@NotNull String command, int port) {
    try (DatagramSocket clientSocket = DatagramSocketFactory.getSocket()) {
      clientSocket.send(getSendingPacket(command, port));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      return matchPattern(AFTER_SEND_ID_PATTERN, getAnswerFromPacket(receivingPacket), "answer");
    } catch (Exception e) {
      System.out.println("Не удалось отправить команду: " + command);
      return ERROR_MSG;
    }
  }

  @NotNull
  private static DatagramPacket getSendingPacket(@NotNull String command, int port) throws UnknownHostException {
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, InetAddressFactory.getAddress(), port);
  }

  @NotNull
  private static DatagramPacket getReceivingPacket() {
    byte[] receivingDataBuffer = new byte[RECEIVED_DATA_BUFFER_SIZE];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        try {
          DatagramPacket receivingPacket = getReceivingPacket();
          clientSocket.receive(receivingPacket);
          String receivedData = getAnswerFromPacket(receivingPacket);
          String prefix = matchPattern(FIRST_WORD_PATTERN, receivedData, "first");
          switch (prefix) {
            case "req":
              handleKeepAlive(receivedData, receivingPacket.getPort());
              break;
            case "RECEIVE":
              handleReceiveMsg(receivedData, receivingPacket.getPort());
              break;
            case "STATE":
              handleReceiveCall(receivedData, receivingPacket.getPort());
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
  private static String getAnswerFromPacket(@NotNull DatagramPacket receivingPacket) {
    byte[] answerBytes = new byte[receivingPacket.getLength()];
    System.arraycopy(receivingPacket.getData(), receivingPacket.getOffset(), answerBytes, 0, receivingPacket.getLength());
    return new String(answerBytes);
  }

  private static void handleReceiveCall(@NotNull String receivedData, int receivePort) throws Exception {
    write(String.format(RECEIVE_CALL_MSG, getNumber(receivedData), receivePort));
    String answer = String.format(STATE_OK_MSG, parseSendId(receivedData));
    sendAnswer(answer, receivePort);
  }

  @NotNull
  private static String parseSendId(@NotNull String text) throws Exception {
    return matchPattern(SEND_ID_PATTERN, text, "sendId");
  }

  @NotNull
  private static String getNumber(@NotNull String text) throws Exception {
    return matchPattern(PHONE_NUMBER_PATTERN, text);
  }

  private static void sendAnswer(@NotNull String answer, int port) {
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = getSendingPacket(answer, port);
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      System.out.println("Не удалось отправить ответ: " + answer);
    }
  }

  private static void handleReceiveMsg(@NotNull String receivedData, int receivePort) throws Exception {
    String msg = matchPattern(MSG_PATTERN, receivedData, "msg");
    write(String.format(RECEIVE_SMS_MSG, msg, receivePort));
    String answer = String.format(RECEIVE_OK_MSG, parseSendId(receivedData));
    sendAnswer(answer, receivePort);
  }

  private static void handleKeepAlive(@NotNull String receivedData, int port) throws Exception {
    Matcher matcher = KEEP_ALIVE_PARAM_PATTERN.matcher(receivedData);
    if (matcher.find()) {
      String lineId = matcher.group("id");
      String password = matcher.group("pass");
      String gsmStatus = matcher.group("gsmStatus");
      int ansStatus = 0;
      if (gsmLineMap.get(lineId) != null && !gsmLineMap.get(lineId).getPassword().equals(password)) {
        ansStatus = -1;
      }
      gsmLineMap.put(lineId, new GsmLine(port, password, gsmStatus));
      String answer = String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus);
      sendAnswer(answer, port);
    } else {
      throw new Exception(String.format("Не удалось обработать keepAlive: %s", receivedData));
    }
  }

  @NotNull
  public static String matchPattern(@NotNull Pattern pattern, @NotNull String text) throws Exception {
    return matchPattern(pattern, text, null);
  }

  @NotNull
  public static String matchPattern(@NotNull Pattern pattern, @NotNull String text, @Nullable String group) throws Exception {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return group != null ? matcher.group(group) : matcher.group();
    }
    throw new Exception(String.format("В тексте %s ничего не найдено по паттерну %s с группой %s", text, pattern.pattern(), group));
  }

  @NotNull
  public static Map<String, GsmLine> getGsmLineMap() {
    Map<String, GsmLine> gsmLineTmp = new HashMap<>(gsmLineMap);
    gsmLineMap.clear();
    return gsmLineTmp;
  }
}
