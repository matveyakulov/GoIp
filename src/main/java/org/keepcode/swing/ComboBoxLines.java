package org.keepcode.swing;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class ComboBoxLines extends JComboBox<String> {

    public ComboBoxLines(String[] items) {
        super(items);
        setMaximumSize(new Dimension(100, 50));
    }
}
