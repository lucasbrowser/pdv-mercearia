package com.pdv;

import com.pdv.controller.AuthController;
import com.pdv.util.Database;
import com.pdv.view.LoginView;

import javax.swing.*;
import java.awt.Font;

public class App {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        } catch (Exception ignored) { }
        Database.init();
        SwingUtilities.invokeLater(() -> new LoginView(new AuthController()).setVisible(true));
    }
}
