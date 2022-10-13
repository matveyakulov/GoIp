package org.keepcode.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.domain.GsmLine;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.factory.InetAddressFactory;
import org.keepcode.util.PropUtil;
import org.keepcode.validate.Validator;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keepcode.util.CommandStrings.DONE;
import static org.keepcode.util.CommandStrings.GET_GSM_NUM;
import static org.keepcode.util.CommandStrings.MSG;
import static org.keepcode.util.CommandStrings.PASSWORD;
import static org.keepcode.util.CommandStrings.RECEIVE_OK_MSG;
import static org.keepcode.util.CommandStrings.REG_STATUS_MSG;
import static org.keepcode.util.CommandStrings.SEND;
import static org.keepcode.util.CommandStrings.SET_GSM_NUM;
import static org.keepcode.util.CommandStrings.STATE_OK_MSG;
import static org.keepcode.util.CommandStrings.SVR_REBOOT_DEV;
import static org.keepcode.util.CommandStrings.SVR_REBOOT_MODULE;
import static org.keepcode.util.CommandStrings.USSD;
import static org.keepcode.util.MessageStrings.RECEIVE_CALL_MSG;
import static org.keepcode.util.MessageStrings.RECEIVE_SMS_MSG;
import static org.keepcode.writer.ReceiveWriter.write;

public class GsmService {

  private static final int RECEIVE_PORT = PropUtil.getReceivePort();

  private static final Map<String, GsmLine> gsmLineMap = new ConcurrentHashMap<>();

  private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\+\\d+");

  private static final Pattern SEND_ID_PATTERN = Pattern.compile("(?<sendId>-?\\d+)");

  private static final Pattern FIRST_COMMAND_PATTERN = Pattern.compile("(?<first>^\\w+)");

  private static final Pattern KEEP_ALIVE_PARAM_PATTERN = Pattern.compile(".*id:(?<id>.+);.*pass:(?<pass>.+);num.*" +
    "gsm_status:(?<gsmStatus>\\w*);");

  private static final Pattern MSG_PATTERN = Pattern.compile("msg:(?<msg>.+)");

  private static final Pattern AFTER_SEND_ID_PATTERN = Pattern.compile("\\d+\\s?(?<answer>.*)");

  private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR.*\\s+(?<errorMsg>.+)$");

  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  private static final AtomicInteger UID = new AtomicInteger(1);

  private static final int CORRECT_ANSWER_PASSWORD = 0;

  private static final int UN_CORRECT_ANSWER_PASSWORD = -1;

  @NotNull
  public static String reboot(@NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      String.format(SVR_REBOOT_DEV, getSendId(), password),
      gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String numberInfo(@NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(String.format(GET_GSM_NUM, getSendId(), password),
      gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String lineReboot(@NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(String.format(SVR_REBOOT_MODULE, getSendId(), password),
      gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String sendUssd(@NotNull String lineId, @NotNull String ussd, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(String.format(USSD, getSendId(), password, ussd),
      gsmLineMap.get(lineId).getPort());
  }

  @NotNull
  public static String setGsmNum(@NotNull String lineId, @NotNull String num, @NotNull String password) {
    String command = String.format(SET_GSM_NUM, getSendId(), num, password);
    return sendCommandAndGetInfoAfterSendId(command, gsmLineMap.get(lineId).getPort());
  }

  private static int getSendId() {
    return UID.incrementAndGet();
  }

  @NotNull
  public static String sendCommandAndGetInfoAfterSendId(@NotNull String command, int port) {
    try {
      return matchPattern(AFTER_SEND_ID_PATTERN, sendCommandAndGetFullAnswer(command, port), "answer");
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @NotNull
  public static String sendCommandAndGetFullAnswer(@NotNull String command, int port) throws Exception {
    try (DatagramSocket clientSocket = DatagramSocketFactory.getSocket()) {
      return sendCommandAndGetFullAnswer(clientSocket, command, port);
    }
  }

  @NotNull
  public static String sendSms(@NotNull String lineId, @NotNull String[] phonesFromTextField, @NotNull String message) {
    if (message.getBytes().length > 3000) {
      return "Сообщение не должно превышать 3к символов";
    }
    StringBuilder responseBuilder = new StringBuilder();
    List<String> validPhones = new ArrayList<>();
    for (String phone : phonesFromTextField) {
      if (Validator.isValidNum(phone)) {
        validPhones.add(phone);
      } else {
        responseBuilder.append(String.format("Номер: %s не прошел валидацию\n", phone));
      }
    }
    if (validPhones.isEmpty()) {
      return responseBuilder.toString();
    }
    int port = gsmLineMap.get(lineId).getPort();
    int sendId = getSendId();
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      String answer = sendCommandAndGetFullAnswer(
        datagramSocket,
        String.format(MSG, sendId, message.length(), message),
        port);
      if (answer.contains("ERROR")) {
        responseBuilder.append(String.format("Старт сессии завершился с ошибкой %s\n",
          matchPattern(ERROR_PATTERN, answer, "errorMsg")));
        return responseBuilder.toString();
      }
      answer = sendCommandAndGetFullAnswer(
        datagramSocket,
        String.format(PASSWORD, sendId, gsmLineMap.get(lineId).getPassword()),
        port);
      if (answer.contains("ERROR")) {
        responseBuilder.append("Неверный пароль от линии, попробуйте позже еще раз\n");
        return responseBuilder.toString();
      }
      for (int i = 0; i < validPhones.size(); i++) {
        String sendAnswer = sendCommandAndGetFullAnswer(
          datagramSocket,
          String.format(SEND, sendId, i + 1, validPhones.get(i)),
          port);
        if (sendAnswer.contains("WAIT")) {
          DatagramPacket receivingPacket = getReceivingPacket();
          datagramSocket.receive(receivingPacket);
          sendAnswer = getAnswerFromPacket(receivingPacket);
        }
        if (sendAnswer.contains("ERROR")) {
          responseBuilder.append(String.format("Отправка смс на номер %s завершилась с ошибкой %s\n", validPhones.get(i),
            matchPattern(ERROR_PATTERN, sendAnswer, "errorMsg")));
        } else {
          responseBuilder.append(String.format("На номер %s смс успешно отправлено\n", validPhones.get(i)));
        }
      }
      sendCommandAndGetFullAnswer(String.format(DONE, sendId), port);
      return responseBuilder.toString();
    } catch (Exception e) {
      responseBuilder.append(String.format("Все закончилось с ошибкой: %s\n", e.getMessage()));
      return responseBuilder.toString();
    }
  }

  @NotNull
  private static String sendCommandAndGetFullAnswer(@NotNull DatagramSocket clientSocket, @NotNull String command, int port) throws Exception {
    try {
      clientSocket.send(getSendingPacket(command, port));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      return getAnswerFromPacket(receivingPacket);
    } catch (Exception e) {
      throw new Exception(String.format("Не удалось отправить команду:%s из-за ошибки %s", command, e.getMessage()));
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
          String prefix = matchPattern(FIRST_COMMAND_PATTERN, receivedData, "first");
          switch (prefix) {
            case "req":
              handleKeepAlive(receivedData, receivingPacket.getPort());
              break;
            case "RECEIVE":
              handleReceiveMsg(receivedData, receivingPacket.getPort());
              break;
            case "STATE":
              handleReceiveCall(receivedData, receivingPacket.getPort());
              break;
            default:
              System.out.println("Не обработан запрос: " + receivedData);
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
    return new String(receivingPacket.getData()).trim();
  }

  private static void handleReceiveCall(@NotNull String receivedData, int receivePort) throws Exception {
    if (receivedData.contains("INCOMING")) {
      write(String.format(RECEIVE_CALL_MSG, getNumber(receivedData), receivePort));
    }
    sendAnswer(String.format(STATE_OK_MSG, parseSendId(receivedData)), receivePort);
  }

  @NotNull
  private static String parseSendId(@NotNull String text) throws Exception {
    return matchPattern(SEND_ID_PATTERN, text, "sendId");
  }

  @NotNull
  private static String getNumber(@NotNull String text) throws Exception {
    return matchPattern(PHONE_NUMBER_PATTERN, text);
  }

  private static void sendAnswer(@NotNull String answer, int port) throws Exception {
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = getSendingPacket(answer, port);
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      throw new Exception(String.format("Не удалось отправить ответ: %s из-за ошибки: %s", answer, e.getMessage()));
    }
  }

  private static void handleReceiveMsg(@NotNull String receivedData, int receivePort) throws Exception {
    String msg = matchPattern(MSG_PATTERN, receivedData, "msg");
    write(String.format(RECEIVE_SMS_MSG, msg, receivePort));
    sendAnswer(String.format(RECEIVE_OK_MSG, parseSendId(receivedData)), receivePort);
  }

  private static void handleKeepAlive(@NotNull String receivedData, int port) throws Exception {
    Matcher matcher = KEEP_ALIVE_PARAM_PATTERN.matcher(receivedData);
    if (matcher.find()) {
      String lineId = matcher.group("id");
      String password = matcher.group("pass");
      String gsmStatus = matcher.group("gsmStatus");
      int ansStatus = CORRECT_ANSWER_PASSWORD;
      GsmLine currentLine = gsmLineMap.get(lineId);
      if (currentLine != null && !currentLine.getPassword().equals(password)) {
        ansStatus = UN_CORRECT_ANSWER_PASSWORD;
      }
      gsmLineMap.put(lineId, new GsmLine(port, password, gsmStatus));
      sendAnswer(String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus), port);
    } else {
      throw new Exception(String.format("Не удалось обработать keepAlive: %s, потому что не найдено ничего по паттерну %s",
        receivedData, KEEP_ALIVE_PARAM_PATTERN.pattern()));
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
