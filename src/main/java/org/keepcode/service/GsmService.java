package org.keepcode.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.domain.GsmLine;
import org.keepcode.domain.SimUssdCommand;
import org.keepcode.enums.LineStatus;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.factory.InetAddressFactory;
import org.keepcode.factory.SimUssdFactory;
import org.keepcode.util.PropUtil;
import org.keepcode.validate.Validator;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private static final Map<String, Map<String, GsmLine>> hostLineInfo = new HashMap<>();

  private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\+\\d+");

  private static final Pattern PHONE_NUMBER_FROM_USSD_PATTERN = Pattern.compile("(?<phone>\\d{9,})");

  private static final Pattern SEND_ID_PATTERN = Pattern.compile("(?<sendId>-?\\d+)");

  private static final Pattern FIRST_COMMAND_PATTERN = Pattern.compile("(?<first>^\\w+)");

  private static final Pattern KEEP_ALIVE_PARAM_PATTERN = Pattern.compile(".*id:(?<id>.+);pass:(?<pass>.+);" +
    "num:(?<num>[+()\\-\\s\\d]*);signal.*gsm_status:(?<gsmStatus>\\w*);.*imsi:(?<imsi>\\d+);iccid.*pro:(?<operator>\\w*);idle");

  private static final Pattern RECEIVE_PATTERN = Pattern.compile("id:(?<id>.+);password:(?<password>.+);srcnum.*msg:(?<msg>.+)");

  private static final Pattern AFTER_SEND_ID_PATTERN = Pattern.compile("\\d+\\s?(?<answer>.*)");

  private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR.*\\s+(?<errorMsg>.+)$");

  private static final Pattern IMSI_PATTERN = Pattern.compile("(?<countryCode>\\d{3})(?<operatorCode>\\d{2})\\d+");

  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  private static final AtomicInteger UID = new AtomicInteger(1);

  private static final int CORRECT_ANSWER_PASSWORD = 0;

  private static final int UN_CORRECT_ANSWER_PASSWORD = -1;

  private static final Map<Integer, Map<Integer, SimUssdCommand>> countryOperatorSimUssdCommand =
    SimUssdFactory.getAllAvailableCountryOperatorSimUssdCommand();

  @NotNull
  public static String reboot(@NotNull String host, @NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SVR_REBOOT_DEV, getSendId(), password),
      hostLineInfo.get(host).get(lineId).getPort());
  }

  @NotNull
  public static String numberInfo(@NotNull String host, @NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(GET_GSM_NUM, getSendId(), password),
      hostLineInfo.get(host).get(lineId).getPort());
  }

  @NotNull
  public static String lineReboot(@NotNull String host, @NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SVR_REBOOT_MODULE, getSendId(), password),
      hostLineInfo.get(host).get(lineId).getPort());
  }

  @NotNull
  public static String sendUssd(@NotNull String host, @NotNull String lineId, @NotNull String ussd, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(USSD, getSendId(), password, ussd),
      hostLineInfo.get(host).get(lineId).getPort());
  }

  @NotNull
  public static String setGsmNum(@NotNull String host, @NotNull String lineId, @NotNull String num, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SET_GSM_NUM, getSendId(), num, password),
      hostLineInfo.get(host).get(lineId).getPort());
  }

  private static int getSendId() {
    return UID.incrementAndGet();
  }

  @NotNull
  public static String sendCommandAndGetInfoAfterSendId(@NotNull String host, @NotNull String command, int port) {
    try {
      return matchPattern(AFTER_SEND_ID_PATTERN, sendCommandAndGetFullAnswer(host, command, port), "answer");
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @NotNull
  public static String sendCommandAndGetFullAnswer(@NotNull String host, @NotNull String command, int port) throws Exception {
    try (DatagramSocket clientSocket = DatagramSocketFactory.getSocket()) {
      return sendCommandAndGetFullAnswer(host, clientSocket, command, port);
    }
  }

  @NotNull
  private static String sendCommandAndGetFullAnswer(@NotNull String host, @NotNull DatagramSocket clientSocket,
                                                    @NotNull String command, int port) throws Exception {
    try {
      clientSocket.send(getSendingPacket(host, command, port));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      return getAnswerFromPacket(receivingPacket);
    } catch (Exception e) {
      throw new Exception(String.format("Не удалось отправить команду:%s из-за ошибки %s", command, e.getMessage()));
    }
  }

  @NotNull
  private static DatagramPacket getSendingPacket(@NotNull String host, @NotNull String command, int port) throws UnknownHostException {
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, InetAddressFactory.getAddress(host), port);
  }

  @NotNull
  private static DatagramPacket getReceivingPacket() {
    byte[] receivingDataBuffer = new byte[RECEIVED_DATA_BUFFER_SIZE];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  @NotNull
  public static String sendSms(@NotNull String host, @NotNull String lineId, @NotNull String[] phonesFromTextField,
                               @NotNull String message) {
    if (message.getBytes(StandardCharsets.UTF_8).length > 3000) {
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
    GsmLine gsmLine = hostLineInfo.get(host).get(lineId);
    int port = gsmLine.getPort();
    int sendId = getSendId();
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      String answer = sendCommandAndGetFullAnswer(
        host,
        datagramSocket,
        String.format(MSG, sendId, message.length(), message),
        port);
      if (answer.contains("ERROR")) {
        responseBuilder.append(String.format("Старт сессии завершился с ошибкой %s\n",
          matchPattern(ERROR_PATTERN, answer, "errorMsg")));
        return responseBuilder.toString();
      }
      answer = sendCommandAndGetFullAnswer(
        host,
        datagramSocket,
        String.format(PASSWORD, sendId, gsmLine.getPassword()),
        port);
      if (answer.contains("ERROR")) {
        responseBuilder.append("Неверный пароль от линии, попробуйте позже еще раз\n");
        return responseBuilder.toString();
      }
      for (int i = 0; i < validPhones.size(); i++) {
        String sendAnswer = sendCommandAndGetFullAnswer(
          host,
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
      sendCommandAndGetFullAnswer(host, String.format(DONE, sendId), port);
      return responseBuilder.toString();
    } catch (Exception e) {
      responseBuilder.append(String.format("Все закончилось с ошибкой: %s\n", e.getMessage()));
      return responseBuilder.toString();
    }
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        try {
          DatagramPacket receivingPacket = getReceivingPacket();
          clientSocket.receive(receivingPacket);
          String receivedData = getAnswerFromPacket(receivingPacket);
          String prefix = matchPattern(FIRST_COMMAND_PATTERN, receivedData, "first");
          String host = receivingPacket.getAddress().getHostAddress();
          switch (prefix) {
            case "req":
              handleKeepAlive(host, receivedData, receivingPacket.getPort());
              break;
            case "RECEIVE":
              handleReceiveMsg(host, receivedData, receivingPacket.getPort());
              break;
            case "STATE":
              handleReceiveCall(host, receivedData, receivingPacket.getPort());
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

  private static void handleKeepAlive(@NotNull String host, @NotNull String receivedData, int port) throws Exception {
    Matcher matcher = KEEP_ALIVE_PARAM_PATTERN.matcher(receivedData);
    if (matcher.find()) {
      String lineId = matcher.group("id");
      String password = matcher.group("pass");
      String num = matcher.group("num");
      String gsmStatus = matcher.group("gsmStatus");
      String imsi = matcher.group("imsi");
      String operator = matcher.group("operator");  // пока нигде его не использую, по идее графику надо будет допилить, но там непонятно надо ли
      int ansStatus = CORRECT_ANSWER_PASSWORD;
      hostLineInfo.computeIfAbsent(host, k -> new HashMap<>());
      GsmLine currentLine = hostLineInfo.get(host).get(lineId);
      if (currentLine != null && !currentLine.getPassword().equals(password)) {
        ansStatus = UN_CORRECT_ANSWER_PASSWORD;
      }
      GsmLine gsmLine = new GsmLine(port, password, gsmStatus, imsi, operator, num);
      hostLineInfo.get(host).put(lineId, gsmLine);
      sendAnswer(host, String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus), port);
      if (num.trim().equals("") && gsmLine.getStatus() == LineStatus.ACTIVE) {
        new Thread(() -> setNumber(host, lineId, imsi, password)).start();
      }
    } else {
      throw new Exception(String.format("Не удалось обработать keepAlive: %s, потому что не найдено ничего по паттерну %s",
        receivedData, KEEP_ALIVE_PARAM_PATTERN.pattern()));
    }
  }

  private static void handleReceiveCall(@NotNull String host, @NotNull String receivedData, int port) throws Exception {
    if (receivedData.contains("INCOMING")) {
      write(String.format(RECEIVE_CALL_MSG, getNumber(receivedData), port));
    }
    sendAnswer(host, String.format(STATE_OK_MSG, parseSendId(receivedData)), port);
  }

  @NotNull
  private static String getNumber(@NotNull String text) throws Exception {
    return matchPattern(PHONE_NUMBER_PATTERN, text);
  }

  @NotNull
  private static String parseSendId(@NotNull String text) throws Exception {
    return matchPattern(SEND_ID_PATTERN, text, "sendId");
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

  private static void sendAnswer(@NotNull String host, @NotNull String answer, int port) throws Exception {
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = getSendingPacket(host, answer, port);
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      throw new Exception(String.format("Не удалось отправить ответ: %s из-за ошибки: %s", answer, e.getMessage()));
    }
  }

  private static void handleReceiveMsg(@NotNull String host, @NotNull String receivedData, int port) throws Exception {
    Matcher matcher = RECEIVE_PATTERN.matcher(receivedData);
    if (matcher.find()) {
      String msg = matcher.group("msg");
      try {
        msg = msg.replaceAll("[+()\\-\\s*]", "");
        String phone = matchPattern(PHONE_NUMBER_FROM_USSD_PATTERN, msg, "phone");
        String lineId = matcher.group("id");
        String password = matcher.group("password");
        new Thread(() -> {
          String setGsmNumAnswer = setGsmNum(host, lineId, phone, password);
          if(setGsmNumAnswer.toLowerCase().contains("ok")){
            System.out.printf("На линии %s номер изменен на %s%n", lineId, phone);
          }
        }).start();
      } catch (Exception e) {
        System.out.printf("В сообщении %s номер не распознан", msg); // весь лог этим засрется, но хз как еще обработать
      }
      write(String.format(RECEIVE_SMS_MSG, msg, port));
      sendAnswer(host, String.format(RECEIVE_OK_MSG, parseSendId(receivedData)), port);
    }
  }

  @NotNull
  public static Map<String, Map<String, GsmLine>> getHostLineInfo() {
    return new HashMap<>(hostLineInfo);
  }

  public static void clearHostLineInfo() {
    hostLineInfo.clear();
  }

  public static void setNumber(@NotNull String host, @NotNull String lineId, @NotNull String imsi, @NotNull String password) {
    Matcher matcher = IMSI_PATTERN.matcher(imsi);
    if (matcher.find()) {
      int countryCode = Integer.parseInt(matcher.group("countryCode"));
      int operatorCode = Integer.parseInt(matcher.group("operatorCode"));
      if (!countryOperatorSimUssdCommand.containsKey(countryCode)) {
        System.out.println("Не поддерживаемая страна с кодом: " + countryCode);
        return;
      }
      if (!countryOperatorSimUssdCommand.get(countryCode).containsKey(operatorCode)) {
        System.out.printf("Не поддерживаемый оператор с кодом: %s в стране с кодом: %s%n", operatorCode, countryCode);
        return;
      }
      String ussdAnswer = sendUssd(host, lineId,
        countryOperatorSimUssdCommand.get(countryCode).get(operatorCode).getNumInfo(), password);
      try {
        String textAfterSendId = matchPattern(AFTER_SEND_ID_PATTERN, ussdAnswer);
        textAfterSendId = textAfterSendId.replaceAll("[+()\\-\\s]", "");
        String phone = matchPattern(PHONE_NUMBER_FROM_USSD_PATTERN, textAfterSendId, "phone");
        String setGsmNumAnswer = setGsmNum(host, lineId, phone, password);
        if(setGsmNumAnswer.toLowerCase().contains("ok")){
          System.out.printf("На линии %s номер изменен на %s", lineId, phone);
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    } else {
      System.out.printf("Ничего не найдено в imsi %s по паттерну %s%n", imsi, IMSI_PATTERN.pattern());
    }
  }
}
