package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Agent {

  private final String name;

  private final String host;

  private final List<DeviceInfo> devicesInfo; // todo создать его сразу

  public Agent(String name, String host) { // todo notnull везде
    this.name = name;
    this.host = host;
    this.devicesInfo = new ArrayList<>();
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
  public List<DeviceInfo> getDeviceInfoList() {
    return devicesInfo;
  }
}
