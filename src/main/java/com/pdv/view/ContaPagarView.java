package com.pdv.view;

import com.pdv.controller.ContaPagarController;
import com.pdv.controller.ContaPagarController.Filtro;
import com.pdv.model.Caixa;
import com.pdv.model.ContaPagar;
import com.pdv.model.TipoMovCaixa;
import com.pdv.util.Fmt;
import com.pdv.util.ImpressoraCupom;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ContaPagarView extends JDialog {
    private final ContaPagarController ctrl = new ContaPagarController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Vencimento", "Fornecedor", "Descrição", "Documento", "Valor", "Situação", "Pago em", "Forma"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = Ui.tabela(modelo);
    private final JComboBox<Filtro> filtro = new JComboBox<>(Filtro.values());
    private final DatePicker de = new DatePicker(LocalDate.now().minusMonths(3));
    private final DatePicker ate = new DatePicker(LocalDate.now().plusMonths(3));
    private final JLabel resumo = new JLabel(" ");
    private List<ContaPagar> contas = List.of();

    public ContaPagarView(Window pai) {
        super(pai, "Contas a Pagar", ModalityType.APPLICATION_MODAL);

        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foco, r, c);
                if (!sel && r < contas.size()) {
                    comp.setForeground(switch (contas.get(r).situacao(LocalDate.now())) {
                        case VENCIDA -> new Color(0xC62828);
                        case VENCE_HOJE -> new Color(0xE65100);
                        case PAGA -> new Color(0x2E7D32);
                        case CANCELADA -> Color.GRAY;
                        case A_VENCER -> t.getForeground();
                    });
                }
                return comp;
            }
        });

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Mostrar:"));
        topo.add(filtro);
        topo.add(new JLabel("Vencimento de:"));
        topo.add(de);
        topo.add(new JLabel("até:"));
        topo.add(ate);
        topo.add(Ui.botao("Filtrar", this::carregar));

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Nova conta", () -> { if (ContaPagarForm.abrir(this, ctrl, null)) carregar(); }));
        botoes.add(Ui.botao("Editar", () -> { if (ContaPagarForm.abrir(this, ctrl, selecionada())) carregar(); }));
        botoes.add(Ui.botao("Pagar", this::pagar));
        botoes.add(Ui.botao("Cancelar conta selecionada", () -> {
            ContaPagar c = selecionada();
            if (Ui.confirmar(this, "Cancelar a conta \"" + c.descricao() + "\"?")) { ctrl.cancelar(c.id()); carregar(); }
        }));
        botoes.add(Ui.botao("Fornecedores", () -> new FornecedorView(this).setVisible(true)));

        botoes.add(Ui.cancelar(this));

        resumo.setFont(resumo.getFont().deriveFont(Font.BOLD));
        resumo.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        JPanel sul = new JPanel(new BorderLayout());
        sul.add(resumo, BorderLayout.NORTH);
        sul.add(botoes, BorderLayout.CENTER);

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(sul, BorderLayout.SOUTH);
        setSize(1150, 600);
        setLocationRelativeTo(pai);
        filtro.setSelectedItem(Filtro.ABERTAS);
        carregar();
    }

    private ContaPagar selecionada() {
        int r = tabela.getSelectedRow();
        if (r < 0) throw new NegocioException("Selecione uma conta.");
        return contas.get(r);
    }

    private void carregar() {
        contas = ctrl.listar(Fmt.data(de.getText()), Fmt.data(ate.getText()), (Filtro) filtro.getSelectedItem());
        modelo.setRowCount(0);
        LocalDate hoje = LocalDate.now();
        BigDecimal aberto = BigDecimal.ZERO, vencido = BigDecimal.ZERO, pago = BigDecimal.ZERO;
        for (ContaPagar c : contas) {
            ContaPagar.Situacao s = c.situacao(hoje);
            if (c.aberta()) {
                aberto = aberto.add(c.valor());
                if (s == ContaPagar.Situacao.VENCIDA) vencido = vencido.add(c.valor());
            } else if (s == ContaPagar.Situacao.PAGA) pago = pago.add(c.valorPago());
            modelo.addRow(new Object[]{c.vencimento().format(Fmt.DATA), c.fornecedorNome(), c.descricao(), c.documento(),
                    Fmt.moeda(c.valor()), s.getRotulo(), c.dataPagamento() == null ? "" : c.dataPagamento().format(Fmt.DATA_HORA),
                    c.formaPagamento()});
        }
        resumo.setText("Em aberto: " + Fmt.moeda(aberto) + "   |   Vencido: " + Fmt.moeda(vencido) + "   |   Pago: " + Fmt.moeda(pago));
    }

    private void pagar() {
        ContaPagar c = selecionada();
        if (!c.aberta()) throw new NegocioException("Esta conta não está em aberto.");
        JComboBox<String> forma = new JComboBox<>(ContaPagar.FORMAS.toArray(new String[0]));
        JTextField valor = new JTextField(Fmt.num(c.valor()), 12);
        JTextField obs = new JTextField(24);
        Optional<BigDecimal> gaveta = ctrl.dinheiroNoCaixa();
        JLabel info = new JLabel(gaveta.map(v -> "Dinheiro disponível na gaveta: " + Fmt.moeda(v))
                .orElse("Caixa fechado: pagamento em dinheiro do caixa indisponível."));
        info.setForeground(gaveta.isPresent() ? new Color(0x2E7D32) : new Color(0xC62828));
        Object[] msg = {"Conta: " + c.descricao() + (c.fornecedorNome().isBlank() ? "" : " - " + c.fornecedorNome()),
                "Vencimento: " + c.vencimento().format(Fmt.DATA) + "   Valor: " + Fmt.moeda(c.valor()),
                "Forma de pagamento:", forma, info, "Valor pago (R$):", valor, "Observação (opcional):", obs};
        if (JOptionPane.showConfirmDialog(this, msg, "Pagar conta", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String f = (String) forma.getSelectedItem();
        Optional<Caixa> caixa = ctrl.pagar(c.id(), valor.getText(), f, obs.getText());
        caixa.ifPresent(cx -> {
            try {
                ImpressoraCupom.imprimirTexto(ImpressoraCupom.montarTextoMovimento(cx, TipoMovCaixa.PAGAMENTO_CONTA,
                        valor.getText().trim(), c.descricao(), Sessao.atual().nome()));
            } catch (NegocioException ex) {
                Ui.erro(this, new NegocioException("Pagamento registrado, mas o comprovante não foi impresso: " + ex.getMessage()));
            }
        });
        Ui.info(this, caixa.isPresent() ? "Conta paga. A saída foi lançada no caixa #" + caixa.get().id() + "." : "Conta paga.");
        carregar();
    }
}
