package org.keepcode.listener;

import org.keepcode.response.Response;
import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SetNumActionListener implements ActionListener {

    private final JTextField number;
    private final JComboBox<String> line;

    public SetNumActionListener(JTextField number, JComboBox<String> line) {
        this.number = number;
        this.line = line;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Response response = GsmService.setGsmNum(number.getText(), (String) line.getSelectedItem());
        line.setSelectedIndex(0);
        number.setText("");
    }
}
