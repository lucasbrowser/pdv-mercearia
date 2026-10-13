package com.pdv.view;

import com.pdv.controller.ContaPagarController;
import com.pdv.controller.PesoObrigatorioException;
import com.pdv.controller.VendaController;
import com.pdv.model.ItemVenda;
import com.pdv.model.Produto;
import com.pdv.model.TipoMovCaixa;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Frente de caixa. O foco fica SEMPRE no campo de código: o leitor de código de barras
 * "digita" o código e envia Enter, então basta bipar. Texto (não numérico) abre a pesquisa.
 */
public class PdvView extends JFrame {
    private static final Color ESCURO = new Color(0x1E272E);
    private static final Color VERDE = new Color(0x00E676);
    private static final Color CINZA = new Color(0xB0BEC5);

    private final VendaController ctrl = new VendaController();
    private final Runnable aoFechar;
    private final ItemTableModel modelo = new ItemTableModel();
    private final JTable tabela = Ui.tabela(modelo);
    private final JTextField campo = new JTextField();
    private final JLabel lblStatus = new JLabel("CAIXA LIVRE", SwingConstants.CENTER);
    private final JLabel lblUltimo = valor(20, Color.WHITE);
    private final JLabel lblUltimoDet = valor(15, CINZA);
    private final JLabel lblSub = valor(22, Color.WHITE);
    private final JLabel lblDesc = valor(22, Color.WHITE);
    private final JLabel lblTotal = valor(46, VERDE);
    private final JLabel lblTroco = valor(22, new Color(0xFFD54F));
    private final JLabel lblMsg = new JLabel(" ");

    public PdvView(Runnable aoFechar) {
        super("PDV - Frente de Caixa");
        this.aoFechar = aoFechar;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setExtendedState(MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1000, 650));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sair(); }
            @Override public void windowOpened(WindowEvent e) { campo.requestFocusInWindow(); }
        });
        montarTela();
        registrarAtalhos();
        atualizar();
    }

    // ------------------------------------------------------------------ layout
    private static JLabel valor(int tamanho, Color cor) {
        JLabel l = new JLabel(" ");
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamanho));
        l.setForeground(cor);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel legenda(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        l.setForeground(CINZA);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void montarTela() {
        // cabeçalho
        JLabel nome = new JLabel(Config.get("mercado.nome"));
        nome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        nome.setForeground(Color.WHITE);
        lblStatus.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblStatus.setForeground(VERDE);
        JLabel oper = new JLabel("Caixa #" + ctrl.caixaAberto().map(c -> String.valueOf(c.id())).orElse("-")
                + "   |   Operador: " + Sessao.atual().nome() + "   |");
        JLabel relogio = new JLabel(" ");
        oper.setForeground(CINZA);
        relogio.setForeground(CINZA);
        oper.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        relogio.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        JPanel dir = new JPanel();
        dir.setOpaque(false);
        int nContas = new ContaPagarController().lembretes().size();
        if (nContas > 0) {
            JLabel aviso = new JLabel("Contas a pagar: " + nContas + "  |");
            aviso.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            aviso.setForeground(new Color(0xFFD54F));
            dir.add(aviso);
        }
        dir.add(oper);
        dir.add(relogio);
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(ESCURO);
        topo.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        topo.add(nome, BorderLayout.WEST);
        topo.add(lblStatus, BorderLayout.CENTER);
        topo.add(dir, BorderLayout.EAST);
        javax.swing.Timer relogioTimer = new javax.swing.Timer(1000,
                e -> relogio.setText(LocalDateTime.now().format(Fmt.DATA_HORA)));
        relogioTimer.setInitialDelay(0);
        relogioTimer.start();

        // tabela de itens
        tabela.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        tabela.setRowHeight(36);
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        tabela.setFocusable(false); // mantém o foco no campo do leitor
        DefaultTableCellRenderer direita = new DefaultTableCellRenderer();
        direita.setHorizontalAlignment(SwingConstants.RIGHT);
        int[] larguras = {45, 150, 400, 100, 110, 120};
        for (int c = 0; c < larguras.length; c++) {
            tabela.getColumnModel().getColumn(c).setPreferredWidth(larguras[c]);
            if (c >= 3) tabela.getColumnModel().getColumn(c).setCellRenderer(direita);
        }
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createTitledBorder("Itens da venda"));

        // painel lateral
        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBackground(ESCURO);
        lateral.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        lateral.setPreferredSize(new Dimension(420, 0));
        lateral.add(legenda("ÚLTIMO ITEM"));
        lateral.add(lblUltimo);
        lateral.add(lblUltimoDet);
        lateral.add(Box.createVerticalStrut(28));
        lateral.add(legenda("SUBTOTAL"));
        lateral.add(lblSub);
        lateral.add(Box.createVerticalStrut(8));
        lateral.add(legenda("DESCONTO"));
        lateral.add(lblDesc);
        lateral.add(Box.createVerticalStrut(18));
        lateral.add(legenda("TOTAL A PAGAR"));
        lateral.add(lblTotal);
        lateral.add(Box.createVerticalGlue());
        lateral.add(lblTroco);

        // rodapé: campo do leitor
        lblMsg.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblMsg.setForeground(new Color(0xC62828));
        campo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
        campo.setBorder(BorderFactory.createTitledBorder(
                "Código de barras ou descrição  (Enter adiciona  •  3*código = 3 un.  •  0,350*código = peso)"));
        JLabel atalhos = new JLabel("F2 Pesquisar   F3 Alterar qtd   F4 Remover item   F5 Desconto   "
                + "F6 Sangria   F8 Cancelar venda   F10 Finalizar   ↑↓ Selecionar item   Esc Sair");
        atalhos.setForeground(Color.DARK_GRAY);
        JPanel baixo = new JPanel(new BorderLayout(0, 4));
        baixo.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));
        baixo.add(lblMsg, BorderLayout.NORTH);
        baixo.add(campo, BorderLayout.CENTER);
        baixo.add(atalhos, BorderLayout.SOUTH);

        add(topo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(lateral, BorderLayout.EAST);
        add(baixo, BorderLayout.SOUTH);

        campo.addActionListener(e -> executar(this::processarEntrada));
    }

    private void registrarAtalhos() {
        JRootPane r = getRootPane();
        Ui.atalho(r, "F2", () -> executar(() -> pesquisar(campo.getText().trim())));
        Ui.atalho(r, "F3", () -> executar(this::alterarQuantidade));
        Ui.atalho(r, "F4", () -> executar(this::removerItem));
        Ui.atalho(r, "F5", () -> executar(this::aplicarDesconto));
        Ui.atalho(r, "F6", () -> executar(() -> CaixaView.movimento(this, TipoMovCaixa.SANGRIA)));
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
        if (txt.matches("((\\d+([.,]\\d+)?)\\*)?\\d+")) { // leitor / código digitado (aceita 0,350*codigo)
            try {
                mostrarUltimo(ctrl.adicionarPorCodigo(txt));
            } catch (PesoObrigatorioException ex) {                // produto por peso: pede o peso na balança
                pedirPeso(ex.getProduto());
            }
        } else pesquisar(txt);                                      // texto: abre a pesquisa
    }

    private void pesquisar(String inicial) {
        campo.setText("");
        PesquisaProdutoDialog.Escolha e = PesquisaProdutoDialog.escolher(this, ctrl, inicial);
        if (e != null) mostrarUltimo(ctrl.adicionar(e.produto(), e.quantidade()));
    }

    private void pedirPeso(Produto p) {
        String s = JOptionPane.showInputDialog(this, p.descricao() + "  (" + Fmt.moeda(p.preco()) + " por " + p.unidade()
                + ")\nInforme o peso/volume lido na balança (ex.: 0,350):", "Venda por peso", JOptionPane.QUESTION_MESSAGE);
        if (s != null && !s.isBlank()) mostrarUltimo(ctrl.adicionar(p, Fmt.quantidade(s, "peso", true)));
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
                Fmt.qtd(item.getQuantidade()));
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
        lblUltimoDet.setText(Fmt.qtd(i.getQuantidade()) + " " + i.getUnidade() + " x " + Fmt.moeda(i.getPreco()) + " = " + Fmt.moeda(i.getSubtotal()));
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
                case 3 -> Fmt.qtd(i.getQuantidade()) + " " + i.getUnidade();
                case 4 -> Fmt.num(i.getPreco());
                default -> Fmt.num(i.getSubtotal());
            };
        }
    }
}
