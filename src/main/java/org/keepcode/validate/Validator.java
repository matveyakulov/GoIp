package org.keepcode.validate;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class Validator {
  //todo у тебя столько регулярок для номера...
  private static final Pattern NUM_PATTERN = Pattern.compile("^\\+?\\d{11}$");
  //todo кривая
  private static final Pattern USSD_PATTERN = Pattern.compile("[#*\\d]+");

  public static boolean isValidNum(@NotNull String num) {
    return NUM_PATTERN.matcher(num).find();
  }

  public static boolean isValidUssd(@NotNull String ussd) {
    return USSD_PATTERN.matcher(ussd).find();
  }
}
