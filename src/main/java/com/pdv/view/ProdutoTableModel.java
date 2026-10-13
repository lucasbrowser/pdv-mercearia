package com.pdv.view;

import com.pdv.model.Produto;
import com.pdv.util.Fmt;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class ProdutoTableModel extends AbstractTableModel {
    private static final String[] COLUNAS = {"Código", "Descrição", "Un.", "Preço", "Estoque", "Mínimo", "Ativo"};
    private List<Produto> dados = List.of();

    public void setDados(List<Produto> d) { dados = d; fireTableDataChanged(); }
    public Produto get(int linha) { return dados.get(linha); }
    public List<Produto> getDados() { return dados; }

    @Override public int getRowCount() { return dados.size(); }
    @Override public int getColumnCount() { return COLUNAS.length; }
    @Override public String getColumnName(int c) { return COLUNAS[c]; }

    @Override public Object getValueAt(int r, int c) {
        Produto p = dados.get(r);
        return switch (c) {
            case 0 -> p.codigoBarras();
            case 1 -> p.descricao();
            case 2 -> p.unidade();
            case 3 -> Fmt.num(p.preco());
            case 4 -> Fmt.qtd(p.estoque());
            case 5 -> Fmt.qtd(p.estoqueMinimo());
            default -> p.ativo() ? "Sim" : "Não";
        };
    }

    /** Vermelho: estoque abaixo/igual ao mínimo. Cinza: produto inativo. */
    public static void aplicarCores(JTable tabela, ProdutoTableModel modelo) {
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foco, r, c);
                Produto p = modelo.get(r);
                if (!sel) comp.setForeground(!p.ativo() ? Color.GRAY
                        : p.baixoEstoque() ? new Color(0xC62828) : t.getForeground());
                return comp;
            }
        });
    }
}
