package org.keepcode.validate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {

  public static boolean validateNum(String num){
    Pattern pattern = Pattern.compile("^\\+?\\d{11}$");
    Matcher matcher = pattern.matcher(num);
    return matcher.matches();
  }

  public static boolean validateUssd(String ussd){
    Pattern pattern = Pattern.compile("[+, #,*, \\d]*");
    Matcher matcher = pattern.matcher(ussd);
    return matcher.matches();
  }
}
