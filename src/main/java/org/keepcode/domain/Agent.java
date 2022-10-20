package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;

public class Agent {

  private final String name;

  private final String host;

  private final DeviceInfo deviceInfo;

  public Agent(String name, String host, DeviceInfo deviceInfo) {
    this.name = name;
    this.host = host;
    this.deviceInfo = deviceInfo;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public String getHost() {
    return host;
  }

  @NotNull
  public DeviceInfo getDeviceInfo() {
    return deviceInfo;
  }
}
