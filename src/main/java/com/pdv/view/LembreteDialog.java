package com.pdv.view;

import com.pdv.model.ContaPagar;
import com.pdv.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Aviso de contas a pagar em forma de prancheta. */
public class LembreteDialog extends JDialog {

    public static void mostrar(Window pai, List<ContaPagar> contas, Runnable abrirContas) {
        if (contas.isEmpty()) {
            Ui.info(pai, "Nenhum lembrete: não há contas vencidas ou próximas do vencimento.");
            return;
        }
        new LembreteDialog(pai, contas, abrirContas).setVisible(true);
    }

    private LembreteDialog(Window pai, List<ContaPagar> contas, Runnable abrirContas) {
        super(pai, "Lembretes - Contas a pagar", ModalityType.APPLICATION_MODAL);
        LocalDate hoje = LocalDate.now();

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        adicionar(lista, "VENCIDAS", contas, hoje, ContaPagar.Situacao.VENCIDA, new Color(0xC62828));
        adicionar(lista, "VENCEM HOJE", contas, hoje, ContaPagar.Situacao.VENCE_HOJE, new Color(0xE65100));
        adicionar(lista, "PRÓXIMOS VENCIMENTOS", contas, hoje, ContaPagar.Situacao.A_VENCER, new Color(0x37474F));

        BigDecimal total = contas.stream().map(ContaPagar::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        JLabel titulo = new JLabel("LEMBRETES - CONTAS A PAGAR");
        titulo.setFont(new Font(Font.SERIF, Font.BOLD, 20));
        JLabel soma = new JLabel(contas.size() + " conta(s)  |  Total: " + Fmt.moeda(total));
        soma.setFont(soma.getFont().deriveFont(Font.BOLD));

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.setOpaque(false);
        botoes.add(Ui.botao("Abrir contas a pagar", () -> { dispose(); abrirContas.run(); }));
        botoes.add(Ui.botao("Fechar", this::dispose));

        JPanel cabecalho = new JPanel(new GridLayout(0, 1));
        cabecalho.setOpaque(false);
        cabecalho.add(titulo);
        cabecalho.add(soma);

        JPanel papel = new JPanel(new BorderLayout(0, 8));
        papel.add(cabecalho, BorderLayout.NORTH);
        papel.add(scroll, BorderLayout.CENTER);
        papel.add(botoes, BorderLayout.SOUTH);

        setContentPane(new PranchetaPanel(papel));
        setSize(660, 560);
        setLocationRelativeTo(pai);
        Ui.atalho(getRootPane(), "ESCAPE", this::dispose);
    }

    private static void adicionar(JPanel lista, String titulo, List<ContaPagar> contas, LocalDate hoje,
                                  ContaPagar.Situacao sit, Color cor) {
        List<ContaPagar> grupo = contas.stream().filter(c -> c.situacao(hoje) == sit).toList();
        if (grupo.isEmpty()) return;
        JLabel t = new JLabel(titulo + " (" + grupo.size() + ")");
        t.setFont(new Font(Font.SERIF, Font.BOLD, 16));
        t.setForeground(cor);
        t.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        lista.add(t);
        for (ContaPagar c : grupo) {
            long d = c.diasParaVencer(hoje);
            String quando = d < 0 ? "atrasada " + (-d) + " dia(s)" : d == 0 ? "vence HOJE" : "vence em " + d + " dia(s)";
            String quem = c.fornecedorNome().isBlank() ? c.descricao() : c.fornecedorNome() + " - " + c.descricao();
            JLabel l = new JLabel("<html>[&nbsp;&nbsp;] " + c.vencimento().format(Fmt.DATA) + " &nbsp;<b>" + esc(quem) + "</b> &nbsp;"
                    + Fmt.moeda(c.valor()) + " &nbsp;<i>(" + quando + ")</i></html>");
            l.setForeground(cor);
            l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 0));
            lista.add(l);
        }
    }

    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); }
}
