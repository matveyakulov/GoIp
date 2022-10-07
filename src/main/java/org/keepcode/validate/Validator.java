package org.keepcode.validate;

import org.keepcode.match.TextMatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keepcode.match.TextMatcher.match;

public class Validator {

  private static final Pattern NUM_PATTERN = Pattern.compile("^\\+?\\d{11}$");
  //todo тут даже идея говорит о дубликатах
  private static final Pattern USSD_PATTERN = Pattern.compile("[+, #,*, \\d]*");

  public static boolean validateNum(String num){
    //todo а просто find не подойдет?
    return match(NUM_PATTERN, num).length() != 0;
  }

  public static boolean validateUssd(String ussd){
    return match(USSD_PATTERN, ussd).length() != 0;
  }
}
