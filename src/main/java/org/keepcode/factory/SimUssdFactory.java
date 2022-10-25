package org.keepcode.factory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.domain.SimUssdCommand;
import org.keepcode.enums.Country;
import org.keepcode.enums.SimOperator;

import java.util.EnumMap;
import java.util.Map;

import static org.keepcode.enums.Country.RUSSIA;
import static org.keepcode.enums.SimOperator.BEELINE28;
import static org.keepcode.enums.SimOperator.BEELINE99;
import static org.keepcode.enums.SimOperator.KUBGSM;
import static org.keepcode.enums.SimOperator.MEGAFON;
import static org.keepcode.enums.SimOperator.MTC;
import static org.keepcode.enums.SimOperator.TELE2;

public class SimUssdFactory {

  private static Map<Country, Map<SimOperator, SimUssdCommand>> countryOperatorSimUssdCommand;

  @NotNull
  public static Map<Country, Map<SimOperator, SimUssdCommand>> getAllAvailableCountryOperatorSimUssdCommand() { //todo nullpointer may be
    if (countryOperatorSimUssdCommand == null) {
      countryOperatorSimUssdCommand = new EnumMap<>(Country.class);
      addCountryOperatorSimUssdCommand();
    }
    return countryOperatorSimUssdCommand;
  }

  private static void addCountryOperatorSimUssdCommand() {
    addRussianCountryOperatorSimUssdCommand();
  }  //todo странная хрень

  private static void addRussianCountryOperatorSimUssdCommand() {
    Map<SimOperator, SimUssdCommand> simUssdCommandMap = new EnumMap<>(SimOperator.class);
    simUssdCommandMap.put(MTC, new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(MEGAFON, new SimUssdCommand("*201#"));
    simUssdCommandMap.put(KUBGSM, new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(TELE2, new SimUssdCommand("*201#"));
    simUssdCommandMap.put(BEELINE28, new SimUssdCommand("*111*0887#"));
    simUssdCommandMap.put(BEELINE99, new SimUssdCommand("*111*0887#"));
    countryOperatorSimUssdCommand.put(RUSSIA, simUssdCommandMap);
  }

  @Nullable
  public static Country getCountryByCode(int countryCode) {  //todo чекнуть на нулл
    for (Country country : countryOperatorSimUssdCommand.keySet()) {
      if (country.getCode() == countryCode) {
        return country;
      }
    }
    return null;
  }

  @Nullable
  public static SimOperator containsCountryAndOperatorCode(@NotNull Country country, int operatorCode) { // todo нет обработки на county null
    for (SimOperator simOperator : countryOperatorSimUssdCommand.get(country).keySet()) {
      if (simOperator.getCode() == operatorCode) {
        return simOperator;
      }
    }
    return null;
  }
}
