package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DeviceInfo {

  private final String sn;

  private final String firmware;

  private final String model;

  public DeviceInfo(String sn, String firmware, String model) {
    this.sn = sn;
    this.firmware = firmware;
    this.model = model;
  }

  @NotNull
  public String getSn() {
    return sn;
  }

  @NotNull
  public String getFirmware() {
    return firmware;
  }

  @NotNull
  public String getModel() {
    return model;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeviceInfo that = (DeviceInfo) o;
    return sn.equals(that.sn) && firmware.equals(that.firmware) && model.equals(that.model);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sn, firmware, model);
  }
}
