package org.keepcode.factory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.domain.SimUssdCommand;
import org.keepcode.enums.Country;
import org.keepcode.enums.SimOperator;

import java.util.EnumMap;
import java.util.Map;

import static org.keepcode.enums.Country.RUSSIA;

public class SimUssdFactory {

  private static final Map<Country, Map<SimOperator, SimUssdCommand>> countryOperatorSimUssdCommand = new EnumMap<>(Country.class);

  static {
    addRussianCountryOperatorSimUssdCommand();
  }

  @NotNull
  public static Map<Country, Map<SimOperator, SimUssdCommand>> getAllAvailableCountryOperatorSimUssdCommand() {
    return countryOperatorSimUssdCommand;
  }

  private static void addRussianCountryOperatorSimUssdCommand() {
    Map<SimOperator, SimUssdCommand> simUssdCommandMap = new EnumMap<>(SimOperator.class);
    simUssdCommandMap.put(SimOperator.getOperatorByCode(1), new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(SimOperator.getOperatorByCode(2), new SimUssdCommand("*201#"));
    simUssdCommandMap.put(SimOperator.getOperatorByCode(13), new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(SimOperator.getOperatorByCode(20), new SimUssdCommand("*201#"));
    simUssdCommandMap.put(SimOperator.getOperatorByCode(28), new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(SimOperator.getOperatorByCode(99), new SimUssdCommand("*111*0887#"));
    countryOperatorSimUssdCommand.put(RUSSIA, simUssdCommandMap);
  }

  @Nullable
  public static Country getCountryByCode(int countryCode) {
    for (Country country : countryOperatorSimUssdCommand.keySet()) {
      if (country.getCode() == countryCode) {
        return country;
      }
    }
    return null;
  }

  @Nullable
  public static boolean containsCountryAndOperatorCode(@NotNull Country country, @NotNull SimOperator simOperator) {
    if(simOperator == SimOperator.UNKNOWN){
      return false;
    }
    Map<SimOperator, SimUssdCommand> simOperatorSimUssdCommandMap = countryOperatorSimUssdCommand.get(country);
    if(simOperatorSimUssdCommandMap == null){
      return false;
    }
    for (SimOperator simOperatorInMap : simOperatorSimUssdCommandMap.keySet()) {
      if (simOperator == simOperatorInMap) {
        return true;
      }
    }
    return false;
  }
}
