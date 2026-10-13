package com.pdv.view;

import com.pdv.controller.AuthController;
import com.pdv.controller.ContaPagarController;
import com.pdv.model.ContaPagar;
import com.pdv.model.Permissao;
import com.pdv.model.Usuario;
import com.pdv.util.Sessao;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainView extends JFrame {
    private final ContaPagarController contas = new ContaPagarController();
    private final JButton btnLembretes = new JButton();
    private List<ContaPagar> lembretes = List.of();

    public MainView() {
        super("PDV Mercado de Bairro - Menu principal");
        Usuario u = Sessao.atual();
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel boasVindas = new JLabel("Olá, " + u.nome() + (u.admin() ? "  (Administrador)" : ""));
        boasVindas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        btnLembretes.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnLembretes.addActionListener(e -> mostrarLembretes());

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(BorderFactory.createEmptyBorder(14, 20, 4, 20));
        topo.add(boasVindas, BorderLayout.WEST);
        topo.add(btnLembretes, BorderLayout.EAST);

        JPanel grade = new JPanel(new GridLayout(0, 3, 14, 14));
        grade.setBorder(BorderFactory.createEmptyBorder(14, 20, 20, 20));
        grade.add(card("Caixa (PDV)", Permissao.VENDER, this::abrirPdv));
        grade.add(card("Produtos", Permissao.GERENCIAR_PRODUTOS, () -> new ProdutoView(this).setVisible(true)));
        grade.add(card("Estoque", Permissao.GERENCIAR_ESTOQUE, () -> new EstoqueView(this).setVisible(true)));
        grade.add(card("Vendas", Permissao.CONSULTAR_VENDAS, () -> new VendasView(this).setVisible(true)));
        grade.add(card("Caixa (abertura / sangria / fechamento)", Permissao.GERENCIAR_CAIXA, () -> new CaixaView(this).setVisible(true)));
        grade.add(card("Fornecedores", Permissao.GERENCIAR_CONTAS, () -> new FornecedorView(this).setVisible(true)));
        grade.add(card("Contas a pagar", Permissao.GERENCIAR_CONTAS, () -> new ContaPagarView(this).setVisible(true)));
        grade.add(card("Relatórios", Permissao.VER_RELATORIOS, () -> new RelatorioView(this).setVisible(true)));
        grade.add(card("Usuários", Permissao.GERENCIAR_USUARIOS, () -> new UsuarioView(this).setVisible(true)));
        JButton sair = Ui.botao("Trocar usuário / Sair", this::trocarUsuario);
        sair.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        grade.add(sair);

        add(topo, BorderLayout.NORTH);
        add(grade, BorderLayout.CENTER);
        setSize(950, 520);
        setLocationRelativeTo(null);

        atualizarLembretes();
        javax.swing.Timer t = new javax.swing.Timer(15 * 60 * 1000, e -> atualizarLembretes()); // reavalia a cada 15 min
        t.start();
    }

    /** Chamado logo após o login: abre a prancheta se houver contas vencidas/próximas do vencimento. */
    public void mostrarLembretesInicial() {
        if (!lembretes.isEmpty()) SwingUtilities.invokeLater(this::mostrarLembretes);
    }

    private void mostrarLembretes() {
        lembretes = contas.lembretes();
        LembreteDialog.mostrar(this, lembretes, () -> new ContaPagarView(this).setVisible(true));
        atualizarLembretes();
    }

    private void atualizarLembretes() {
        try {
            lembretes = contas.lembretes();
        } catch (Exception ex) {
            lembretes = List.of();
        }
        boolean podeVer = Sessao.pode(Permissao.GERENCIAR_CONTAS);
        btnLembretes.setVisible(podeVer);
        if (!podeVer) return;
        long vencidas = lembretes.stream().filter(c -> c.situacao(java.time.LocalDate.now()) == ContaPagar.Situacao.VENCIDA).count();
        btnLembretes.setText(lembretes.isEmpty() ? "Lembretes: nenhum" : "Lembretes: " + lembretes.size() + " conta(s)"
                + (vencidas > 0 ? " - " + vencidas + " VENCIDA(S)" : ""));
        btnLembretes.setBackground(lembretes.isEmpty() ? null : vencidas > 0 ? new Color(0xFFCDD2) : new Color(0xFFF59D));
        btnLembretes.setOpaque(!lembretes.isEmpty());
    }

    private JButton card(String texto, Permissao exigida, Runnable acao) {
        JButton b = Ui.botao(texto, () -> { acao.run(); atualizarLembretes(); });
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        b.setEnabled(Sessao.pode(exigida));
        if (!b.isEnabled()) b.setToolTipText("Sem permissão: " + exigida.getDescricao());
        return b;
    }

    private void abrirPdv() {
        if (!CaixaView.garantirCaixaAberto(this)) return;
        setVisible(false);
        new PdvView(() -> { setVisible(true); atualizarLembretes(); }).setVisible(true);
    }

    private void trocarUsuario() {
        new AuthController().sair();
        dispose();
        new LoginView(new AuthController()).setVisible(true);
    }
}
