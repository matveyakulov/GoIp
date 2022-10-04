package org.keepcode.swing;

import javax.swing.*;
import java.awt.*;

public class CustomTextField extends JTextField {

    public CustomTextField() {
        setMaximumSize(new Dimension(100, 50));
        setEditable(true);
    }
}
