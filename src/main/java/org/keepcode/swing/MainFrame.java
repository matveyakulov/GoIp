package org.keepcode.swing;

import org.keepcode.listener.NumberInfoActionListener;
import org.keepcode.listener.RebootLineActionListener;
import org.keepcode.listener.SetNumActionListener;
import org.keepcode.listener.UssdActionListener;
import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

  private Box sendUssdAnswer;

  private Box numberInfoAnswer;

  private String[] lines = new String[0];

  private ComboBoxLines comboBoxLinesStatus = new ComboBoxLines(lines);

  private ComboBoxLines linesComboUssd = new ComboBoxLines(lines);

  private ComboBoxLines linesComboGetNumInfo = new ComboBoxLines(lines);

  private ComboBoxLines linesComboRebootLine = new ComboBoxLines(lines);

  private ComboBoxLines linesComboSetGsmNum = new ComboBoxLines(lines);

  int port = 7777;

  public MainFrame() throws HeadlessException {
    super("Goip");
    Box mainBox = Box.createVerticalBox();
    setSize(500, 500);
    mainBox.add(createUssdCommand());
    mainBox.add(sendUssdAnswer);
    mainBox.add(createRebootCommand());
    mainBox.add(createNumberInfoCommand());
    mainBox.add(numberInfoAnswer);
    mainBox.add(createRebootLineCommand());
    mainBox.add(createSetGsmNumCommand());
    mainBox.add(comboBoxLinesStatus);
    setVisible(true);
    add(mainBox);
    new Thread(() -> {
      while (true) {
        Map<String, String> linesStatus = GsmService.getLinesStatus(port);
        comboBoxLinesStatus.setModel(new DefaultComboBoxModel<>(createStatusLine(linesStatus)));
        lines = linesStatus.keySet().stream().toArray(String[]::new);
        linesComboRebootLine.setModel(new DefaultComboBoxModel<>(lines));
        linesComboUssd.setModel(new DefaultComboBoxModel<>(lines));
        linesComboGetNumInfo.setModel(new DefaultComboBoxModel<>(lines));
        linesComboSetGsmNum.setModel(new DefaultComboBoxModel<>(lines));
        revalidate();
      }
    }).start();
  }

  private String[] createStatusLine(Map<String, String> lineStatus) {
    List<String> lines = new ArrayList<>(lineStatus.size());
    for (String s : lineStatus.keySet()) {
      lines.add(String.format("%s - %s", s, lineStatus.get(s)));
    }
    return lines.toArray(new String[0]);
  }

  private Box createUssdCommand() {
    JButton sendUssdBtn = new JButton("Отправить");
    JTextField sendUssdValue = new CustomTextField();
    sendUssdValue.setEditable(true);
    sendUssdAnswer = Box.createHorizontalBox();
    sendUssdBtn.addActionListener(new UssdActionListener(linesComboUssd, sendUssdValue, sendUssdAnswer));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Отправить ussd:"));
    innerBox.add(sendUssdValue);
    innerBox.add(new JLabel("на линию:"));
    innerBox.add(linesComboUssd);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  private Box createRebootCommand() {
    Box box = Box.createHorizontalBox();
    JButton button = new JButton("Рестарт goip");
    button.addActionListener(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          GsmService.reboot();
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    box.add(button);
    return box;
  }

  private Box createNumberInfoCommand() {
    JTextField numberInfoLine = new CustomTextField();
    numberInfoLine.setEditable(true);
    JButton sendUssdBtn = new JButton("Отправить");
    JTextField line = new CustomTextField();
    line.setEditable(true);
    numberInfoAnswer = Box.createHorizontalBox();
    sendUssdBtn.addActionListener(new NumberInfoActionListener(linesComboGetNumInfo, numberInfoAnswer));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Узнать номер на линии:"));
    innerBox.add(linesComboGetNumInfo);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  private Box createRebootLineCommand() {
    JTextField numberInfoLine = new CustomTextField();
    numberInfoLine.setEditable(true);
    JButton sendUssdBtn = new JButton("Отправить");
    JTextField line = new CustomTextField();
    line.setEditable(true);
    sendUssdBtn.addActionListener(new RebootLineActionListener(linesComboRebootLine));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Перезагрузить линию:"));
    innerBox.add(linesComboRebootLine);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  private Box createSetGsmNumCommand() {
    JTextField number = new CustomTextField();
    number.setEditable(true);
    JButton sendUssdBtn = new JButton("Отправить");
    JTextField line = new CustomTextField();
    line.setEditable(true);
    sendUssdAnswer = Box.createHorizontalBox();
    sendUssdBtn.addActionListener(new SetNumActionListener(number, linesComboSetGsmNum));
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("На линии:"));
    innerBox.add(linesComboSetGsmNum);
    innerBox.add(new JLabel("изменить номер на:"));
    innerBox.add(number);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }
}
