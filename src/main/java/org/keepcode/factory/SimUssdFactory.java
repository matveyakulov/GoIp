package org.keepcode.factory;

import org.keepcode.domain.SimUssdCommand;

import java.util.HashMap;
import java.util.Map;

public class SimUssdFactory {

  private static Map<Integer, Map<Integer, SimUssdCommand>> countryOperatorSimUssdCommand;

  public static Map<Integer, Map<Integer, SimUssdCommand>> getAllAvailableCountryOperatorSimUssdCommand() {
    if (countryOperatorSimUssdCommand == null) {
      countryOperatorSimUssdCommand = new HashMap<>();
      addCountryOperatorSimUssdCommand();
    }
    return countryOperatorSimUssdCommand;
  }

  private static void addCountryOperatorSimUssdCommand() {
    addRussianCountryOperatorSimUssdCommand();
  }

  private static void addRussianCountryOperatorSimUssdCommand() {
    Map<Integer, SimUssdCommand> simUssdCommandMap = new HashMap<>();
    simUssdCommandMap.put(1, new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(2, new SimUssdCommand("*201#"));
    simUssdCommandMap.put(13, new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(20, new SimUssdCommand("*201#"));
    simUssdCommandMap.put(28, new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(99, new SimUssdCommand("*111*0887#"));
    countryOperatorSimUssdCommand.put(250, simUssdCommandMap);
  }
}
