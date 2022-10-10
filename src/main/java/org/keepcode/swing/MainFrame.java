package org.keepcode.swing;

import org.jetbrains.annotations.NotNull;
import org.keepcode.domain.GsmLine;
import org.keepcode.service.GsmService;
import org.keepcode.validate.Validator;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static org.keepcode.validate.Validator.isValidNum;

public class MainFrame extends JFrame {

  private final Box sendUssdAnswer = Box.createHorizontalBox();
  private final Box numberInfoAnswer = Box.createHorizontalBox();
  private final Box rebootCommandAnswer = Box.createHorizontalBox();
  private final Box rebootLineCommandAnswer = Box.createHorizontalBox();

  private final Box setGsmNumAnswer = Box.createHorizontalBox();

  private String[] lines = new String[0];

  private final ComboBoxLines<String> comboBoxLinesStatus = new ComboBoxLines<>(new String[0]);

  private final ComboBoxLines<String> linesComboUssd = new ComboBoxLines<>(lines);

  private final ComboBoxLines<String> linesComboGetNumInfo = new ComboBoxLines<>(lines);

  private final ComboBoxLines<String> linesComboRebootLine = new ComboBoxLines<>(lines);

  private final ComboBoxLines<String> linesComboSetGsmNum = new ComboBoxLines<>(lines);

  private static Map<String, GsmLine> gsmLinesCurrent;

  private static JButton sendUssdBtn;
  private static JButton rebootGoipBtn;
  private static JButton sendNumInfoBtn;
  private static JButton rebootLineBtn;
  private static JButton sendSetNumBtn;

  public MainFrame() throws HeadlessException {
    super("Goip");
    Box mainBox = Box.createVerticalBox();
    setSize(500, 500);
    mainBox.add(createUssdCommand());
    mainBox.add(sendUssdAnswer);
    mainBox.add(createRebootCommand());
    mainBox.add(rebootCommandAnswer);
    mainBox.add(createNumberInfoCommand());
    mainBox.add(numberInfoAnswer);
    mainBox.add(createRebootLineCommand());
    mainBox.add(rebootLineCommandAnswer);
    mainBox.add(createSetGsmNumCommand());
    mainBox.add(setGsmNumAnswer);
    mainBox.add(comboBoxLinesStatus);
    setVisible(true);
    add(mainBox);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    changeEnableBtn();
    new Thread(GsmService::listen).start();
    new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(35 * 1000);
        } catch (InterruptedException e) {  // подождать не удалось - берем что есть
          System.out.println(e.getCause().getMessage());
        }
        Map<String, GsmLine> gsmLinesNew = GsmService.getGsmLineMap();

        if (gsmLinesCurrent == null ||
          !gsmLinesCurrent.values().equals(gsmLinesNew.values())) {
          gsmLinesCurrent = gsmLinesNew;
          SwingUtilities.invokeLater(() -> updateCheckBoxes(gsmLinesCurrent));
        }
      }
    }).start();
  }

  private void updateCheckBoxes(@NotNull Map<String, GsmLine> gsmLines) {
    if(comboBoxLinesStatus.getItemCount() == 0) {
      comboBoxLinesStatus.setModel(new DefaultComboBoxModel<>(createStatusLine(gsmLines)));
    } else {
      updateLinesStatusIfChanged(createStatusLine(gsmLines));
    }
    lines = gsmLines.keySet().toArray(new String[0]);
    linesComboRebootLine.setModel(new DefaultComboBoxModel<>(lines));
    linesComboUssd.setModel(new DefaultComboBoxModel<>(lines));
    linesComboGetNumInfo.setModel(new DefaultComboBoxModel<>(lines));
    linesComboSetGsmNum.setModel(new DefaultComboBoxModel<>(lines));
    if (!gsmLines.isEmpty()) {
      changeEnableBtn();
    }
    revalidate();
  }

  private void updateLinesStatusIfChanged(@NotNull String[] statusLine){
    ComboBoxModel<String> lineFromModel = comboBoxLinesStatus.getModel();
    for(int i = 0; i < statusLine.length; i++){
      if(!statusLine[i].equals(lineFromModel.getElementAt(i))){
        comboBoxLinesStatus.insertItemAt(statusLine[i], i);
      }
    }
    if(statusLine.length < comboBoxLinesStatus.getItemCount()){
      for(int i = statusLine.length; i < comboBoxLinesStatus.getItemCount(); i++){
        comboBoxLinesStatus.remove(i);
      }
    }
  }

  private static void changeEnableBtn() {
    boolean enable = !sendUssdBtn.isEnabled();
    sendUssdBtn.setEnabled(enable);
    rebootGoipBtn.setEnabled(enable);
    sendNumInfoBtn.setEnabled(enable);
    rebootLineBtn.setEnabled(enable);
    sendSetNumBtn.setEnabled(enable);
  }

  @NotNull
  private String[] createStatusLine(@NotNull Map<String, GsmLine> lineStatus) {
    String[] lines = new String[lineStatus.size()];
    int i = 0;
    for (String lineId : lineStatus.keySet()) {
      lines[i++] = String.format("%s - %s", lineId, lineStatus.get(lineId).getStatus().getStatus());
    }
    return lines;
  }

  @NotNull
  private Box createUssdCommand() {
    sendUssdBtn = new JButton("Отправить");
    JTextField sendUssdValue = new CustomTextField();
    sendUssdBtn.addActionListener(e ->
      new Thread(() -> {
        if (Validator.isValidUssd(sendUssdValue.getText()) && linesComboUssd.getSelectedItem() != null) {
          String lineId = (String) linesComboUssd.getSelectedItem();
          String response = GsmService.sendUssd(lineId, sendUssdValue.getText(), gsmLinesCurrent.get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            sendUssdAnswer.add(new JLabel(response));
            linesComboUssd.setSelectedIndex(0);
            sendUssdValue.setText("");
            sendUssdAnswer.revalidate();
          });
        }
      }).start());
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Отправить ussd:"));
    innerBox.add(sendUssdValue);
    innerBox.add(new JLabel("на линию:"));
    innerBox.add(linesComboUssd);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  @NotNull
  private Box createRebootCommand() {
    Box box = Box.createHorizontalBox();
    rebootGoipBtn = new JButton("Рестарт goip");
    rebootGoipBtn.addActionListener(e -> {
      new Thread(() -> {
        if (gsmLinesCurrent != null && !gsmLinesCurrent.isEmpty()) {
          String line = gsmLinesCurrent.keySet().stream().findFirst().get();
          gsmLinesCurrent.get(line);
          String answer = GsmService.reboot(line, gsmLinesCurrent.get(line).getPassword());
          SwingUtilities.invokeLater(() -> {
            rebootCommandAnswer.add(new JLabel(answer));
            rebootCommandAnswer.revalidate();
          });
        }
      }).start();
    });
    box.add(rebootGoipBtn);
    return box;
  }

  @NotNull
  private Box createNumberInfoCommand() {
    sendNumInfoBtn = new JButton("Отправить");
    sendNumInfoBtn.addActionListener(e ->
      new Thread(() -> {
        if (linesComboGetNumInfo.getSelectedItem() != null) {
          String lineId = (String) linesComboGetNumInfo.getSelectedItem();
          String response = GsmService.numberInfo(lineId, gsmLinesCurrent.get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            numberInfoAnswer.add(new JLabel(response));
            linesComboGetNumInfo.setSelectedIndex(0);
            numberInfoAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Узнать номер на линии:"));
    innerBox.add(linesComboGetNumInfo);
    innerBox.add(sendNumInfoBtn);
    return innerBox;
  }

  @NotNull
  private Box createRebootLineCommand() {
    rebootLineBtn = new JButton("Отправить");
    rebootLineBtn.addActionListener(e ->
      new Thread(() -> {
        if (linesComboRebootLine.getSelectedItem() != null) {
          String lineId = (String) linesComboRebootLine.getSelectedItem();
          String answer = GsmService.lineReboot(lineId, gsmLinesCurrent.get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            linesComboRebootLine.setSelectedIndex(0);
            rebootLineCommandAnswer.add(new JLabel(answer));
            rebootLineCommandAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Перезагрузить линию:"));
    innerBox.add(linesComboRebootLine);
    innerBox.add(rebootLineBtn);
    innerBox.add(rebootLineCommandAnswer);
    return innerBox;
  }

  @NotNull
  private Box createSetGsmNumCommand() {
    JTextField number = new CustomTextField();
    sendSetNumBtn = new JButton("Отправить");
    sendSetNumBtn.addActionListener(e ->
      new Thread(() -> {
        if (isValidNum(number.getText()) && linesComboSetGsmNum.getSelectedItem() != null) {
          String lineId = (String) linesComboSetGsmNum.getSelectedItem();
          String answer = GsmService.setGsmNum(lineId, number.getText(), gsmLinesCurrent.get(lineId).getPassword());
          linesComboSetGsmNum.setSelectedIndex(0);
          SwingUtilities.invokeLater(() -> {
            number.setText("");
            setGsmNumAnswer.add(new JLabel(answer));
            setGsmNumAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("На линии:"));
    innerBox.add(linesComboSetGsmNum);
    innerBox.add(new JLabel("изменить номер на:"));
    innerBox.add(number);
    innerBox.add(sendSetNumBtn);
    return innerBox;
  }
}
