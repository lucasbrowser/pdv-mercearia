package com.pdv.view;

import com.pdv.controller.ContaPagarController;
import com.pdv.controller.FornecedorController;
import com.pdv.model.ContaPagar;
import com.pdv.model.Fornecedor;
import com.pdv.util.Fmt;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Formulário de conta a pagar (nova, com parcelamento, ou edição de conta em aberto). */
public class ContaPagarForm extends JDialog {
    private final ContaPagarController ctrl;
    private final ContaPagar existente;
    private final JComboBox<Fornecedor> fornecedor = new JComboBox<>();
    private final JTextField descricao = new JTextField(26), documento = new JTextField(),
            valor = new JTextField(), parcelas = new JTextField("1"),
            lembrete = new JTextField("3"), obs = new JTextField();
    private final DatePicker vencimento = new DatePicker(java.time.LocalDate.now());
    private boolean salvou;

    /** Retorna true se salvou. */
    public static boolean abrir(Window pai, ContaPagarController ctrl, ContaPagar existente) {
        ContaPagarForm f = new ContaPagarForm(pai, ctrl, existente);
        f.setVisible(true);
        return f.salvou;
    }

    private ContaPagarForm(Window pai, ContaPagarController ctrl, ContaPagar existente) {
        super(pai, existente == null ? "Nova conta a pagar" : "Editar conta #" + existente.id(), ModalityType.APPLICATION_MODAL);
        this.ctrl = ctrl;
        this.existente = existente;

        List<Fornecedor> forn = new ArrayList<>(new FornecedorController().pesquisar("", true));
        fornecedor.addItem(Fornecedor.SEM);
        forn.forEach(fornecedor::addItem);

        if (existente != null) {
            for (int i = 0; i < fornecedor.getItemCount(); i++)
                if (fornecedor.getItemAt(i).id() == existente.fornecedorId()) fornecedor.setSelectedIndex(i);
            descricao.setText(existente.descricao());
            documento.setText(existente.documento());
            valor.setText(Fmt.num(existente.valor()));
            vencimento.setText(existente.vencimento().format(Fmt.DATA));
            lembrete.setText(String.valueOf(existente.lembreteDias()));
            obs.setText(existente.observacao());
            parcelas.setEnabled(false);
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
        Ui.linha(form, 0, "Fornecedor:", fornecedor);
        Ui.linha(form, 1, "Descrição:", descricao);
        Ui.linha(form, 2, "Nº documento (NF/boleto):", documento);
        Ui.linha(form, 3, existente == null ? "Valor TOTAL (R$):" : "Valor (R$):", valor);
        Ui.linha(form, 4, existente == null ? "1º vencimento:" : "Vencimento:", vencimento);
        Ui.linha(form, 5, "Parcelas (mensais):", parcelas);
        Ui.linha(form, 6, "Avisar quantos dias antes:", lembrete);
        Ui.linha(form, 7, "Observação:", obs);

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Salvar", this::salvar));
        botoes.add(Ui.botao("Cancelar", this::dispose));

        add(form, BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);
        pack();
        setMinimumSize(new Dimension(520, 0));
        setLocationRelativeTo(pai);
    }

    private void salvar() {
        long fid = ((Fornecedor) fornecedor.getSelectedItem()).id();
        if (existente == null)
            ctrl.criar(fid, descricao.getText(), documento.getText(), valor.getText(), vencimento.getText(),
                    parcelas.getText(), lembrete.getText(), obs.getText());
        else
            ctrl.atualizar(existente.id(), fid, descricao.getText(), documento.getText(), valor.getText(),
                    vencimento.getText(), lembrete.getText(), obs.getText());
        salvou = true;
        dispose();
    }
}
