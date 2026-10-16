package com.pdv.view;

import com.pdv.controller.HistoricoVendasController;
import com.pdv.model.Venda;
import com.pdv.util.Fmt;
import com.pdv.util.ImpressoraCupom;
import com.pdv.util.NegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class VendasView extends JDialog {
    private final HistoricoVendasController ctrl = new HistoricoVendasController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Nº", "Data/Hora", "Operador", "Pagamento", "Total", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = Ui.tabela(modelo);
    private final DatePicker de = new DatePicker(LocalDate.now());
    private final DatePicker ate = new DatePicker(LocalDate.now());
    private final JLabel resumo = new JLabel(" ");
    private List<Venda> vendas = List.of();

    public VendasView(Window pai) {
        super(pai, "Histórico de Vendas", ModalityType.APPLICATION_MODAL);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("De:")); topo.add(de);
        topo.add(new JLabel("Até:")); topo.add(ate);
        topo.add(Ui.botao("Filtrar", this::filtrar));
        resumo.setFont(resumo.getFont().deriveFont(Font.BOLD));
        topo.add(resumo);

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Ver cupom", () -> {
            JTextArea area = new JTextArea(ImpressoraCupom.montarTexto(ctrl.buscar(selecionada().getId())));
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Cupom", JOptionPane.PLAIN_MESSAGE);
        }));
        botoes.add(Ui.botao("Reimprimir", () -> {
            ImpressoraCupom.imprimir(ctrl.buscar(selecionada().getId()));
            Ui.info(this, "Cupom enviado para a impressora.");
        }));
        botoes.add(Ui.botao("Cancelar venda selecionada", () -> {
            Venda v = selecionada();
            if (Ui.confirmar(this, "Cancelar a venda #" + v.getId() + "? Os itens voltarão ao estoque.")) {
                ctrl.cancelar(v.getId());
                filtrar();
            }
        }));

        botoes.add(Ui.cancelar(this));

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);
        setSize(950, 540);
        setLocationRelativeTo(pai);
        filtrar();
    }

    private Venda selecionada() {
        int r = tabela.getSelectedRow();
        if (r < 0) throw new NegocioException("Selecione uma venda.");
        return vendas.get(r);
    }

    private void filtrar() {
        try {
            vendas = ctrl.listar(LocalDate.parse(de.getText().trim(), Fmt.DATA), LocalDate.parse(ate.getText().trim(), Fmt.DATA));
        } catch (DateTimeParseException e) {
            throw new NegocioException("Data inválida. Use o formato dd/MM/aaaa.");
        }
        modelo.setRowCount(0);
        BigDecimal soma = BigDecimal.ZERO;
        int n = 0;
        for (Venda v : vendas) {
            boolean ok = "CONCLUIDA".equals(v.getStatus());
            if (ok) { soma = soma.add(v.getTotal()); n++; }
            modelo.addRow(new Object[]{v.getId(), v.getDataHora().format(Fmt.DATA_HORA), v.getUsuarioNome(),
                    v.getFormaPagamento(), Fmt.moeda(v.getTotal()), ok ? "Concluída" : "Cancelada"});
        }
        resumo.setText("   " + n + " venda(s) concluída(s)  |  Total: " + Fmt.moeda(soma));
    }
}
