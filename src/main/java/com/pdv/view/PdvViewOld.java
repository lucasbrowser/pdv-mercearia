package com.pdv.view;

import com.pdv.controller.VendaController;
import com.pdv.model.ItemVenda;
import com.pdv.model.Produto;
import com.pdv.model.Venda;
import com.pdv.util.Config;
import com.pdv.util.Fmt;
import com.pdv.util.ImpressoraCupom;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;

/**
 * Frente de caixa com interface gerenciada pelo NetBeans GUI Builder.
 * A lógica de vendas foi preservada a partir da versão original.
 */
public class PdvViewOld extends JFrame {
    private static final Color ESCURO = new Color(0x1E272E);
    private static final Color VERDE = new Color(0x00E676);
    private static final Color CINZA = new Color(0xB0BEC5);

    private final VendaController ctrl = new VendaController();
    private final Runnable aoFechar;
    private final ItemTableModel modelo = new ItemTableModel();
    private javax.swing.Timer relogioTimer;

    public PdvViewOld(Runnable aoFechar) {
        super("PDV - Frente de Caixa");
        this.aoFechar = aoFechar;
        initComponents();
        configurarTela();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1000, 650));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sair(); }
            @Override public void windowOpened(WindowEvent e) { campo.requestFocusInWindow(); }
        });
        registrarAtalhos();
        atualizar();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField campo;
    private javax.swing.JLabel lblAtalhos;
    private javax.swing.JLabel lblDesc;
    private javax.swing.JLabel lblMsg;
    private javax.swing.JLabel lblNomeMercado;
    private javax.swing.JLabel lblOperador;
    private javax.swing.JLabel lblRelogio;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblSub;
    private javax.swing.JLabel lblTituloDesc;
    private javax.swing.JLabel lblTituloSub;
    private javax.swing.JLabel lblTituloTotal;
    private javax.swing.JLabel lblTituloUltimo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JLabel lblTroco;
    private javax.swing.JLabel lblUltimo;
    private javax.swing.JLabel lblUltimoDet;
    private javax.swing.JPanel pnlLateral;
    private javax.swing.JPanel pnlOperador;
    private javax.swing.JPanel pnlRodape;
    private javax.swing.JPanel pnlTopo;
    private javax.swing.JScrollPane scrollItens;
    private javax.swing.JTable tabela;
    // End of variables declaration//GEN-END:variables

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlTopo = new javax.swing.JPanel();
        lblNomeMercado = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        pnlOperador = new javax.swing.JPanel();
        lblOperador = new javax.swing.JLabel();
        lblRelogio = new javax.swing.JLabel();
        scrollItens = new javax.swing.JScrollPane();
        tabela = new javax.swing.JTable();
        pnlLateral = new javax.swing.JPanel();
        lblTituloUltimo = new javax.swing.JLabel();
        lblUltimo = new javax.swing.JLabel();
        lblUltimoDet = new javax.swing.JLabel();
        lblTituloSub = new javax.swing.JLabel();
        lblSub = new javax.swing.JLabel();
        lblTituloDesc = new javax.swing.JLabel();
        lblDesc = new javax.swing.JLabel();
        lblTituloTotal = new javax.swing.JLabel();
        lblTotal = new javax.swing.JLabel();
        lblTroco = new javax.swing.JLabel();
        pnlRodape = new javax.swing.JPanel();
        lblMsg = new javax.swing.JLabel();
        campo = new javax.swing.JTextField();
        lblAtalhos = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1000, 650));
        setPreferredSize(new java.awt.Dimension(720, 360));

        pnlTopo.setLayout(new java.awt.BorderLayout());
        pnlTopo.add(lblNomeMercado, java.awt.BorderLayout.WEST);

        lblStatus.setText("CAIXA LIVRE");
        pnlTopo.add(lblStatus, java.awt.BorderLayout.CENTER);

        pnlOperador.add(lblOperador);

        lblRelogio.setText(" ");
        pnlOperador.add(lblRelogio);

        pnlTopo.add(pnlOperador, java.awt.BorderLayout.EAST);

        getContentPane().add(pnlTopo, java.awt.BorderLayout.NORTH);

        scrollItens.setViewportView(tabela);

        getContentPane().add(scrollItens, java.awt.BorderLayout.CENTER);

        pnlLateral.setLayout(new javax.swing.BoxLayout(pnlLateral, javax.swing.BoxLayout.Y_AXIS));

        lblTituloUltimo.setText("ÚLTIMO ITEM");
        pnlLateral.add(lblTituloUltimo);

        lblUltimo.setText(" ");
        pnlLateral.add(lblUltimo);

        lblUltimoDet.setText(" ");
        pnlLateral.add(lblUltimoDet);

        lblTituloSub.setText("SUBTOTAL");
        pnlLateral.add(lblTituloSub);

        lblSub.setText(" ");
        pnlLateral.add(lblSub);

        lblTituloDesc.setText("DESCONTO");
        pnlLateral.add(lblTituloDesc);

        lblDesc.setText(" ");
        pnlLateral.add(lblDesc);

        lblTituloTotal.setText("TOTAL A PAGAR");
        pnlLateral.add(lblTituloTotal);

        lblTotal.setText(" ");
        pnlLateral.add(lblTotal);

        lblTroco.setText(" ");
        pnlLateral.add(lblTroco);

        getContentPane().add(pnlLateral, java.awt.BorderLayout.EAST);

        pnlRodape.setLayout(new java.awt.BorderLayout());

        lblMsg.setText(" ");
        pnlRodape.add(lblMsg, java.awt.BorderLayout.NORTH);
        pnlRodape.add(campo, java.awt.BorderLayout.CENTER);

        lblAtalhos.setText("F2 Pesquisar   F3 Alterar qtd   F4 Remover item   F5 Desconto   F8 Cancelar venda   F10 Finalizar   ↑↓ Selecionar item   Esc Sair");
        pnlRodape.add(lblAtalhos, java.awt.BorderLayout.SOUTH);

        getContentPane().add(pnlRodape, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // </editor-fold>

    private void configurarTela() {
        setTitle("PDV - Frente de Caixa");
        pnlTopo.setBackground(ESCURO);
        pnlTopo.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        lblNomeMercado.setText(Config.get("mercado.nome"));
        lblNomeMercado.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        lblNomeMercado.setForeground(Color.WHITE);
        lblStatus.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblStatus.setForeground(VERDE);
        lblOperador.setText("Operador: " + Sessao.atual().nome() + "   |");
        lblOperador.setForeground(CINZA);
        lblRelogio.setForeground(CINZA);
        lblOperador.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        lblRelogio.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        tabela.setModel(modelo);
        tabela.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        tabela.setRowHeight(36);
        tabela.setFocusable(false);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        tabela.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer direita = new DefaultTableCellRenderer();
        direita.setHorizontalAlignment(SwingConstants.RIGHT);
        int[] larguras = {45, 150, 420, 70, 110, 120};
        for (int c = 0; c < larguras.length; c++) {
            tabela.getColumnModel().getColumn(c).setPreferredWidth(larguras[c]);
            if (c >= 3) tabela.getColumnModel().getColumn(c).setCellRenderer(direita);
        }
        scrollItens.setBorder(BorderFactory.createTitledBorder("Itens da venda"));

        pnlLateral.setBackground(ESCURO);
        pnlLateral.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        pnlLateral.setPreferredSize(new Dimension(380, 0));
        configurarLegenda(lblTituloUltimo);
        configurarLegenda(lblTituloSub);
        configurarLegenda(lblTituloDesc);
        configurarLegenda(lblTituloTotal);
        configurarValor(lblUltimo, 20, Color.WHITE);
        configurarValor(lblUltimoDet, 15, CINZA);
        configurarValor(lblSub, 22, Color.WHITE);
        configurarValor(lblDesc, 22, Color.WHITE);
        configurarValor(lblTotal, 46, VERDE);
        configurarValor(lblTroco, 22, new Color(0xFFD54F));

        lblMsg.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblMsg.setForeground(new Color(0xC62828));
        campo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
        campo.setBorder(BorderFactory.createTitledBorder(
                "Código de barras ou descrição  (Enter adiciona  •  3*código = 3 unidades)"));
        lblAtalhos.setForeground(Color.DARK_GRAY);
        pnlRodape.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        relogioTimer = new javax.swing.Timer(1000,
                e -> lblRelogio.setText(LocalDateTime.now().format(Fmt.DATA_HORA)));
        relogioTimer.setInitialDelay(0);
        relogioTimer.start();
        campo.addActionListener(e -> executar(this::processarEntrada));
    }

    private static void configurarValor(JLabel label, int tamanho, Color cor) {
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamanho));
        label.setForeground(cor);
        label.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private static void configurarLegenda(JLabel label) {
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        label.setForeground(CINZA);
    }

    private void registrarAtalhos() {
        JRootPane r = getRootPane();
        Ui.atalho(r, "F2", () -> executar(() -> pesquisar(campo.getText().trim())));
        Ui.atalho(r, "F3", () -> executar(this::alterarQuantidade));
        Ui.atalho(r, "F4", () -> executar(this::removerItem));
        Ui.atalho(r, "F5", () -> executar(this::aplicarDesconto));
        Ui.atalho(r, "F8", () -> executar(this::cancelarVenda));
        Ui.atalho(r, "F10", () -> executar(this::finalizar));
        Ui.atalho(r, "UP", () -> moverSelecao(-1));
        Ui.atalho(r, "DOWN", () -> moverSelecao(1));
        Ui.atalho(r, "ESCAPE", this::sair);
    }

    // ------------------------------------------------------------------ ações
    /** Executa uma ação do operador; erros de regra aparecem na faixa vermelha (sem travar o fluxo do caixa). */
    private void executar(Runnable acao) {
        try {
            msg(" ");
            acao.run();
        } catch (NegocioException ex) {
            Toolkit.getDefaultToolkit().beep();
            msg(ex.getMessage());
        } catch (Exception ex) {
            Ui.erro(this, ex);
        }
        atualizar();
        campo.requestFocusInWindow();
    }

    private void processarEntrada() {
        String txt = campo.getText().trim();
        campo.setText("");
        if (txt.isEmpty()) return;
        if (txt.matches("(\\d+\\*)?\\d+")) mostrarUltimo(ctrl.adicionarPorCodigo(txt)); // leitor / código digitado
        else pesquisar(txt);                                                              // texto: abre a pesquisa
    }

    private void pesquisar(String inicial) {
        campo.setText("");
        PesquisaProdutoDialog.Escolha e = PesquisaProdutoDialog.escolher(this, ctrl, inicial);
        if (e != null) mostrarUltimo(ctrl.adicionar(e.produto(), e.quantidade()));
    }

    private int indiceAlvo() {
        int n = modelo.getRowCount();
        if (n == 0) throw new NegocioException("Nenhum item na venda.");
        int i = tabela.getSelectedRow();
        return i < 0 ? n - 1 : i; // sem seleção = último item
    }

    private void alterarQuantidade() {
        int i = indiceAlvo();
        ItemVenda item = ctrl.getVenda().getItens().get(i);
        String s = JOptionPane.showInputDialog(this, "Nova quantidade para " + item.getDescricao() + ":",
                String.valueOf(item.getQuantidade()));
//        if (s != null) ctrl.alterarQuantidade(i, Fmt.inteiro(s, "quantidade"));
        if (s != null) ctrl.alterarQuantidade(i, Fmt.quantidade(s, "quantidade", Produto.fracionada(item.getUnidade())));

    }

    private void removerItem() {
        int i = indiceAlvo();
        String desc = ctrl.getVenda().getItens().get(i).getDescricao();
        ctrl.removerItem(i);
        msg("Item removido: " + desc);
    }

    private void aplicarDesconto() {
        if (modelo.getRowCount() == 0) throw new NegocioException("Nenhum item na venda.");
        String s = JOptionPane.showInputDialog(this, "Desconto (valor em R$ ou percentual. Ex.: 5,00  ou  10%):");
        if (s != null && !s.isBlank()) ctrl.aplicarDesconto(s);
    }

    private void cancelarVenda() {
        if (modelo.getRowCount() == 0) throw new NegocioException("Nenhum item na venda.");
        if (Ui.confirmar(this, "Cancelar TODA a venda em andamento?")) {
            ctrl.cancelarVenda();
            lblUltimo.setText(" ");
            lblUltimoDet.setText(" ");
        }
    }

    private void finalizar() {
        if (modelo.getRowCount() == 0) throw new NegocioException("Nenhum item na venda.");
        String[] pg = PagamentoDialog.solicitar(this, ctrl.getVenda().getTotal());
        if (pg == null) return;
        Venda v = ctrl.finalizar(pg[0], pg[1]);
        lblUltimo.setText(" ");
        lblUltimoDet.setText(" ");
        lblTroco.setText("<html>Venda #" + v.getId() + " concluída<br>Troco: " + Fmt.moeda(v.getTroco()) + "</html>");
        try {
            ImpressoraCupom.imprimir(v);
        } catch (NegocioException ex) {
            Ui.erro(this, new NegocioException("Venda #" + v.getId() + " registrada, mas o cupom NÃO foi impresso:\n"
                    + ex.getMessage() + "\n\nReimprima em Menu > Vendas > Reimprimir."));
        }
    }

    private void moverSelecao(int d) {
        int n = modelo.getRowCount();
        if (n == 0) return;
        int i = tabela.getSelectedRow();
        i = i < 0 ? n - 1 : Math.max(0, Math.min(n - 1, i + d));
        tabela.setRowSelectionInterval(i, i);
        tabela.scrollRectToVisible(tabela.getCellRect(i, 0, true));
    }

    private void sair() {
        if (modelo.getRowCount() > 0) {
            msg("Há uma venda em andamento. Finalize (F10) ou cancele (F8) antes de sair.");
            return;
        }
        dispose();
        aoFechar.run();
    }

    // ------------------------------------------------------------------ exibição
    private void mostrarUltimo(ItemVenda i) {
        lblUltimo.setText("<html><body style='width:370px'>" + i.getDescricao() + "</body></html>");
        lblUltimoDet.setText(i.getQuantidade() + " x " + Fmt.moeda(i.getPreco()) + " = " + Fmt.moeda(i.getSubtotal()));
        lblTroco.setText(" ");
    }

    private void msg(String s) { lblMsg.setText(s.isBlank() ? " " : s); }

    private void atualizar() {
        modelo.fireTableDataChanged();
        Venda v = ctrl.getVenda();
        lblSub.setText(Fmt.moeda(v.getSubtotal()));
        lblDesc.setText("- " + Fmt.moeda(v.getDesconto()));
        lblTotal.setText(Fmt.moeda(v.getTotal()));
        boolean emAndamento = !v.getItens().isEmpty();
        lblStatus.setText(emAndamento ? "VENDA EM ANDAMENTO" : "CAIXA LIVRE");
        lblStatus.setForeground(emAndamento ? new Color(0xFFD54F) : VERDE);
        if (emAndamento) tabela.scrollRectToVisible(tabela.getCellRect(v.getItens().size() - 1, 0, true));
    }

    private class ItemTableModel extends AbstractTableModel {
        private final String[] colunas = {"#", "Código", "Descrição", "Qtd", "Unitário", "Subtotal"};

        @Override public int getRowCount() { return ctrl.getVenda().getItens().size(); }
        @Override public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }

        @Override public Object getValueAt(int r, int c) {
            ItemVenda i = ctrl.getVenda().getItens().get(r);
            return switch (c) {
                case 0 -> r + 1;
                case 1 -> i.getCodigoBarras();
                case 2 -> i.getDescricao();
                case 3 -> i.getQuantidade();
                case 4 -> Fmt.num(i.getPreco());
                default -> Fmt.num(i.getSubtotal());
            };
        }
    }
}
