package com.pdv.view;

import com.pdv.controller.VendaController;
import com.pdv.model.Produto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Pesquisa de produto por nome/código no PDV (F2). */
public class PesquisaProdutoDialog extends JDialog {
    public record Escolha(Produto produto, int quantidade) { }

    private final VendaController ctrl;
    private final ProdutoTableModel modelo = new ProdutoTableModel();
    private final JTable tabela = Ui.tabela(modelo);
    private final JTextField busca = new JTextField();
    private final JSpinner qtd = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private Escolha escolha;

    public static Escolha escolher(Window pai, VendaController ctrl, String inicial) {
        PesquisaProdutoDialog d = new PesquisaProdutoDialog(pai, ctrl, inicial);
        d.setVisible(true);
        return d.escolha;
    }

    private PesquisaProdutoDialog(Window pai, VendaController ctrl, String inicial) {
        super(pai, "Pesquisar produto", ModalityType.APPLICATION_MODAL);
        this.ctrl = ctrl;
        ProdutoTableModel.aplicarCores(tabela, modelo);
        tabela.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        tabela.setRowHeight(30);
        busca.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        busca.setText(inicial);

        Ui.aoDigitar(busca, this::carregar);
        busca.addActionListener(e -> confirmar());
        tabela.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) confirmar(); }
        });
        Ui.atalho(getRootPane(), "ESCAPE", this::dispose);
        Ui.atalho(getRootPane(), "DOWN", () -> mover(1));
        Ui.atalho(getRootPane(), "UP", () -> mover(-1));

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(new JLabel("Quantidade:"));
        rodape.add(qtd);
        rodape.add(Ui.botao("Adicionar (Enter)", this::confirmar));
        rodape.add(Ui.botao("Cancelar (Esc)", this::dispose));

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        raiz.add(busca, BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabela), BorderLayout.CENTER);
        raiz.add(rodape, BorderLayout.SOUTH);
        setContentPane(raiz);
        setSize(860, 520);
        setLocationRelativeTo(pai);
        carregar();
        SwingUtilities.invokeLater(busca::requestFocusInWindow);
    }

    private void carregar() {
        modelo.setDados(ctrl.pesquisarProdutos(busca.getText()));
        if (modelo.getRowCount() > 0) tabela.setRowSelectionInterval(0, 0);
    }

    private void mover(int d) {
        int n = modelo.getRowCount();
        if (n == 0) return;
        int i = Math.max(0, Math.min(n - 1, tabela.getSelectedRow() + d));
        tabela.setRowSelectionInterval(i, i);
        tabela.scrollRectToVisible(tabela.getCellRect(i, 0, true));
    }

    private void confirmar() {
        int r = tabela.getSelectedRow();
        if (r < 0 && modelo.getRowCount() > 0) r = 0;
        if (r < 0) return;
        escolha = new Escolha(modelo.get(r), (Integer) qtd.getValue());
        dispose();
    }
}
