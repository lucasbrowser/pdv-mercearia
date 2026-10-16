package com.pdv.view;

import com.pdv.controller.ProdutoController;
import com.pdv.model.Produto;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;

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
    private final ModoCadastro modo;
    private long idAtual = 0;

    public ProdutoView(Window pai) {
        super(pai, "Cadastro de Produtos", ModalityType.APPLICATION_MODAL);
        ProdutoTableModel.aplicarCores(tabela, modelo);

        JButton bNovo = Ui.botao("Novo", this::novo);
        JButton bEditar = Ui.botao("Editar", this::editar);
        JButton bSalvar = Ui.botao("Salvar", this::salvar);
        JButton bDescartar = Ui.botao("Descartar", this::descartar);
        modo = new ModoCadastro(bNovo, bEditar, bSalvar, bDescartar,
                new JComponent[]{codigo, descricao, unidade, preco, minimo, ativo},
                new JComponent[]{estoque},                       // estoque inicial só no cadastro novo
                new JComponent[]{tabela, busca});
        modo.observar(codigo, descricao, unidade, preco, estoque, minimo, ativo);
        modo.dicaEdicao("Editando o produto. O estoque só é alterado na tela Estoque.");

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
        botoes.add(bNovo);
        botoes.add(bEditar);
        botoes.add(bSalvar);
        botoes.add(bDescartar);
        botoes.add(Ui.cancelar(this, () -> modo.podeSair(this)));

        JPanel direita = new JPanel(new BorderLayout());
        direita.setPreferredSize(new Dimension(430, 0));
        direita.add(modo.getRotulo(), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(form, BorderLayout.NORTH);
        centro.add(botoes, BorderLayout.CENTER);
        direita.add(centro, BorderLayout.CENTER);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            int r = tabela.getSelectedRow();
            if (e.getValueIsAdjusting() || modo.editando() || r < 0) return;
            Produto p = modelo.get(r);
            modo.carregando(() -> preencher(p));
            modo.consulta(true);
        });

        add(esquerda, BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setSize(1150, 560);
        setLocationRelativeTo(pai);
        carregar();
    }

    private void carregar() {
        modelo.setDados(ctrl.pesquisar(busca.getText()));
        if (!modo.editando()) { // a busca limpa a seleção: mostra o formulário vazio e bloqueado
            idAtual = 0;
            modo.carregando(this::limpar);
            modo.consulta(false);
        }
    }

    private void limpar() {
        codigo.setText("");
        descricao.setText("");
        unidade.setSelectedIndex(0);
        preco.setText("");
        estoque.setText("0");
        minimo.setText("0");
        ativo.setSelected(true);
    }

    private void preencher(Produto p) {
        idAtual = p.id();
        codigo.setText(p.codigoBarras());
        descricao.setText(p.descricao());
        unidade.setSelectedItem(p.unidade());
        preco.setText(Fmt.num(p.preco()));
        estoque.setText(Fmt.qtd(p.estoque()));
        minimo.setText(Fmt.qtd(p.estoqueMinimo()));
        ativo.setSelected(p.ativo());
    }

    private void novo() {
        tabela.clearSelection();
        idAtual = 0;
        modo.carregando(this::limpar);
        modo.novo();
        codigo.requestFocusInWindow();
    }

    private void editar() {
        if (idAtual == 0) throw new NegocioException("Selecione um produto na tabela para editar.");
        modo.edicao();
        descricao.requestFocusInWindow();
    }

    private void salvar() {
        ctrl.salvar(idAtual, codigo.getText(), descricao.getText(), (String) unidade.getSelectedItem(),
                preco.getText(), estoque.getText(), minimo.getText(), ativo.isSelected());
        Ui.info(this, "Produto salvo com sucesso.");
        modo.consulta(false);
        carregar();
    }

    private void descartar() {
        if (!modo.podeSair(this)) return;
        int r = tabela.getSelectedRow();
        boolean voltaParaRegistro = idAtual != 0 && r >= 0;
        modo.consulta(voltaParaRegistro);
        if (voltaParaRegistro) {
            Produto p = modelo.get(r);
            modo.carregando(() -> preencher(p)); // restaura os dados originais
        } else {
            idAtual = 0;
            modo.carregando(this::limpar);
        }
    }
}
