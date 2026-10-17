package com.pdv.view;

import com.pdv.controller.FornecedorController;
import com.pdv.model.Fornecedor;
import com.pdv.util.NegocioException;

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
    private final ModoCadastro modo;
    private List<Fornecedor> lista = List.of();
    private long idAtual = 0;

    public FornecedorView(Window pai) {
        super(pai, "Cadastro de Fornecedores", ModalityType.APPLICATION_MODAL);

        JButton bNovo = Ui.botao("Novo", "novo", 'N', this::novo);
        JButton bEditar = Ui.botao("Editar", "editar", 'E', this::editar);
        JButton bSalvar = Ui.botao("Salvar", "salvar", 'S', this::salvar);
        JButton bCancelar = Ui.botao("Cancelar", "cancelar", 'C', this::descartar);
        modo = new ModoCadastro(bNovo, bEditar, bSalvar, bCancelar,
                new JComponent[]{nome, documento, contato, telefone, email, endereco, obs, ativo},
                new JComponent[0], new JComponent[]{tabela, busca});
        modo.observar(nome, documento, contato, telefone, email, endereco, obs, ativo);

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

        JPanel botoes = new JPanel(new GridLayout(1, 4, 8, 0));
        botoes.setBorder(BorderFactory.createEmptyBorder(8, 6, 6, 6));
        botoes.add(bNovo);
        botoes.add(bEditar);
        botoes.add(bSalvar);
        botoes.add(bCancelar);
        Ui.igualar(botoes);
        modo.instalar(this);

        JPanel direita = new JPanel(new BorderLayout());
        direita.setPreferredSize(new Dimension(540, 0));
        direita.add(modo.getRotulo(), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(form, BorderLayout.NORTH);
        JPanel abaixo = new JPanel(new BorderLayout());
        abaixo.add(botoes, BorderLayout.NORTH);
        centro.add(abaixo, BorderLayout.CENTER);
        direita.add(centro, BorderLayout.CENTER);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            int r = tabela.getSelectedRow();
            if (e.getValueIsAdjusting() || modo.editando() || r < 0) return;
            Fornecedor f = lista.get(r);
            modo.carregando(() -> preencher(f));
            modo.consulta(true);
        });

        add(esquerda, BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setSize(1200, 540);
        setLocationRelativeTo(pai);
        carregar();
    }

    private void carregar() {
        lista = ctrl.pesquisar(busca.getText(), false);
        modelo.setRowCount(0);
        for (Fornecedor f : lista)
            modelo.addRow(new Object[]{f.nome(), f.documento(), f.contato(), f.telefone(), f.email(), f.ativo() ? "Sim" : "Não"});
        if (!modo.editando()) {
            idAtual = 0;
            modo.carregando(this::limpar);
            modo.consulta(false);
        }
    }

    private void limpar() {
        for (JTextField t : new JTextField[]{nome, documento, contato, telefone, email, endereco, obs}) t.setText("");
        ativo.setSelected(true);
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

    private void novo() {
        tabela.clearSelection();
        idAtual = 0;
        modo.carregando(this::limpar);
        modo.novo();
        nome.requestFocusInWindow();
    }

    private void editar() {
        if (idAtual == 0) throw new NegocioException("Selecione um fornecedor na tabela para editar.");
        modo.edicao();
        nome.requestFocusInWindow();
    }

    private void salvar() {
        ctrl.salvar(idAtual, nome.getText(), documento.getText(), contato.getText(), telefone.getText(),
                email.getText(), endereco.getText(), obs.getText(), ativo.isSelected());
        Ui.info(this, "Fornecedor salvo com sucesso.");
        modo.consulta(false);
        carregar();
    }

    private void descartar() {
        if (!modo.podeSair(this)) return;
        int r = tabela.getSelectedRow();
        boolean voltaParaRegistro = idAtual != 0 && r >= 0;
        modo.consulta(voltaParaRegistro);
        if (voltaParaRegistro) {
            Fornecedor f = lista.get(r);
            modo.carregando(() -> preencher(f));
        } else {
            idAtual = 0;
            modo.carregando(this::limpar);
        }
    }
}
