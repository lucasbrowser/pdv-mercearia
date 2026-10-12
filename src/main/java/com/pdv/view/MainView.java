package com.pdv.view;

import com.pdv.controller.AuthController;
import com.pdv.model.Permissao;
import com.pdv.model.Usuario;
import com.pdv.util.Sessao;

import javax.swing.*;
import java.awt.*;

public class MainView extends JFrame {

    public MainView() {
        super("PDV Mercado de Bairro - Menu principal");
        Usuario u = Sessao.atual();
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel boasVindas = new JLabel("Olá, " + u.nome() + (u.admin() ? "  (Administrador)" : ""));
        boasVindas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        boasVindas.setBorder(BorderFactory.createEmptyBorder(14, 20, 4, 20));

        JPanel grade = new JPanel(new GridLayout(0, 3, 14, 14));
        grade.setBorder(BorderFactory.createEmptyBorder(14, 20, 20, 20));
        grade.add(card("Caixa (PDV)", Permissao.VENDER, this::abrirPdv));
        grade.add(card("Produtos", Permissao.GERENCIAR_PRODUTOS, () -> new ProdutoView(this).setVisible(true)));
        grade.add(card("Estoque", Permissao.GERENCIAR_ESTOQUE, () -> new EstoqueView(this).setVisible(true)));
        grade.add(card("Vendas", Permissao.CONSULTAR_VENDAS, () -> new VendasView(this).setVisible(true)));
        grade.add(card("Usuários", Permissao.GERENCIAR_USUARIOS, () -> new UsuarioView(this).setVisible(true)));
        JButton sair = Ui.botao("Trocar usuário / Sair", this::trocarUsuario);
        sair.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        grade.add(sair);

        add(boasVindas, BorderLayout.NORTH);
        add(grade, BorderLayout.CENTER);
        setSize(720, 360);
        setLocationRelativeTo(null);
    }

    private JButton card(String texto, Permissao exigida, Runnable acao) {
        JButton b = Ui.botao(texto, acao);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        b.setEnabled(Sessao.pode(exigida));
        if (!b.isEnabled()) b.setToolTipText("Sem permissão: " + exigida.getDescricao());
        return b;
    }

    private void abrirPdv() {
        setVisible(false);
        new PdvView(() -> setVisible(true)).setVisible(true);
    }

    private void trocarUsuario() {
        new AuthController().sair();
        dispose();
        new LoginView(new AuthController()).setVisible(true);
    }
}
