package org.keepcode.domain;

import org.jetbrains.annotations.NotNull;

public class SimUssdCommand {

  private final String numInfo;

  public SimUssdCommand(@NotNull String numInfo) {
    this.numInfo = numInfo;
  }

  @NotNull
  public String getNumInfo() {
    return numInfo;
  }
}
