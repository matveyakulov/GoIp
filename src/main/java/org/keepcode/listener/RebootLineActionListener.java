package org.keepcode.listener;

import org.keepcode.response.Response;
import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RebootLineActionListener implements ActionListener {

    private final JComboBox<String> number;

    public RebootLineActionListener(JComboBox<String> number) {
        this.number = number;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GsmService.lineOff((String) number.getSelectedItem());
        number.setSelectedIndex(0);
    }
}
