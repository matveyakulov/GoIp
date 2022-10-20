package org.keepcode.db;

import org.jetbrains.annotations.NotNull;
import org.keepcode.domain.Agent;
import org.keepcode.domain.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

public class AgentDB {

  private static List<Agent> agents;

  @NotNull
  private static List<Agent> getAvailableAgents() {
    if (agents == null) {
      agents = new ArrayList<>();
      agents.add(new Agent("Mat", "192.168.2.3",
        new DeviceInfo("8MCDRM18047514", "GST1610-1.01-62-4", "GoIPx8")));
    }
    return agents;
  }

  public static boolean containsHostAndDeviceInfo(@NotNull String host, @NotNull DeviceInfo deviceInfo) {
    for (Agent agent : getAvailableAgents()) {
      if (agent.getHost().equals(host) && agent.getDeviceInfo().equals(deviceInfo)) {
        return true;
      }
    }
    return false;
  }
}
