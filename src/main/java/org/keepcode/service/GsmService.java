package org.keepcode.service;

import org.keepcode.response.Response;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class GsmService {

  private static final String HOST = "192.168.2.3";
  private static int PORT = 10992;
  private static final String password = "1234";

  public static Response reboot() throws IOException {
    String command = String.format("svr_reboot_dev %d %s", System.currentTimeMillis(), password);
    DatagramSocket clientSocket = new DatagramSocket(PORT);
    clientSocket.send(getSendingPacket(command));
    clientSocket.close();
    return new Response(200, "OK");

  }

  public static Response numberInfo(String num) {
    try {
      String command = String.format("get_gsm_num %d %s", System.currentTimeMillis(), password);
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.send(getSendingPacket(command, getPort(num)));

      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String answer = new String(receivingPacket.getData()).trim();
      String phone = answer.substring(answer.lastIndexOf(" ") + 1);
      clientSocket.close();
      return new Response(200, phone);
    } catch (Exception e) {
      return new Response(500, "Internal");
    }
  }

  public static Response lineOff(String line) {
    try {
      String command = String.format("svr_reboot_module %d %s", System.currentTimeMillis(), password);
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.send(getSendingPacket(command, getPort(line)));
      clientSocket.close();
      return new Response(200, "OK");
    } catch (Exception e) {
      return new Response(500, "Internal");
    }
  }

  public static Response sendUssd(String line, String ussd) {
    try {
      String sendid = String.valueOf(System.currentTimeMillis()).substring(7);
      String command = String.format("USSD %s %s %s", sendid, password, ussd);
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.send(getSendingPacket(command, getPort(line)));

      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String receivedData = new String(receivingPacket.getData()).trim();
      String phone = receivedData.substring(receivedData.lastIndexOf(sendid) + sendid.length());
      clientSocket.close();
      return new Response(200, phone);
    } catch (Exception e) {
      return new Response(500, "Internal");
    }
  }

  public static Response setGsmNum(String num, String line) {
    try {
      String sendid = String.valueOf(System.currentTimeMillis()).substring(7);
      String command = String.format("set_gsm_num %s %s %s", sendid, num, password);
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.send(getSendingPacket(command, getPort(line)));

      DatagramPacket receivingPacket = getReceivingPacket();
      clientSocket.receive(receivingPacket);
      String answer = new String(receivingPacket.getData()).trim();
      String phone = answer.substring(answer.lastIndexOf(sendid) + sendid.length());
      clientSocket.close();
      return new Response(200, phone);
    } catch (Exception e) {
      return new Response(500, "Internal");
    }
  }

  public static Map<String, String> getLinesStatus(int port) {
    Map<String, String> lines = new HashMap<>();
    long start = System.currentTimeMillis();
    long end = System.currentTimeMillis();
    try(DatagramSocket clientSocket = new DatagramSocket(port)) {
      while ((end - start) < 35 * 1000) {
        byte[] receivingDataBuffer = new byte[2048];
        DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
        clientSocket.receive(receivingPacket);
        String receivedData = new String(receivingPacket.getData()).trim();
        int indexGoip = receivedData.indexOf("goip0");
        String goipId = receivedData.substring(indexGoip + 5, indexGoip + 6);
        int indexStatus = receivedData.indexOf("gsm_status");
        String status = receivedData.substring(indexStatus + 11, receivedData.indexOf(";", indexStatus));
        lines.put(goipId, status);
        end = System.currentTimeMillis();
      }
      return lines;
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  private static DatagramPacket getSendingPacket(String command, int port) throws UnknownHostException {
    InetAddress IPAddress = InetAddress.getByName(HOST);
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, IPAddress, port);
  }

  private static DatagramPacket getSendingPacket(String command) throws UnknownHostException {
    InetAddress IPAddress = InetAddress.getByName(HOST);
    byte[] commandBytes = command.getBytes();
    return new DatagramPacket(commandBytes, commandBytes.length, IPAddress, PORT);
  }

  private static DatagramPacket getReceivingPacket() {
    byte[] receivingDataBuffer = new byte[2048];
    return new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
  }

  private static int getPort(String port) {
    try {
      return (PORT / 10) * 10 + Integer.parseInt(port);
    } catch (Exception e) {
      return PORT;
    }
  }
}
