package org.keepcode.service;

import org.keepcode.domain.GsmLine;
import org.keepcode.util.PropUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.keepcode.match.TextMatcher.match;
import static org.keepcode.util.CommandStrings.*;
import static org.keepcode.util.MessageUtil.*;
import static org.keepcode.writer.FileWriter.write;

//todo расположи методы в правильном порядке!
public class GsmService {

  private static final String HOST;
  private static final int RECEIVE_PORT;
  private static final int SEND_PORT;

  //todo naming
  private static final Map<Integer, GsmLine> lines;

  private static final String ERROR_MSG = "ERROR";
  private static final String END_SYMBOL = ";";

  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\+?\\d+");

  private static final Pattern LINE_NUM_PATTERN = Pattern.compile("\\d+");

  //todo ты уверен, что такая регулярка - нормальная?
  private static final Pattern START_END_PATTERN = Pattern.compile(":.+;");

  static {
    HOST = PropUtil.getHost();
    RECEIVE_PORT = PropUtil.getReceivePort();
    SEND_PORT = PropUtil.getDefaultSendPort();
    lines = new ConcurrentHashMap<>();
  }

  public static String reboot(int line, String password) {
    try {
      return sendCommand(SVR_REBOOT_DEV, line, password);
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  public static String numberInfo(int line, String password) {
    try {
      return sendCommand(GET_GSM_NUM, line, password);
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  public static String lineReboot(int line, String password) {
    try {
      return sendCommand(SVR_REBOOT_MODULE, line, password);
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  public static String sendUssd(int line, String ussd, String password) {
    try {
      return sendCommand(USSD, line, password, ussd);
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  public static String setGsmNum(String num, int line, String password) {
    try {
      return sendCommand(SET_GSM_NUM, line, num, password);
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  //todo В принципе мог бы передавать сюда сразу собранную команду
  private static String sendCommand(String command, int line, String... params) {
    String sendId = getSendId();
    //todo Naming
    StringBuilder stringBuilder = new StringBuilder(command);
    stringBuilder.append(" ").append(sendId);
    for (String param : params) {
      stringBuilder.append(" ").append(param);
    }
    try (DatagramSocket clientSocket = new DatagramSocket()) {
      clientSocket.send(getSendingPacket(stringBuilder.toString(), getPort(line)));
      DatagramPacket receivingPacket = getReceivingPacket();
      //todo что происходит, если ответа нет долгое время или вообще нет?
      clientSocket.receive(receivingPacket);
      //todo Зачем тут trim?
      return getAfterWord(sendId, new String(receivingPacket.getData()).trim());
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        String receivedData = new String(receivingPacket.getData()).trim();
        String lineId = getFromTo("id:", receivedData);
        try {
          //todo switch?
          int receivePort = getLineNum(lineId);
          if (receivedData.startsWith("req:")) {
            handleKeepAlive(receivedData);
          } else if (receivedData.startsWith("RECEIVE:")) {
            handleReceiveMsg(receivedData, receivePort);
          } else if (receivedData.startsWith("STATE:")) {
            handleReceiveCall(receivedData, receivePort);
          }
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private static void handleReceiveCall(String receivedData, int receivePort) {
    String phone = getNumber(receivedData);
    write(String.format(RECEIVE_CALL_MSG, phone, receivePort));
    String str = String.format(STATE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  private static void handleReceiveMsg(String receivedData, int receivePort) {
    String msg = getAfterWord("msg:", receivedData);
    write(String.format(RECEIVE_SMS_MSG, msg, receivePort));
    String str = String.format(RECEIVE_OK_MSG, parseSendId(receivedData));
    sendAnswer(str, receivePort);
  }

  private static void handleKeepAlive(String receivedData) {
    String lineId = getFromTo("id:", receivedData);
    int lineNum = getLineNum(lineId);
    String password = getFromTo("pass:", receivedData);
    String status = getFromTo("gsm_status:", receivedData);
    lines.put(lineNum, new GsmLine(password, status));
    int ansStatus = 0;
    if (lines.get(lineNum) != null && !lines.get(lineNum).getPassword().equals(password)) {
      ansStatus = -1;
    }
    String answer = String.format(REG_STATUS_MSG, parseSendId(receivedData), ansStatus);
    sendAnswer(answer, lineNum);
  }

  private static void sendAnswer(String msg, int lineNum) {
    byte[] sendingDataBuffer = msg.getBytes();
    try (DatagramSocket datagramSocket = new DatagramSocket()) {
      DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer, sendingDataBuffer.length,
        InetAddress.getByName(HOST), getPort(lineNum));
      datagramSocket.send(sendingPacket);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static DatagramPacket getSendingPacket(String command, int port) throws UnknownHostException {
    //todo может быть стоит вынести сразу InetAddress
    InetAddress IPAddress = InetAddress.getByName(HOST);
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, IPAddress, port);
  }

  private static DatagramPacket getReceivingPacket() {
    //todo Goip только по UDP общается? Если так, может стоит вынести размер датаграммы в константу?
    byte[] receivingDataBuffer = new byte[8196];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  private static int getPort(int port) {
    return (SEND_PORT / 10) * 10 + port;
  }

  private static int getLineNum(String lineId) {
    try {
      //todo зачем тут обработка, в которой ты просто кидаешь ошибку?
      return Integer.parseInt(match(LINE_NUM_PATTERN, lineId));
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static String getNumber(String text) {
    return match(NUMBER_PATTERN, text);
  }

  //todo тут до сих пор стрингу возвращаешь
  private static String getSendId() {
    return String.valueOf((int) (System.currentTimeMillis() % 1e07));
  }

  private static String getFromTo(String start, String text) {
    try {
      int indexStart = text.indexOf(start) + start.length();
      return text.substring(indexStart, text.indexOf(END_SYMBOL, indexStart));
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  private static String getAfterWord(String word, String text) {
    try {
      //todo такая реализация обработки ответа плохая
      return text.substring(text.lastIndexOf(word) + word.length());
    } catch (Exception e) {
      return ERROR_MSG;
    }
  }

  private static String parseSendId(String text) {
    return match(START_END_PATTERN, text);
  }

  public static Map<Integer, GsmLine> getLines() {
    return lines;
  }
}
