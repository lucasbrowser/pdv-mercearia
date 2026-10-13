package com.pdv.view;

import com.pdv.controller.ProdutoController;
import com.pdv.model.Produto;
import com.pdv.util.Fmt;

import javax.swing.*;
import java.awt.*;

public class ProdutoView extends JDialog {
    private final ProdutoController ctrl = new ProdutoController();
    private final ProdutoTableModel modelo = new ProdutoTableModel();
    private final JTable tabela = Ui.tabela(modelo);
    private final JTextField busca = new JTextField(22);
    private final JTextField codigo = new JTextField(), descricao = new JTextField(), preco = new JTextField(),
            estoque = new JTextField("0"), minimo = new JTextField("0");
    private final JComboBox<String> unidade = new JComboBox<>(new String[]{"UN", "KG", "CX", "PCT", "LT"});
    private final JCheckBox ativo = new JCheckBox("Produto ativo", true);
    private long idAtual = 0;

    public ProdutoView(Window pai) {
        super(pai, "Cadastro de Produtos", ModalityType.APPLICATION_MODAL);
        ProdutoTableModel.aplicarCores(tabela, modelo);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Buscar (descrição ou código):"));
        topo.add(busca);
        Ui.aoDigitar(busca, this::carregar);

        JPanel esquerda = new JPanel(new BorderLayout());
        esquerda.add(topo, BorderLayout.NORTH);
        esquerda.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Dados do produto"));
        Ui.linha(form, 0, "Código de barras:", codigo);
        Ui.linha(form, 1, "Descrição:", descricao);
        Ui.linha(form, 2, "Unidade (KG/LT = venda por peso):", unidade);
        Ui.linha(form, 3, "Preço (R$):", preco);
        Ui.linha(form, 4, "Estoque inicial:", estoque);
        Ui.linha(form, 5, "Estoque mínimo:", minimo);
        Ui.linha(form, 6, "", ativo);

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Novo", this::novo));
        botoes.add(Ui.botao("Salvar", this::salvar));

        JPanel direita = new JPanel(new BorderLayout());
        direita.setPreferredSize(new Dimension(380, 0));
        direita.add(form, BorderLayout.NORTH);
        direita.add(botoes, BorderLayout.CENTER);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabela.getSelectedRow() >= 0) preencher(modelo.get(tabela.getSelectedRow()));
        });

        add(esquerda, BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setSize(1100, 560);
        setLocationRelativeTo(pai);
        carregar();
    }

    private void carregar() { modelo.setDados(ctrl.pesquisar(busca.getText())); }

    private void novo() {
        idAtual = 0;
        tabela.clearSelection();
        codigo.setText(""); descricao.setText(""); preco.setText(""); estoque.setText("0"); minimo.setText("0");
        unidade.setSelectedIndex(0);
        ativo.setSelected(true);
        estoque.setEnabled(true);
        codigo.requestFocusInWindow();
    }

    private void preencher(Produto p) {
        idAtual = p.id();
        codigo.setText(p.codigoBarras());
        descricao.setText(p.descricao());
        unidade.setSelectedItem(p.unidade());
        preco.setText(Fmt.num(p.preco()));
        estoque.setText(Fmt.qtd(p.estoque()));
        estoque.setEnabled(false); // estoque só muda pela tela de Estoque (rastreabilidade)
        minimo.setText(Fmt.qtd(p.estoqueMinimo()));
        ativo.setSelected(p.ativo());
    }

    private void salvar() {
        ctrl.salvar(idAtual, codigo.getText(), descricao.getText(), (String) unidade.getSelectedItem(),
                preco.getText(), estoque.getText(), minimo.getText(), ativo.isSelected());
        Ui.info(this, "Produto salvo com sucesso.");
        novo();
        carregar();
    }
}
