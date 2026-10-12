package com.pdv.view;

import com.pdv.controller.EstoqueController;
import com.pdv.model.MovimentoEstoque;
import com.pdv.model.Produto;
import com.pdv.model.TipoMovimento;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class EstoqueView extends JDialog {
    private final EstoqueController ctrl = new EstoqueController();
    private final ProdutoTableModel modelo = new ProdutoTableModel();
    private final JTable tabela = Ui.tabela(modelo);
    private final JTextField busca = new JTextField(22);
    private final JCheckBox baixo = new JCheckBox("Somente abaixo do mínimo");
    private final JLabel alerta = new JLabel(" ");
    private final DefaultTableModel movs = new DefaultTableModel(
            new String[]{"Data/Hora", "Produto", "Tipo", "Qtd", "Motivo", "Usuário"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public EstoqueView(Window pai) {
        super(pai, "Controle de Estoque", ModalityType.APPLICATION_MODAL);
        ProdutoTableModel.aplicarCores(tabela, modelo);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Buscar:"));
        topo.add(busca);
        topo.add(baixo);
        alerta.setForeground(new Color(0xC62828));
        topo.add(alerta);
        Ui.aoDigitar(busca, this::carregar);
        baixo.addActionListener(e -> carregar());

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Entrada (+)", () -> movimentar(TipoMovimento.ENTRADA)));
        botoes.add(Ui.botao("Saída / Perda (-)", () -> movimentar(TipoMovimento.SAIDA)));
        botoes.add(Ui.botao("Ajuste (contagem)", () -> movimentar(TipoMovimento.AJUSTE)));

        JPanel posicao = new JPanel(new BorderLayout());
        posicao.add(topo, BorderLayout.NORTH);
        posicao.add(new JScrollPane(tabela), BorderLayout.CENTER);
        posicao.add(botoes, BorderLayout.SOUTH);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Posição atual", posicao);
        abas.addTab("Movimentações", new JScrollPane(Ui.tabela(movs)));
        abas.addChangeListener(e -> { if (abas.getSelectedIndex() == 1) carregarMovimentos(); });

        add(abas);
        setSize(1000, 580);
        setLocationRelativeTo(pai);
        carregar();
    }

    private void carregar() {
        modelo.setDados(ctrl.listar(busca.getText(), baixo.isSelected()));
        long n = modelo.getDados().stream().filter(p -> p.estoque() <= p.estoqueMinimo()).count();
        alerta.setText(n > 0 ? "⚠ " + n + " produto(s) com estoque baixo" : " ");
    }

    private void carregarMovimentos() {
        movs.setRowCount(0);
        for (MovimentoEstoque m : ctrl.historico())
            movs.addRow(new Object[]{m.dataHora().format(Fmt.DATA_HORA), m.produto(), m.tipo().getRotulo(),
                    (m.quantidade() > 0 ? "+" : "") + m.quantidade(), m.motivo(), m.usuario()});
    }

    private void movimentar(TipoMovimento tipo) {
        int linha = tabela.getSelectedRow();
        if (linha < 0) throw new NegocioException("Selecione um produto na tabela.");
        Produto p = modelo.get(linha);
        JTextField qtd = new JTextField(8);
        JTextField motivo = new JTextField(24);
        Object[] msg = {"Produto: " + p.descricao() + "  (estoque atual: " + p.estoque() + ")",
                tipo == TipoMovimento.AJUSTE ? "Nova quantidade em estoque (contagem física):" : "Quantidade:", qtd,
                tipo == TipoMovimento.ENTRADA ? "Observação (opcional):" : "Motivo (obrigatório):", motivo};
        if (JOptionPane.showConfirmDialog(this, msg, tipo.getRotulo(), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            ctrl.movimentar(p, tipo, qtd.getText(), motivo.getText());
            carregar();
        }
    }
}
