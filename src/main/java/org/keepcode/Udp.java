package org.keepcode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Udp {

    private static int PORT = 10992;

    public static void main(String[] args) throws IOException {
        int port = 7777;
        String host = "192.168.2.3";
        try {
            DatagramSocket clientSocket = new DatagramSocket(port);
            InetAddress IPAddress = InetAddress.getByName(host);
            while (true) {
                byte[] receivingDataBuffer = new byte[2048];
                DatagramPacket receivingPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                clientSocket.receive(receivingPacket);

                String receivedData = new String(receivingPacket.getData()).trim();
                System.out.println(receivedData);
                if (receivedData.startsWith("req:")) {
                    int indexGoip = receivedData.indexOf("goip0");
                    String receivePort = receivedData.substring(indexGoip + 5, indexGoip + 6);
                    String str = "reg:" + receivedData.substring(receivedData.indexOf(":") + 1, receivedData.indexOf(";")) + ";status:0;";
                    System.out.println(str);
                    byte[] sendingDataBuffer1 = str.getBytes();
                    DatagramPacket sendingPacket2 = new DatagramPacket(sendingDataBuffer1, sendingDataBuffer1.length, IPAddress, getPort(receivePort));
                    try {
                        clientSocket.send(sendingPacket2);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (receivedData.startsWith("RECEIVE:")) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter("goip.txt", true));
                    int indexGoip = receivedData.indexOf("goip0");
                    String receivePort = receivedData.substring(indexGoip + 5, indexGoip + 6);
                    int indexEnd = receivedData.indexOf(":", receivedData.indexOf("msg:") + 6);
                    if (indexEnd == -1) {
                        indexEnd = receivedData.length();
                    }
                    String msg = receivedData.substring(receivedData.indexOf("msg:") + 4, indexEnd);
                    writer.append(String.format("\nСмс %s пришло на %s линию\n", msg, receivePort));
                    writer.close();
                    String str = "RECEIVE " + receivedData.substring(8, receivedData.indexOf(";")) + " OK\n";
                    byte[] sendingDataBuffer1 = str.getBytes();
                    DatagramPacket sendingPacket2 = new DatagramPacket(sendingDataBuffer1, sendingDataBuffer1.length, IPAddress, getPort(receivePort));
                    try {
                        clientSocket.send(sendingPacket2);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (receivedData.startsWith("STATE:") && receivedData.contains("gsm_remain_state:INCOMING")) {
                    int index = receivedData.indexOf("INCOMING");
                    String phone = receivedData.substring(index + 9, index + 21);
                    int indexGoip = receivedData.indexOf("goip0");
                    String receivePort = receivedData.substring(indexGoip + 5, indexGoip + 6);
                    BufferedWriter writer = new BufferedWriter(new FileWriter("goip.txt", true));
                    writer.append(String.format("\nЗвонок с номера: %s на %s линию\n", phone, receivePort));
                    System.out.println(receivedData);
                    writer.close();
                    String str = "STATE " + receivedData.substring(receivedData.indexOf(":") + 1, receivedData.indexOf(";")) + " OK\n";
                    byte[] sendingDataBuffer1 = str.getBytes();
                    DatagramPacket sendingPacket2 = new DatagramPacket(sendingDataBuffer1, sendingDataBuffer1.length, IPAddress, getPort(receivePort));
                    try {
                        clientSocket.send(sendingPacket2);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            // Закройте соединение с сервером через сокет
            //clientSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static int getPort(String port) {
        try {
            return (PORT / 10) * 10 + Integer.parseInt(port);
        } catch (Exception e) {
            return PORT;
        }
    }
}
