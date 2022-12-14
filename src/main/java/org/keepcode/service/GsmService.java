package org.keepcode.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.db.AgentDB;
import org.keepcode.domain.DeviceInfo;
import org.keepcode.domain.GsmLine;
import org.keepcode.enums.Country;
import org.keepcode.enums.LineStatus;
import org.keepcode.enums.SimOperator;
import org.keepcode.factory.DatagramSocketFactory;
import org.keepcode.factory.SimUssdFactory;
import org.keepcode.util.HttpUtil;
import org.keepcode.util.PropUtil;
import org.keepcode.validate.Validator;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keepcode.helpstring.CommandStrings.RECEIVE_OK_MSG_ANSWER;
import static org.keepcode.helpstring.CommandStrings.REG_STATUS_MSG_ANSWER;
import static org.keepcode.helpstring.CommandStrings.SEND_DONE;
import static org.keepcode.helpstring.CommandStrings.SEND_GET_GSM_NUM;
import static org.keepcode.helpstring.CommandStrings.SEND_MSG;
import static org.keepcode.helpstring.CommandStrings.SEND_MSG_COMMAND;
import static org.keepcode.helpstring.CommandStrings.SEND_PASSWORD;
import static org.keepcode.helpstring.CommandStrings.SEND_SET_GSM_NUM;
import static org.keepcode.helpstring.CommandStrings.SEND_SVR_REBOOT_DEV;
import static org.keepcode.helpstring.CommandStrings.SEND_SVR_REBOOT_MODULE;
import static org.keepcode.helpstring.CommandStrings.SEND_USSD;
import static org.keepcode.helpstring.CommandStrings.STATE_OK_MSG_ANSWER;
import static org.keepcode.writer.ReceiveWriter.write;

public class GsmService {

  private static final int RECEIVE_PORT = PropUtil.getReceivePort();

  private static final Map<String, Map<String, GsmLine>> hostLineInfo = new HashMap<>();

  private static final Pattern PHONE_NUMBER_FROM_RESPONSE_PATTERN = Pattern.compile("(?<phone>[+\\-)(\\s\\d]{9,})");  // ?? ?????????????????? ?????? ???? ???????? ???????????????? ????????????????, ?????? ?????? ??????????-???? ?????????? ???????? (((((((( ???????? ???? ??????????

  private static final Pattern SEND_ID_PATTERN = Pattern.compile("(?<sendId>-?\\d+)");

  private static final Pattern FIRST_WORD_COMMAND_PATTERN = Pattern.compile("(?<first>^\\w+)");

  private static final Pattern KEEP_ALIVE_PARAM_PATTERN = Pattern.compile(".*id:(?<id>.+);pass:(?<pass>.+);" +
    "num:(?<num>[+()\\-\\s\\d]*);signal.*gsm_status:(?<gsmStatus>\\w*);.*imsi:(?<imsi>\\d+);iccid.*pro:(?<operator>\\w*);idle");

  private static final Pattern RECEIVE_PATTERN = Pattern.compile("id:(?<id>.+);password:(?<password>.+);srcnum.*msg:(?<msg>.+)");

  private static final Pattern ANSWER_AFTER_SEND_ID_PATTERN = Pattern.compile("\\d+\\s?(?<answer>.*)");

  private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR.*\\s+(?<errorMsg>.+)$");

  private static final Pattern IMSI_PATTERN = Pattern.compile("(?<countryCode>\\d{3})(?<operatorCode>\\d{2,3})\\d{10}");

  private static final Pattern DEVICE_INFO_PATTERN = Pattern.compile("<sn>(?<sn>\\w+)</sn>.*" +
    "<version>(?<version>[\\w-.]+)</version>.*<model>(?<model>\\w+)</model>");

  public static final String RECEIVE_CALL_MSG = "\n???????????? ?? ????????????: %s ???? %s ????????\n";
  public static final String RECEIVE_SMS_MSG = "\n?????? '%s' ???????????? ???? %d ????????\n";

  private static final Integer RECEIVED_DATA_BUFFER_SIZE = 8196;

  private static final AtomicInteger UID = new AtomicInteger(1);

  private static final int CORRECT_ANSWER_PASSWORD = 0;

  private static final int UN_CORRECT_ANSWER_PASSWORD = -1;

  @NotNull
  public static String reboot(@NotNull String host, @NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SEND_SVR_REBOOT_DEV, getSendId(), password),
      lineId);
  }

  @NotNull
  public static String numberInfo(@NotNull String host, @NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SEND_GET_GSM_NUM, getSendId(), password),
      lineId);
  }

  @NotNull
  public static String lineReboot(@NotNull String host, @NotNull String lineId, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SEND_SVR_REBOOT_MODULE, getSendId(), password), lineId
      );
  }

  @NotNull
  public static String sendUssd(@NotNull String host, @NotNull String lineId, @NotNull String ussd, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SEND_USSD, getSendId(), password, ussd),
      lineId);
  }

  @NotNull
  public static String setGsmNum(@NotNull String host, @NotNull String lineId, long num, @NotNull String password) {
    return sendCommandAndGetInfoAfterSendId(
      host,
      String.format(SEND_SET_GSM_NUM, getSendId(), num, password),
      lineId);
  }

  private static int getSendId() {
    return UID.incrementAndGet();
  }

  @NotNull
  public static String sendCommandAndGetInfoAfterSendId(@NotNull String host, @NotNull String command, @NotNull String lineId) {
    try {
      return matchPattern(ANSWER_AFTER_SEND_ID_PATTERN, sendCommandAndGetFullAnswer(host, command, lineId), "answer");
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @NotNull
  public static String sendCommandAndGetFullAnswer(@NotNull String host, @NotNull String command, @NotNull String lineId) throws Exception {
    try (DatagramSocket clientSocket = DatagramSocketFactory.getSocket()) {
      return sendCommandAndGetFullAnswer(host, clientSocket, command, hostLineInfo.get(host).get(lineId).getPort());
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
      throw new Exception(String.format("???? ?????????????? ?????????????????? ??????????????:%s ????-???? ???????????? %s", command, e.getMessage()));
    }
  }

  @NotNull
  private static DatagramPacket getSendingPacket(@NotNull String host, @NotNull String command, int port) throws UnknownHostException {
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, InetAddress.getByName(host), port);
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
      return "?????????????????? ???? ???????????? ?????????????????? 3?? ????????????????";
    }
    StringBuilder responseBuilder = new StringBuilder();
    List<String> validPhones = new ArrayList<>();
    for (String phone : phonesFromTextField) {
      if (Validator.isValidNum(phone)) {
        validPhones.add(phone);
      } else {
        responseBuilder.append(String.format("??????????: %s ???? ???????????? ??????????????????\n", phone));
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
        String.format(SEND_MSG, sendId, message.length(), message),
        port);
      if (answer.contains("ERROR")) {
        responseBuilder.append(String.format("?????????? ???????????? ???????????????????? ?? ?????????????? %s\n",
          matchPattern(ERROR_PATTERN, answer, "errorMsg")));
        return responseBuilder.toString();
      }
      answer = sendCommandAndGetFullAnswer(
        host,
        datagramSocket,
        String.format(SEND_PASSWORD, sendId, gsmLine.getPassword()),
        port);
      if (answer.contains("ERROR")) {
        responseBuilder.append("???????????????? ???????????? ???? ??????????, ???????????????????? ?????????? ?????? ??????\n");
        return responseBuilder.toString();
      }
      for (int i = 0; i < validPhones.size(); i++) {
        String sendAnswer = sendCommandAndGetFullAnswer(
          host,
          datagramSocket,
          String.format(SEND_MSG_COMMAND, sendId, i + 1, validPhones.get(i)),
          port);
        if (sendAnswer.contains("WAIT")) {
          DatagramPacket receivingPacket = getReceivingPacket();
          datagramSocket.receive(receivingPacket);
          sendAnswer = getAnswerFromPacket(receivingPacket);
        }
        if (sendAnswer.contains("ERROR")) {
          responseBuilder.append(String.format("???????????????? ?????? ???? ?????????? %s ?????????????????????? ?? ?????????????? %s\n", validPhones.get(i),
            matchPattern(ERROR_PATTERN, sendAnswer, "errorMsg")));
        } else {
          responseBuilder.append(String.format("???? ?????????? %s ?????? ?????????????? ????????????????????\n", validPhones.get(i)));
        }
      }
      sendAnswer(host, String.format(SEND_DONE, sendId), port); // ?????????? ???? ?????????????? ????????, ???? ???? ???? ????????????????????
      return responseBuilder.toString();
    } catch (Exception e) {
      responseBuilder.append(String.format("?????? ?????????????????????? ?? ??????????????: %s\n", e.getMessage()));
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
          String host = receivingPacket.getAddress().getHostAddress();
          if (!isAvailableHost(host)) {
            System.out.printf("?????????????????? ?????????????? %s ?????? ?????????? %s", receivedData, host);
            continue;
          }
          String prefix = matchPattern(FIRST_WORD_COMMAND_PATTERN, receivedData, "first");
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
              System.out.println("???? ?????????????????? ????????????: " + receivedData);
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

  private static boolean isAvailableHost(@NotNull String host) {
    try {
      String info = HttpUtil.getInfoDeviceBody(host);
      Matcher matcher = DEVICE_INFO_PATTERN.matcher(info);
      if (!matcher.find()) {
        return false;
      }
      String serialNumber = matcher.group("sn");
      String version = matcher.group("version");
      String model = matcher.group("model");
      return AgentDB.containsHostAndDeviceInfo(host, new DeviceInfo(serialNumber, version, model));
    } catch (IOException e) {
      System.out.println(e.getMessage());
      return false;
    }
  }

  private static void handleKeepAlive(@NotNull String host, @NotNull String receivedData, int port) throws Exception {
    Matcher matcher = KEEP_ALIVE_PARAM_PATTERN.matcher(receivedData);
    if (!matcher.find()) {
      throw new Exception(String.format("???? ?????????????? ???????????????????? keepAlive: %s, ???????????? ?????? ???? ?????????????? ???????????? ???? ???????????????? %s",
        receivedData, KEEP_ALIVE_PARAM_PATTERN.pattern()));
    }
    String lineId = matcher.group("id");
    String password = matcher.group("pass");
    String num = matcher.group("num");
    String gsmStatus = matcher.group("gsmStatus");
    long imsi = Long.parseLong(matcher.group("imsi"));
    String operator = matcher.group("operator");  // ???????? ?????????? ?????? ???? ??????????????????, ???? ???????? ?????????????? ???????? ?????????? ????????????????, ???? ?????? ?????????????????? ???????? ????
    int answerCode = CORRECT_ANSWER_PASSWORD;
    hostLineInfo.computeIfAbsent(host, k -> new HashMap<>());
    GsmLine currentLine = hostLineInfo.get(host).get(lineId);
    if (currentLine != null && !currentLine.getPassword().equals(password)) {
      answerCode = UN_CORRECT_ANSWER_PASSWORD;
    }
    Long longNum;
    try {
      longNum = Long.parseLong(num);
    } catch (Exception e) {
      longNum = null;
      System.out.println("???? ?????????????? ???????????????? ?????????? ???? " + num);
    }
    GsmLine gsmLine = new GsmLine(port, password, LineStatus.getLineStatus(gsmStatus), imsi, operator, longNum);
    sendAnswer(host, String.format(REG_STATUS_MSG_ANSWER, parseSendId(receivedData), answerCode), port);
    if (num.trim().isEmpty() && gsmLine.getStatus() == LineStatus.LOGIN) {
      setNumber(host, lineId, imsi, password);
    }
  }

  private static void handleReceiveCall(@NotNull String host, @NotNull String receivedData, int port) throws Exception {
    if (receivedData.contains("INCOMING")) {
      write(String.format(RECEIVE_CALL_MSG, matchPattern(PHONE_NUMBER_FROM_RESPONSE_PATTERN, receivedData, "phone"), port));
    }
    sendAnswer(host, String.format(STATE_OK_MSG_ANSWER, parseSendId(receivedData)), port);
  }

  @NotNull
  private static String parseSendId(@NotNull String text) throws Exception {
    return matchPattern(SEND_ID_PATTERN, text, "sendId");
  }

  @NotNull
  public static String matchPattern(@NotNull Pattern pattern, @NotNull String text, @Nullable String group) throws Exception {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return group != null ? matcher.group(group) : matcher.group();
    }
    throw new Exception(String.format("?? ???????????? %s ???????????? ???? ?????????????? ???? ???????????????? %s ?? ?????????????? %s", text, pattern.pattern(), group));
  }

  private static void sendAnswer(@NotNull String host, @NotNull String answer, int port) throws Exception {
    try (DatagramSocket datagramSocket = DatagramSocketFactory.getSocket()) {
      DatagramPacket sendingPacket = getSendingPacket(host, answer, port);
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      throw new Exception(String.format("???? ?????????????? ?????????????????? ??????????: %s ????-???? ????????????: %s", answer, e.getMessage()));
    }
  }

  private static void handleReceiveMsg(@NotNull String host, @NotNull String receivedData, int port) throws Exception {
    Matcher matcher = RECEIVE_PATTERN.matcher(receivedData);
    if (!matcher.find()) {
      return;
    }
    String msg = matcher.group("msg");
    try {
      long phone = clearNum(matchPattern(PHONE_NUMBER_FROM_RESPONSE_PATTERN, msg, "phone"));
      String lineId = matcher.group("id");
      String password = matcher.group("password");
      new Thread(() -> {
        String setGsmNumAnswer = setGsmNum(host, lineId, phone, password);
        if (setGsmNumAnswer.toLowerCase().contains("ok")) {
          System.out.printf("???? ?????????? %s ?????????? ?????????????? ???? %s%n", lineId, phone);
        }
      }).start();
    } catch (Exception e) {
      System.out.printf("?? ?????????????????? %s ?????????? ???? ??????????????????", msg); // ???????? ?????? ???????? ????????????????, ???? ???? ?????? ?????? ????????????????????
    }
    write(String.format(RECEIVE_SMS_MSG, msg, port));
    sendAnswer(host, String.format(RECEIVE_OK_MSG_ANSWER, parseSendId(receivedData)), port);
  }

  @NotNull
  public static Map<String, Map<String, GsmLine>> getHostLineInfo() {
    return new HashMap<>(hostLineInfo);
  }

  public static void clearHostLineInfo() {
    hostLineInfo.clear();
  }

  public static void setNumber(@NotNull String host, @NotNull String lineId, long imsi, @NotNull String password) {
    Matcher matcher = IMSI_PATTERN.matcher(String.valueOf(imsi));
    if (!matcher.find()) {
      System.out.printf("???????????? ???? ?????????????? ?? imsi %s ???? ???????????????? %s%n", imsi, IMSI_PATTERN.pattern());
      return;
    }
    int countryCode = Integer.parseInt(matcher.group("countryCode"));
    int operatorCode = Integer.parseInt(matcher.group("operatorCode"));
    Country country = SimUssdFactory.getCountryByCode(countryCode);
    if (country == null) {
      System.out.println("???? ???????????????????????????? ???????????? ?? ??????????: " + countryCode);
      return;
    }
    SimOperator simOperator = SimOperator.getOperatorByCode(operatorCode);
    boolean containsCountryAndOperatorCode = SimUssdFactory.containsCountryAndOperatorCode(country, simOperator);
    if (!containsCountryAndOperatorCode) {
      System.out.printf("???? ???????????????????????????? ???????????????? ?? ??????????: %s ?? ???????????? ?? ??????????: %s%n", operatorCode, countryCode);
      return;
    }
    String ussdAnswer = sendUssd(host, lineId,
      SimUssdFactory.getAllAvailableCountryOperatorSimUssdCommand().get(country).get(simOperator).getNumInfo(), password);
    try {
      long phone = clearNum(matchPattern(PHONE_NUMBER_FROM_RESPONSE_PATTERN, ussdAnswer, "phone"));
      String setGsmNumAnswer = setGsmNum(host, lineId, phone, password);
      if (setGsmNumAnswer.toLowerCase().contains("ok")) {
        System.out.printf("???? ?????????? %s ?????????? ?????????????? ???? %s", lineId, phone);
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private static long clearNum(@NotNull String phone){
    return Long.parseLong(phone.replaceAll("[+()\\-\\s]", ""));
  }
}
