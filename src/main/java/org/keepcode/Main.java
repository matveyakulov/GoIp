package org.keepcode;

import org.keepcode.swing.MainFrame;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

  public static void main(String[] args) {
    new MainFrame();
//    Pattern PHONE_NUMBER_FROM_USSD_PATTERN = Pattern.compile("\\s(?<phone>[+()\\-\\s\\d]+)");
//    String s = "Ваш номер 9625244968.";
//    String s1 = "Ваш федеральный номер +7 (904)421-86-71";
//    Matcher matcher = PHONE_NUMBER_FROM_USSD_PATTERN.matcher(s1);
//    if (matcher.find()){
//      System.out.println(matcher.group("phone"));
//    } else System.out.println("NOOOO!");
  }
}
