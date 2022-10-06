package org.keepcode.service;

import org.keepcode.domain.GsmLine;
import org.keepcode.util.PropUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsmService {

  private static final String HOST;
  private static final int RECEIVE_PORT;
  private static final int SEND_PORT;

  private static final Map<Integer, GsmLine> lines;

  private static final String fileName = "goip.txt";

  static {
    HOST = PropUtil.getHost();
    RECEIVE_PORT = PropUtil.getReceivePort();
    SEND_PORT = PropUtil.getDefaultSendPort();
    lines = new HashMap<>();
  }

  public static String reboot(int line, String password) {
    try (DatagramSocket clientSocket = new DatagramSocket()) {
      //Naming sendId
      //Дублирование кода с методом numberInfo, lineReboot, sendUssd, setNum
      String sendid = getSendid();
      String command = String.format("svr_reboot_dev %s %s", sendid, password);
      clientSocket.send(getSendingPacket(command, getPort(line)));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String answer = new String(receivingPacket.getData()).trim();
      clientSocket.close();
      return getAfterWord(sendid, answer);
    } catch (Exception e) {
      return "ERROR";
    }
  }

  public static String numberInfo(int line, String password) {
    try (DatagramSocket clientSocket = new DatagramSocket()) {
      String sendid = getSendid();
      String command = String.format("get_gsm_num %s %s", sendid, password);
      clientSocket.send(getSendingPacket(command, getPort(line)));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String answer = new String(receivingPacket.getData()).trim();
      clientSocket.close();
      return getNumber(answer);
    } catch (Exception e) {
      return "Error";
    }
  }

  public static String lineReboot(int line, String password) {
    try (DatagramSocket clientSocket = new DatagramSocket()) {
      String sendid = getSendid();
      String command = String.format("svr_reboot_module %s %s", sendid, password);
      clientSocket.send(getSendingPacket(command, getPort(line)));
      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String answer = new String(receivingPacket.getData()).trim();
      clientSocket.close();
      return getAfterWord(sendid, answer);
    } catch (Exception e) {
      return "ERROR";
    }
  }

  public static String sendUssd(int line, String ussd, String password) {
    try (DatagramSocket clientSocket = new DatagramSocket()) {
      String sendid = getSendid();
      String command = String.format("USSD %s %s %s", sendid, password, ussd);
      clientSocket.send(getSendingPacket(command, getPort(line)));

      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      clientSocket.close();
      String receivedData = new String(receivingPacket.getData()).trim();
      return getAfterWord(sendid, receivedData);
    } catch (Exception e) {
      return "ERROR";
    }
  }

  public static String setGsmNum(String num, int line, String password) {
    try (DatagramSocket clientSocket = new DatagramSocket()) {
      String command = String.format("set_gsm_num %s %s %s", getSendid(), num, password);
      clientSocket.send(getSendingPacket(command, getPort(line)));

      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String answer = new String(receivingPacket.getData()).trim();
      clientSocket.close();
      return getAfterWord(num, answer);
    } catch (Exception e) {
      return "ERROR";
    }
  }

  public static void listen() {
    try (DatagramSocket clientSocket = new DatagramSocket(RECEIVE_PORT)) {
      while (true) {
        //буфер так и остался постоянным
        byte[] receivingDataBuffer = new byte[2048];
        DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
        clientSocket.receive(receivingPacket);
        String receivedData = new String(receivingPacket.getData()).trim();
        //Можно вынести в константу
        String endSymbol = ";";
        String lineId = getFromTo("id:", endSymbol, receivedData);
        //все еще в строке id линии хранишь + ты передаешь строчку, ее обрезаешь, возвращаешь и тут парсишь в инт ?! :с
        int receivePort = Integer.parseInt(getLineNum(lineId));
        if (receivedData.startsWith("req:")) {
          handleKeepAlive(endSymbol, receivedData);
        } else if (receivedData.startsWith("RECEIVE:")) {
          handleReceiveMsg(receivedData, receivePort);
        } else if (receivedData.startsWith("STATE:")) {
          handleReceiveCall(receivedData, receivePort);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handleReceiveCall(String receivedData, int receivePort) {
    String phone = getNumber(receivedData);
    //форматы можно было в константы вынести
    writeToFile(String.format("\nЗвонок с номера: %s на %s линию\n", phone, receivePort));
    String str = String.format("STATE %s OK\n", parseSendid(receivedData));
    sendAnswer(str, receivePort);
  }//дублирование + отступы
  private static void handleReceiveMsg(String receivedData, int receivePort) {
    String msg = getAfterWord("msg:", receivedData);
    writeToFile(String.format("\nСмс '%s' пришло на %d линию\n", msg, receivePort));
    String str = String.format("RECEIVE %s OK\n", parseSendid(receivedData));
    sendAnswer(str, receivePort);
  }

  private static void handleKeepAlive(String endSymbol, String receivedData) {
    String lineId = getFromTo("id:", endSymbol, receivedData);
    int lineNum = Integer.parseInt(getLineNum(lineId));
    String password = getFromTo("pass:", endSymbol, receivedData);
    String status = getFromTo("gsm_status:", endSymbol, receivedData);
    lines.put(lineNum, new GsmLine(lineId, password, status));
    int ansStatus = 0;
    if (lines.get(lineNum) != null && !lines.get(lineNum).getPassword().equals(password)) {
      ansStatus = -1;
    }
    String answer = String.format("reg:%s;status:%d;", parseSendid(receivedData), ansStatus);
    sendAnswer(answer, lineNum);
  }

  private static void sendAnswer(String msg, int lineNum){
    byte[] sendingDataBuffer = msg.getBytes();
    try (DatagramSocket datagramSocket = new DatagramSocket()) {
      DatagramPacket sendingPacket = new DatagramPacket(sendingDataBuffer, sendingDataBuffer.length,
        InetAddress.getByName(HOST), getPort(lineNum));
      datagramSocket.send(sendingPacket);
    } catch (Exception e){
      //а шо ты тут игноришь + отступ на {
    }
  }

  //должен ли это делать GSM?
  private static void writeToFile(String msg) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));) {
      writer.append(msg);
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //методы в странном порядке расположены
  private static DatagramPacket getSendingPacket(String command, int port) throws UnknownHostException {
    InetAddress IPAddress = InetAddress.getByName(HOST);
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, IPAddress, port);
  }

  private static DatagramPacket getReceivingPacket() {
    //опять таки буфер
    byte[] receivingDataBuffer = new byte[2048];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  public static int getPort(int port) {
    //что конкретно ты тут обрабатываешь
    try {
      return (SEND_PORT / 10) * 10 + port;
    } catch (Exception e) {
      return SEND_PORT;
    }
  }

  public static String getLineNum(String lineId) {
    return lineId.substring(lineId.length() - 2);
  }

  private static String getNumber(String text) {
    //Паттерн можно было в константу
    Matcher matcher = Pattern.compile("\\+?\\d{11}").matcher(text);
    if (matcher.find()) {
      return matcher.group();
    } else {
      return "";
    }
  }

  //Почему public, если ты их только в этом классе используешь?
  //Naming
  public static String getSendid() {
    String time = String.valueOf(System.currentTimeMillis());
    //можно же делить число
    return time.substring(time.length() - 7);
  }

  public static String getFromTo(String start, String end, String text) {
    int indexStart = text.indexOf(start) + start.length();
    return text.substring(indexStart, text.indexOf(end, indexStart));
  }

  private static String getAfterWord(String word, String text) {
    return text.substring(text.lastIndexOf(word) + word.length());
  }

  //лучше было бы сделать регулярку на эту позицию
  private static String parseSendid(String text) {
    return getFromTo(":", ";", text);
  }

  public static Map<Integer, GsmLine> getLines() {
    return lines;
  }
}
