package com.pdv.view;

import com.pdv.controller.AuthController;

import javax.swing.*;
import java.awt.*;

public class LoginView extends JFrame {
    private final AuthController ctrl;
    private final JTextField login = new JTextField(16);
    private final JPasswordField senha = new JPasswordField(16);

    public LoginView(AuthController ctrl) {
        super("PDV - Acesso ao sistema");
        this.ctrl = ctrl;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel titulo = new JLabel("PDV Mercado de Bairro", SwingConstants.CENTER);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));

        JPanel form = new JPanel(new GridBagLayout());
        Ui.linha(form, 0, "Usuário:", login);
        Ui.linha(form, 1, "Senha:", senha);

        JButton entrar = Ui.botao("Entrar", "finalizar", 'E', this::entrar);
        JPanel rodape = new JPanel();
        rodape.add(entrar);
        getRootPane().setDefaultButton(entrar);

        JPanel raiz = new JPanel(new BorderLayout(10, 15));
        raiz.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        raiz.add(titulo, BorderLayout.NORTH);
        raiz.add(form, BorderLayout.CENTER);
        raiz.add(rodape, BorderLayout.SOUTH);
        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
    }

    private void entrar() {
        ctrl.entrar(login.getText(), new String(senha.getPassword()));
        dispose();
        MainView menu = new MainView();
        menu.setVisible(true);
        menu.mostrarLembretesInicial();
    }
}
