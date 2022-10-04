package org.keepcode.listener;

import org.keepcode.response.Response;
import org.keepcode.service.GsmService;
import org.keepcode.validate.Validator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UssdActionListener implements ActionListener {

  private final JComboBox<String> line;
  private final JTextField command;

  private final Box answer;

  public UssdActionListener(JComboBox<String> line, JTextField command, Box answer) {
    this.line = line;
    this.command = command;
    this.answer = answer;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (Validator.validateUssd(command.getText())) {
      Response response = GsmService.sendUssd((String) line.getSelectedItem(), command.getText());
      answer.add(new JLabel(response.getBody()));
      line.setSelectedIndex(0);
      command.setText("");
      answer.revalidate();
    }
  }
}
