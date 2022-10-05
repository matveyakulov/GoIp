package org.keepcode;

import org.keepcode.util.PropUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Udp {

  private static final String fileName = "goip.txt";
  public static void main(String[] args) throws IOException {
    int port = PropUtil.getReceivePort();
    String host = PropUtil.getHost();
    try {
      DatagramSocket clientSocket = new DatagramSocket(port);
      InetAddress IPAddress = InetAddress.getByName(host);
      while (true) {
        byte[] receivingDataBuffer = new byte[2048];
        DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
        clientSocket.receive(receivingPacket);

        String receivedData = new String(receivingPacket.getData()).trim();
        System.out.println(receivedData);
        if (receivedData.startsWith("RECEIVE:")) {
          BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
          String receivePort = getGoipId(receivedData);
          int indexEnd = receivedData.indexOf(":", receivedData.indexOf("msg:") + 6);
          if (indexEnd == -1) {
            indexEnd = receivedData.length();
          }
          String msg = receivedData.substring(receivedData.indexOf("msg:") + 4, indexEnd);
          writer.append(String.format("\nСмс %s пришло на %s линию\n", msg, receivePort));
          writer.flush();
          writer.close();
          String str = String.format("RECEIVE %s OK\n", getSendid(receivedData));
          byte[] sendingDataBuffer1 = str.getBytes();
          DatagramPacket sendingPacket2 = new DatagramPacket(sendingDataBuffer1, sendingDataBuffer1.length, IPAddress, getPort(receivePort));
          try {
            clientSocket.send(sendingPacket2);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else if (receivedData.startsWith("STATE:") && receivedData.contains("gsm_remain_state:INCOMING")) {
          String phone = getNumber(receivedData);
          String receivePort = getGoipId(receivedData);
          BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
          writer.append(String.format("\nЗвонок с номера: %s на %s линию\n", phone, receivePort));
          writer.flush();
          writer.close();
          String str = String.format("STATE %s OK\n", getSendid(receivedData));
          byte[] sendingDataBuffer1 = str.getBytes();
          DatagramPacket sendingPacket2 = new DatagramPacket(sendingDataBuffer1, sendingDataBuffer1.length, IPAddress, getPort(receivePort));
          try {
            clientSocket.send(sendingPacket2);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }

  private static int getPort(String port) {
    int defaultSendPort = PropUtil.getDefaultSendPort();
    try {
      return (defaultSendPort / 10) * 10 + Integer.parseInt(port);
    } catch (Exception e) {
      return defaultSendPort;
    }
  }

  private static String getGoipId(String receivedData) {
    int indexGoip = receivedData.indexOf("goip0");
    return receivedData.substring(indexGoip + 5, indexGoip + 6);
  }

  private static String getNumber(String text) {
    Matcher matcher = Pattern.compile("\\+?\\d{11}").matcher(text);
    if (matcher.find()) {
      return matcher.group();
    } else {
      return "";
    }
  }

  private static String getSendid(String text) {
    return text.substring(text.indexOf(":") + 1, text.indexOf(";"));
  }
}
