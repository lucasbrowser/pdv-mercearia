package com.pdv.view;

import com.pdv.controller.FornecedorController;
import com.pdv.model.Fornecedor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class FornecedorView extends JDialog {
    private final FornecedorController ctrl = new FornecedorController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Nome", "CNPJ/CPF", "Contato", "Telefone", "E-mail", "Ativo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = Ui.tabela(modelo);
    private final JTextField busca = new JTextField(22);
    private final JTextField nome = new JTextField(), documento = new JTextField(), contato = new JTextField(),
            telefone = new JTextField(), email = new JTextField(), endereco = new JTextField(), obs = new JTextField();
    private final JCheckBox ativo = new JCheckBox("Fornecedor ativo", true);
    private List<Fornecedor> lista = List.of();
    private long idAtual = 0;

    public FornecedorView(Window pai) {
        super(pai, "Cadastro de Fornecedores", ModalityType.APPLICATION_MODAL);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Buscar (nome, documento, contato):"));
        topo.add(busca);
        Ui.aoDigitar(busca, this::carregar);

        JPanel esquerda = new JPanel(new BorderLayout());
        esquerda.add(topo, BorderLayout.NORTH);
        esquerda.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Dados do fornecedor"));
        Ui.linha(form, 0, "Nome / Razão social:", nome);
        Ui.linha(form, 1, "CNPJ/CPF:", documento);
        Ui.linha(form, 2, "Contato:", contato);
        Ui.linha(form, 3, "Telefone:", telefone);
        Ui.linha(form, 4, "E-mail:", email);
        Ui.linha(form, 5, "Endereço:", endereco);
        Ui.linha(form, 6, "Observação:", obs);
        Ui.linha(form, 7, "", ativo);

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Novo", this::novo));
        botoes.add(Ui.botao("Salvar", this::salvar));
        botoes.add(Ui.cancelar(this));

        JPanel direita = new JPanel(new BorderLayout());
        direita.setPreferredSize(new Dimension(420, 0));
        direita.add(form, BorderLayout.NORTH);
        direita.add(botoes, BorderLayout.CENTER);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabela.getSelectedRow() >= 0) preencher(lista.get(tabela.getSelectedRow()));
        });

        add(esquerda, BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setSize(1150, 520);
        setLocationRelativeTo(pai);
        carregar();
    }

    private void carregar() {
        lista = ctrl.pesquisar(busca.getText(), false);
        modelo.setRowCount(0);
        for (Fornecedor f : lista)
            modelo.addRow(new Object[]{f.nome(), f.documento(), f.contato(), f.telefone(), f.email(), f.ativo() ? "Sim" : "Não"});
    }

    private void novo() {
        idAtual = 0;
        tabela.clearSelection();
        for (JTextField t : new JTextField[]{nome, documento, contato, telefone, email, endereco, obs}) t.setText("");
        ativo.setSelected(true);
        nome.requestFocusInWindow();
    }

    private void preencher(Fornecedor f) {
        idAtual = f.id();
        nome.setText(f.nome());
        documento.setText(f.documento());
        contato.setText(f.contato());
        telefone.setText(f.telefone());
        email.setText(f.email());
        endereco.setText(f.endereco());
        obs.setText(f.observacao());
        ativo.setSelected(f.ativo());
    }

    private void salvar() {
        ctrl.salvar(idAtual, nome.getText(), documento.getText(), contato.getText(), telefone.getText(),
                email.getText(), endereco.getText(), obs.getText(), ativo.isSelected());
        Ui.info(this, "Fornecedor salvo com sucesso.");
        novo();
        carregar();
    }
}
