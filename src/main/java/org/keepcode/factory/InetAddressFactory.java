package org.keepcode.factory;

import org.keepcode.util.PropUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressFactory {

  private static final String HOST = PropUtil.getHost();

  public static InetAddress getAddress() throws UnknownHostException {
    return InetAddress.getByName(HOST);
  }
}
