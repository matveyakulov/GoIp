package org.keepcode.swing;

import javafx.scene.control.ComboBox;
import org.keepcode.listener.NumberInfoActionListener;
import org.keepcode.listener.RebootLineActionListener;
import org.keepcode.listener.SetNumActionListener;
import org.keepcode.listener.UssdActionListener;
import org.keepcode.service.GsmService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class MainFrame extends JFrame {

    private Box sendUssdAnswer;

    private Box numberInfoAnswer;

    private final String[] lines = {"1", "2", "3", "4", "5", "6", "7", "8"};
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
        setVisible(true);
        add(mainBox);
    }

    private Box createUssdCommand(){
        JButton sendUssdBtn = new JButton("Отправить");
        JTextField sendUssdValue = new CustomTextField();
        sendUssdValue.setEditable(true);
        sendUssdAnswer = Box.createHorizontalBox();
        ComboBoxLines linesCombo = new ComboBoxLines(lines);
        sendUssdBtn.addActionListener(new UssdActionListener(linesCombo, sendUssdValue, sendUssdAnswer));
        Box innerBox = Box.createHorizontalBox();
        innerBox.add(new JLabel("Отправить ussd:"));
        innerBox.add(sendUssdValue);
        innerBox.add(new JLabel("на линию:"));
        innerBox.add(linesCombo);
        innerBox.add(sendUssdBtn);
        return innerBox;
    }

    private Box createRebootCommand(){
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

    private Box createNumberInfoCommand(){
        JTextField numberInfoLine = new CustomTextField();
        numberInfoLine.setEditable(true);
        JButton sendUssdBtn = new JButton("Отправить");
        JTextField line = new CustomTextField();
        line.setEditable(true);
        numberInfoAnswer = Box.createHorizontalBox();
        ComboBoxLines linesCombo = new ComboBoxLines(lines);
        sendUssdBtn.addActionListener(new NumberInfoActionListener(linesCombo, numberInfoAnswer));
        Box innerBox = Box.createHorizontalBox();
        innerBox.add(new JLabel("Узнать номер на линии:"));
        innerBox.add(linesCombo);
        innerBox.add(sendUssdBtn);
        return innerBox;
    }

    private Box createRebootLineCommand(){
        JTextField numberInfoLine = new CustomTextField();
        numberInfoLine.setEditable(true);
        JButton sendUssdBtn = new JButton("Отправить");
        JTextField line = new CustomTextField();
        line.setEditable(true);
        ComboBoxLines linesCombo = new ComboBoxLines(lines);
        sendUssdBtn.addActionListener(new RebootLineActionListener(linesCombo));
        Box innerBox = Box.createHorizontalBox();
        innerBox.add(new JLabel("Перезагрузить линию:"));
        innerBox.add(linesCombo);
        innerBox.add(sendUssdBtn);
        return innerBox;
    }

    private Box createSetGsmNumCommand(){
        JTextField number = new CustomTextField();
        number.setEditable(true);
        JButton sendUssdBtn = new JButton("Отправить");
        JTextField line = new CustomTextField();
        line.setEditable(true);
        sendUssdAnswer = Box.createHorizontalBox();
        ComboBoxLines linesCombo = new ComboBoxLines(lines);
        sendUssdBtn.addActionListener(new SetNumActionListener(number, linesCombo));
        Box innerBox = Box.createHorizontalBox();
        innerBox.add(new JLabel("На линии:"));
        innerBox.add(linesCombo);
        innerBox.add(new JLabel("изменить номер на:"));
        innerBox.add(number);
        innerBox.add(sendUssdBtn);
        return innerBox;
    }
}
