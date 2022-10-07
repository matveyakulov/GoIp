package org.keepcode.factory;

import org.keepcode.util.PropUtil;

import java.net.DatagramSocket;
import java.net.SocketException;

public class DatagramSocketFactory {

  private static final int SOCKET_TIMEOUT = PropUtil.getSocketTimeout();

  public static DatagramSocket getSocket() throws SocketException {
    DatagramSocket datagramSocket = new DatagramSocket();
    datagramSocket.setSoTimeout(SOCKET_TIMEOUT);
    return datagramSocket;
  }
}