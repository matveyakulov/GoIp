package org.keepcode.swing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.keepcode.domain.GsmLine;
import org.keepcode.service.GsmService;
import org.keepcode.validate.Validator;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keepcode.validate.Validator.isValidNum;

public class MainFrame extends JFrame {

  private final Box sendUssdAnswer = Box.createHorizontalBox();
  private final Box numberInfoAnswer = Box.createHorizontalBox();
  private final Box rebootCommandAnswer = Box.createHorizontalBox();
  private final Box rebootLineCommandAnswer = Box.createHorizontalBox();

  private final Box setGsmNumCommandAnswer = Box.createHorizontalBox();

  private final Box sendSmsCommandAnswer = Box.createHorizontalBox();

  private final Box mainBox = Box.createVerticalBox();

  private final JComboBox<String> linesStatusComboBox = getComboBox();

  private final JComboBox<String> sendUssdComboBox = getComboBox();

  private final JComboBox<String> getNumInfoComboBox = getComboBox();

  private final JComboBox<String> rebootLineComboBox = getComboBox();

  private final JComboBox<String> setGsmNumComboBox = getComboBox();

  private final JComboBox<String> sendSmsComboBox = getComboBox();

  private final JComboBox<String> hostComboBox = getComboBox();

  private static Map<String, Map<String, GsmLine>> hostLinesInfoCurrent;

  private static JButton sendUssdBtn;
  private static JButton rebootGoIpBtn;
  private static JButton sendNumInfoBtn;
  private static JButton rebootLineBtn;
  private static JButton sendSetNumBtn;

  private static JButton sendSmsBtn;

  public MainFrame() {
    super("Goip");
    setSize(750, 500);
    hostComboBox.addActionListener((e) -> {
      String host = (String) hostComboBox.getSelectedItem();
      SwingUtilities.invokeLater(() -> {
        refreshFrame(host);
        updateLinesComboBox(hostLinesInfoCurrent.get(host));
      });
    });
    setVisible(true);
    add(mainBox);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Thread listenThread = new Thread(GsmService::listen);
    listenThread.setDaemon(true);
    listenThread.start();
    Thread updateWindowThread = new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(35 * 1000);
        } catch (InterruptedException e) {
          System.out.println("Поток не смог остановиться");
        }
        try {
          Map<String, Map<String, GsmLine>> hostLinesInfoNew = GsmService.getHostLineInfo();
          if ((hostLinesInfoCurrent == null  || !hostLinesInfoCurrent.equals(hostLinesInfoNew) && !hostLinesInfoNew.isEmpty())) {
            hostLinesInfoCurrent = hostLinesInfoNew;
            hostLinesInfoCurrent.put("123", new HashMap<>());  // это чтобы был еще один хост для теста
            String currentHost = hostComboBox.getSelectedItem() != null ?
              (String) hostComboBox.getSelectedItem() :
              hostLinesInfoNew.keySet().stream().findFirst().get();
            SwingUtilities.invokeLater(() -> {
              refreshFrame(currentHost);
              updateHostComboBoxIfChanged(hostLinesInfoCurrent.keySet().toArray(new String[0]));
              updateLinesComboBox(hostLinesInfoCurrent.get(currentHost));
            });
          }
        } catch (NoClassDefFoundError error) {
          mainBox.add(new JLabel("Программа завершилась с ошибкой: " + error.getMessage()));
          revalidate();
          try {
            Thread.sleep(5 * 1000);
          } catch (InterruptedException e) {
            System.exit(-1);
          }
          System.exit(-1);
        }
      }
    });
    updateWindowThread.setDaemon(true);
    updateWindowThread.start();
  }

  private void refreshFrame(String currentHost) {
    mainBox.removeAll();
    mainBox.add(hostComboBox);
    mainBox.add(createUssdCommand(currentHost));
    mainBox.add(sendUssdAnswer);
    mainBox.add(createRebootCommand(currentHost));
    mainBox.add(rebootCommandAnswer);
    mainBox.add(createNumberInfoCommand(currentHost));
    mainBox.add(numberInfoAnswer);
    mainBox.add(createRebootLineCommand(currentHost));
    mainBox.add(rebootLineCommandAnswer);
    mainBox.add(createSetGsmNumCommand(currentHost));
    mainBox.add(setGsmNumCommandAnswer);
    mainBox.add(createSendSmsCommand(currentHost));
    mainBox.add(sendSmsCommandAnswer);
    mainBox.add(linesStatusComboBox);
    revalidate();
  }

  private void updateHostComboBoxIfChanged(@NotNull String[] hosts) {
    if (hostComboBox.getItemCount() == 0) {
      hostComboBox.setModel(new DefaultComboBoxModel<>(hosts));
    } else {
      updateComboBoxIfChanged(hostComboBox, hosts);
    }
  }

  private void updateLinesComboBox(@NotNull Map<String, GsmLine> gsmLines) {
    String[] linesId = gsmLines.keySet().toArray(new String[0]);
    if (linesStatusComboBox.getItemCount() == 0) {
      linesStatusComboBox.setModel(new DefaultComboBoxModel<>(createStatusLine(gsmLines)));
      comboBoxesLinesSetModel(linesId);
    } else {
      updateComboBoxIfChanged(linesStatusComboBox, createStatusLine(gsmLines));
      updateLinesComboBoxIfChanged(linesId);
    }
    changeEnableBtn(!gsmLines.isEmpty());
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

  private void changeEnableBtn(boolean enable) {
    if (enable != sendUssdBtn.isEnabled()) {
      for (JButton button : getAllButtons()) {
        button.setEnabled(enable);
      }
    }
  }

  @NotNull
  private List<JButton> getAllButtons() {
    return Arrays.asList(sendUssdBtn, rebootLineBtn, rebootGoIpBtn, sendNumInfoBtn, sendSetNumBtn, sendSmsBtn);
  }

  private void comboBoxesLinesSetModel(@NotNull String[] linesId) {
    List<JComboBox<String>> comboBoxesLines = getAllComboBoxesLines();
    for (JComboBox<String> comboBoxesLine : comboBoxesLines) {
      comboBoxesLine.setModel(new DefaultComboBoxModel<>(linesId));
    }
  }

  private List<JComboBox<String>> getAllComboBoxesLines() {
    return Arrays.asList(getNumInfoComboBox, rebootLineComboBox, sendUssdComboBox, setGsmNumComboBox, sendSmsComboBox);
  }

  private void updateComboBoxIfChanged(@NotNull JComboBox<String> comboBox, @NotNull String[] values) {
    ComboBoxModel<String> lineFromModel = comboBox.getModel();
    for (int i = 0; i < values.length; i++) {
      if (!values[i].equals(lineFromModel.getElementAt(i))) {
        comboBox.insertItemAt(values[i], i);
      }
    }
    while (values.length < comboBox.getItemCount()) {
      comboBox.removeItemAt(values.length);
    }
  }

  private void updateLinesComboBoxIfChanged(@NotNull String[] lineId) {
    ComboBoxModel<String> lineFromModel = sendUssdComboBox.getModel();
    for (int i = 0; i < lineId.length; i++) {
      if (!lineId[i].equals(lineFromModel.getElementAt(i))) {
        comboBoxesLinesInsertItemAt(lineId[i], i);
      }
    }
    while (lineId.length < sendUssdComboBox.getItemCount()) {
      comboBoxesLinesRemoveItemAt(lineId.length);
    }
  }

  private void comboBoxesLinesInsertItemAt(@NotNull String lineId, int index) {
    List<JComboBox<String>> comboBoxesLines = getAllComboBoxesLines();
    for (JComboBox<String> comboBoxesLine : comboBoxesLines) {
      comboBoxesLine.insertItemAt(lineId, index);
    }
  }

  private void comboBoxesLinesRemoveItemAt(int index) {
    List<JComboBox<String>> comboBoxesLines = getAllComboBoxesLines();
    for (JComboBox<String> comboBoxesLine : comboBoxesLines) {
      comboBoxesLine.removeItemAt(index);
    }
  }

  @NotNull
  private Box createUssdCommand(@NotNull String host) {
    sendUssdBtn = new JButton("Отправить");
    JTextField ussdValue = getTextField();
    sendUssdBtn.addActionListener(e ->
      new Thread(() -> {
        if (Validator.isValidUssd(ussdValue.getText()) && sendUssdComboBox.getSelectedItem() != null) {
          String lineId = (String) sendUssdComboBox.getSelectedItem();
          String response = GsmService.sendUssd(host, lineId, ussdValue.getText(),
            hostLinesInfoCurrent.get(host).get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            sendUssdAnswer.removeAll();
            sendUssdAnswer.add(new JLabel(response));
            sendUssdAnswer.revalidate();
          });
        }
      }).start());
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Отправить ussd:"));
    innerBox.add(ussdValue);
    innerBox.add(new JLabel("на линию:"));
    innerBox.add(sendUssdComboBox);
    innerBox.add(sendUssdBtn);
    return innerBox;
  }

  @NotNull
  private Box createRebootCommand(@NotNull String host) {
    Box box = Box.createHorizontalBox();
    rebootGoIpBtn = new JButton("Рестарт GoIp");
    rebootGoIpBtn.addActionListener(e -> {
      new Thread(() -> {
        if (hostLinesInfoCurrent != null && !hostLinesInfoCurrent.isEmpty()) {
          String line = hostLinesInfoCurrent.keySet().stream().findFirst().get();
          String answer = GsmService.reboot(host, line, hostLinesInfoCurrent.get(host).get(line).getPassword());
          SwingUtilities.invokeLater(() -> {
            rebootCommandAnswer.removeAll();
            rebootCommandAnswer.add(new JLabel(answer));
            rebootCommandAnswer.revalidate();
            hostLinesInfoCurrent.clear();
            clearLinesComboBox();
            GsmService.clearHostLineInfo();
          });
        }
      }).start();
    });
    box.add(rebootGoIpBtn);
    return box;
  }

  private void clearLinesComboBox() {
    getComboBox();
    for (JComboBox<String> comboBox : getAllComboBoxesLines()) {
      comboBox.removeAll();
    }
  }

  @NotNull
  private Box createNumberInfoCommand(@NotNull String host) {
    sendNumInfoBtn = new JButton("Узнать номер на линии");
    sendNumInfoBtn.addActionListener(e ->
      new Thread(() -> {
        if (getNumInfoComboBox.getSelectedItem() != null) {
          String lineId = (String) getNumInfoComboBox.getSelectedItem();
          String response = GsmService.numberInfo(host, lineId, hostLinesInfoCurrent.get(host).get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            numberInfoAnswer.removeAll();
            numberInfoAnswer.add(new JLabel(response));
            numberInfoAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(getNumInfoComboBox);
    innerBox.add(sendNumInfoBtn);
    return innerBox;
  }

  @NotNull
  private Box createRebootLineCommand(@NotNull String host) {
    rebootLineBtn = new JButton("Перезагрузить линию");
    rebootLineBtn.addActionListener(e ->
      new Thread(() -> {
        if (rebootLineComboBox.getSelectedItem() != null) {
          String lineId = (String) rebootLineComboBox.getSelectedItem();
          String answer = GsmService.lineReboot(host, lineId, hostLinesInfoCurrent.get(host).get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            rebootLineCommandAnswer.removeAll();
            rebootLineCommandAnswer.add(new JLabel(answer));
            rebootLineCommandAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(rebootLineComboBox);
    innerBox.add(rebootLineBtn);
    innerBox.add(rebootLineCommandAnswer);
    return innerBox;
  }

  @NotNull
  private Box createSetGsmNumCommand(@NotNull String host) {
    JTextField number = getTextField();
    sendSetNumBtn = new JButton("Изменить");
    sendSetNumBtn.addActionListener(e ->
      new Thread(() -> {
        if (isValidNum(number.getText()) && setGsmNumComboBox.getSelectedItem() != null) {
          String lineId = (String) setGsmNumComboBox.getSelectedItem();
          String answer = GsmService.setGsmNum(host, lineId, number.getText(),
            hostLinesInfoCurrent.get(host).get(lineId).getPassword());
          SwingUtilities.invokeLater(() -> {
            setGsmNumCommandAnswer.removeAll();
            setGsmNumCommandAnswer.add(new JLabel(answer));
            setGsmNumCommandAnswer.revalidate();
          });
        }
      }).start()
    );
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("На линии:"));
    innerBox.add(setGsmNumComboBox);
    innerBox.add(new JLabel("изменить номер на:"));
    innerBox.add(number);
    innerBox.add(sendSetNumBtn);
    return innerBox;
  }

  @NotNull
  private Box createSendSmsCommand(@NotNull String host) {
    sendSmsBtn = new JButton("Отправить");
    JTextField smsTextField = getTextField();
    JTextField phonesTextField = getTextField();
    sendSmsBtn.addActionListener(e ->
      new Thread(() -> {
        String[] phonesFromTextField = phonesTextField.getText().split("\\s");
        if (sendSmsComboBox.getSelectedItem() != null) {
          String lineId = (String) sendSmsComboBox.getSelectedItem();
          String response = GsmService.sendSms(host, lineId, phonesFromTextField, smsTextField.getText());
          SwingUtilities.invokeLater(() -> {
            sendSmsCommandAnswer.removeAll();
            sendSmsCommandAnswer.add(new JLabel(response));
            sendUssdAnswer.revalidate();
          });
        }
      }).start());
    Box innerBox = Box.createHorizontalBox();
    innerBox.add(new JLabel("Отправить sms:"));
    innerBox.add(smsTextField);
    innerBox.add(new JLabel("c линии:"));
    innerBox.add(sendSmsComboBox);
    innerBox.add(new JLabel("на номера(через пробел):"));
    innerBox.add(phonesTextField);
    innerBox.add(sendSmsBtn);
    return innerBox;
  }

  @NotNull
  private JComboBox<String> getComboBox() {
    JComboBox<String> comboBox = new JComboBox<String>(new String[0]) {
      @Nullable
      @Override
      public String getSelectedItem() {
        return (String) super.getSelectedItem();
      }
    };
    comboBox.setMaximumSize(new Dimension(170, 50));
    return comboBox;
  }

  @NotNull
  private JTextField getTextField() {
    JTextField textField = new JTextField();
    textField.setMaximumSize(new Dimension(100, 50));
    textField.setEditable(true);
    return textField;
  }
}
