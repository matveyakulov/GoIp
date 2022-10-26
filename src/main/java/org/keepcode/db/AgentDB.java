package org.keepcode.db;

import org.jetbrains.annotations.NotNull;
import org.keepcode.domain.Agent;
import org.keepcode.domain.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

public class AgentDB {

  private static final List<Agent> agents = new ArrayList<>();

  static {
    Agent agent = new Agent("Mat", "192.168.2.3");
    agent.getDeviceInfoList().add(new DeviceInfo("8MCDRM18047514", "GST1610-1.01-62-4", "GoIPx8"));
    agents.add(agent);
  }
  @NotNull
  private static List<Agent> getAvailableAgents() {
    return agents;
  }

  public static boolean containsHostAndDeviceInfo(@NotNull String host, @NotNull DeviceInfo deviceInfo) {
    for (Agent agent : getAvailableAgents()) {
      if (agent.getHost().equals(host) && agent.getDeviceInfoList().contains(deviceInfo)) {
        return true;
      }
    }
    return false;
  }
}
