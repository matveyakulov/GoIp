package org.keepcode.factory;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressFactory {

  @NotNull
  public static InetAddress getAddress(String host) throws UnknownHostException {
    return InetAddress.getByName(host);
  }
}
