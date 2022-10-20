package org.keepcode.domain;

import java.util.Objects;

public class DeviceInfo {

  private final String serialNumber;

  private final String firmware;

  private final String model;

  public DeviceInfo(String serialNumber, String firmware, String model) {
    this.serialNumber = serialNumber;
    this.firmware = firmware;
    this.model = model;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeviceInfo that = (DeviceInfo) o;
    return serialNumber.equals(that.serialNumber) && firmware.equals(that.firmware) && model.equals(that.model);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serialNumber, firmware, model);
  }
}
