package com.pdv.view;

import com.pdv.util.NegocioException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;

/** Utilitários comuns de interface. */
public final class Ui {
    private Ui() { }

    public static void erro(Component pai, Throwable t) {
        boolean negocio = t instanceof NegocioException;
        if (!negocio) t.printStackTrace();
        JOptionPane.showMessageDialog(pai, negocio ? t.getMessage() : "Erro inesperado: " + t,
                negocio ? "Atenção" : "Erro", negocio ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component pai, String msg) {
        JOptionPane.showMessageDialog(pai, msg, "Informação", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirmar(Component pai, String msg) {
        return JOptionPane.showConfirmDialog(pai, msg, "Confirmação", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Botão cujo clique executa a ação tratando qualquer exceção com um diálogo. */
    public static JButton botao(String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try { acao.run(); } catch (Exception ex) { erro(b, ex); }
        });
        return b;
    }

    /** Botão com ícone e tecla de atalho Alt+letra (mnemônico). */
    public static JButton botao(String texto, String icone, char mnemonico, Runnable acao) {
        JButton b = botao(texto, acao);
        if (icone != null) {
            b.setIcon(Icones.get(icone, 20));
            b.setIconTextGap(8);
        }
        if (mnemonico != 0) {
            b.setMnemonic(mnemonico);
            b.setToolTipText(texto + "  (Alt+" + Character.toUpperCase(mnemonico) + ")");
        }
        return b;
    }

    /** Deixa todos os botões diretos do painel com o mesmo tamanho (o maior entre eles). */
    public static void igualar(Container painel) {
        int w = 0, h = 0;
        for (Component c : painel.getComponents())
            if (c instanceof JButton) {
                Dimension d = c.getPreferredSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
        for (Component c : painel.getComponents())
            if (c instanceof JButton) c.setPreferredSize(new Dimension(w, h));
    }

    public static void atalho(JComponent c, String tecla, Runnable acao) {
        String nome = "atalho-" + tecla;
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla), nome);
        c.getActionMap().put(nome, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                try { acao.run(); } catch (Exception ex) { erro(c, ex); }
            }
        });
    }

    /**
     * Botão "Cancelar": fecha a tela atual e devolve o controle à tela que a abriu (janela proprietária).
     * Também associa a tecla ESC à mesma ação.
     */
    public static JButton cancelar(JDialog d) { return cancelar(d, () -> true); }

    /** Igual a {@link #cancelar(JDialog)}, mas só fecha se {@code podeFechar} permitir (ex.: confirmar perda de dados). */
    public static JButton cancelar(JDialog d, java.util.function.BooleanSupplier podeFechar) {
        Runnable fechar = () -> { if (podeFechar.getAsBoolean()) d.dispose(); };
        JButton b = new JButton("Cancelar", Icones.get("cancelar", 20));
        b.setIconTextGap(8);
        b.setMnemonic('C');
        b.setToolTipText("Fecha esta tela e retorna à tela anterior (Esc / Alt+C)");
        b.addActionListener(e -> fechar.run());
        atalho(d.getRootPane(), "ESCAPE", fechar);
        d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // o X da janela também passa pela confirmação
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { fechar.run(); }
        });
        return b;
    }

    public static JTable tabela(TableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    public static void linha(JPanel p, int y, String rotulo, Component c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = y;
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        p.add(new JLabel(rotulo), g);
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        p.add(c, g);
    }

    public static void aoDigitar(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }
}
