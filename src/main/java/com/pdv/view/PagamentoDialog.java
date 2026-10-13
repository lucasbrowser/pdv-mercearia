package com.pdv.view;

import com.pdv.model.FormaPagamento;
import com.pdv.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/** Retorna {forma, valorRecebido} ou null se cancelado. */
public class PagamentoDialog extends JDialog {
    private final BigDecimal total;
    private final JComboBox<String> forma = new JComboBox<>(FormaPagamento.TODAS.toArray(new String[0]));
    private final JTextField recebido = new JTextField();
    private final JLabel troco = new JLabel("R$ 0,00");
    private String[] resultado;

    public static String[] solicitar(Window pai, BigDecimal total) {
        PagamentoDialog d = new PagamentoDialog(pai, total);
        d.setVisible(true);
        return d.resultado;
    }

    private PagamentoDialog(Window pai, BigDecimal total) {
        super(pai, "Pagamento", ModalityType.APPLICATION_MODAL);
        this.total = total;

        JLabel lblTotal = new JLabel(Fmt.moeda(total), SwingConstants.CENTER);
        lblTotal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        forma.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        recebido.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        troco.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));

        JPanel form = new JPanel(new GridBagLayout());
        Ui.linha(form, 0, "Forma:", forma);
        Ui.linha(form, 1, "Recebido (R$):", recebido);
        Ui.linha(form, 2, "Troco:", troco);

        JButton ok = Ui.botao("Confirmar (Enter)", this::confirmar);
        JPanel botoes = new JPanel();
        botoes.add(ok);
        botoes.add(Ui.botao("Voltar (Esc)", this::dispose));

        forma.addActionListener(e -> atualizar());
        Ui.aoDigitar(recebido, this::atualizar);
        recebido.addActionListener(e -> ok.doClick());
        Ui.atalho(getRootPane(), "ESCAPE", this::dispose);

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        raiz.add(lblTotal, BorderLayout.NORTH);
        raiz.add(form, BorderLayout.CENTER);
        raiz.add(botoes, BorderLayout.SOUTH);
        setContentPane(raiz);
        pack();
        setMinimumSize(new Dimension(460, 0));
        setLocationRelativeTo(pai);
        atualizar();
        SwingUtilities.invokeLater(recebido::requestFocusInWindow);
    }

    private boolean dinheiro() { return forma.getSelectedIndex() == 0; }

    private void atualizar() {
        recebido.setEnabled(dinheiro());
        if (!dinheiro()) { troco.setText(Fmt.moeda(BigDecimal.ZERO)); troco.setForeground(Color.DARK_GRAY); return; }
        try {
            BigDecimal r = recebido.getText().isBlank() ? total : Fmt.parse(recebido.getText());
            boolean falta = r.compareTo(total) < 0;
            troco.setText(falta ? "Faltam " + Fmt.moeda(total.subtract(r)) : Fmt.moeda(r.subtract(total)));
            troco.setForeground(falta ? new Color(0xC62828) : new Color(0x2E7D32));
        } catch (RuntimeException e) {
            troco.setText("—");
        }
    }

    private void confirmar() {
        if (dinheiro() && !recebido.getText().isBlank()) Fmt.parse(recebido.getText()); // valida o formato
        resultado = new String[]{(String) forma.getSelectedItem(), dinheiro() ? recebido.getText() : ""};
        dispose();
    }
}
