package org.keepcode.service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsmService {

  private static final String HOST = "192.168.2.3";
  private static int PORT = 10992;
  private static final String password = "1234";

  private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public static String reboot() {
    Future<String> future = executor.submit(() -> {
      try (DatagramSocket clientSocket = new DatagramSocket()) {
        String command = String.format("svr_reboot_dev %s %s", getSendid(), password);
        clientSocket.send(getSendingPacket(command));
        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        String answer = new String(receivingPacket.getData()).trim();
        clientSocket.close();
        return getLastWorld(answer);
      } catch (Exception e) {
        return "ERROR";
      }
    });
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static String numberInfo(Integer line) {
    Future<String> future = executor.submit(() -> {
      try (DatagramSocket clientSocket = new DatagramSocket()) {
        String command = String.format("get_gsm_num %s %s", getSendid(), password);
        clientSocket.send(getSendingPacket(command, getPort(line)));
        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        String answer = new String(receivingPacket.getData()).trim();
        clientSocket.close();
        return getLastWorld(answer);
      } catch (Exception e) {
        return "Error";
      }
    });
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static String lineReboot(int line) {
    Future<String> future = executor.submit(() -> {
      try (DatagramSocket clientSocket = new DatagramSocket()) {
        String command = String.format("svr_reboot_module %s %s", getSendid(), password);
        clientSocket.send(getSendingPacket(command, getPort(line)));
        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        String answer = new String(receivingPacket.getData()).trim();
        clientSocket.close();
        return getLastWorld(answer);
      } catch (Exception e) {
        return "ERROR";
      }
    });
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static String sendUssd(Integer line, String ussd) {
    Future<String> future = executor.submit(() -> {
      try (DatagramSocket clientSocket = new DatagramSocket()) {
        String command = String.format("USSD %s %s %s", getSendid(), password, ussd);
        clientSocket.send(getSendingPacket(command, getPort(line)));

        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        clientSocket.close();
        String receivedData = new String(receivingPacket.getData()).trim();
        return getLastWorld(receivedData);
      } catch (Exception e) {
        return "ERROR";
      }
    });
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static String setGsmNum(String num, Integer line){
    Future<String> future = executor.submit(() -> {
      try (DatagramSocket clientSocket = new DatagramSocket()) {
        String command = String.format("set_gsm_num %s %s %s", getSendid(), num, password);
        clientSocket.send(getSendingPacket(command, getPort(line)));

        DatagramPacket receivingPacket = getReceivingPacket();
        clientSocket.receive(receivingPacket);
        String answer = new String(receivingPacket.getData()).trim();
        clientSocket.close();
        return getLastWorld(answer);
      } catch (Exception e) {
        return "ERROR";
      }
    });
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, String> getLinesStatus(Integer port) {
    Map<String, String> lines = new HashMap<>();
    long start = System.currentTimeMillis();
    long end = System.currentTimeMillis();
    try (DatagramSocket clientSocket = new DatagramSocket(port)) {
      while ((end - start) < 35 * 1000) {
        byte[] receivingDataBuffer = new byte[2048];
        DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
        clientSocket.receive(receivingPacket);
        String receivedData = new String(receivingPacket.getData()).trim();
        String goipId = getGoipId(receivedData);
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

  private static int getPort(int port) {
    try {
      return (PORT / 10) * 10 + port;
    } catch (Exception e) {
      return PORT;
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

  private static boolean checkError(String msg, String errorText) {
    return !msg.contains(errorText);
  }

  private static String getSendid() {
    String time = String.valueOf(System.currentTimeMillis());
    return time.substring(time.length() - 7);
  }

  private static String getLastWorld(String text) {
    return text.substring(text.lastIndexOf(" ") + 1);
  }
}
