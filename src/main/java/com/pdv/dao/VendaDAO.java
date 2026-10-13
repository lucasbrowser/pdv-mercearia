package com.pdv.dao;

import com.pdv.model.ItemVenda;
import com.pdv.model.ItemVendido;
import com.pdv.model.TipoMovimento;
import com.pdv.model.Venda;
import com.pdv.util.Database;
import com.pdv.util.NegocioException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendaDAO {
    private static final String SELECT_VENDA =
            "SELECT v.*, u.nome AS usuario_nome FROM venda v JOIN usuario u ON u.id = v.usuario_id";

    /** Grava venda + itens e baixa o estoque na MESMA transação (tudo ou nada). Exige caixa aberto. */
    public long salvar(Venda v) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                exigirCaixaAberto(c, v.getCaixaId());
                long id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO venda(data_hora, usuario_id, caixa_id, desconto, total, forma_pagamento, valor_recebido, status) "
                                + "VALUES (?,?,?,?,?,?,?,'CONCLUIDA')", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, v.getDataHora().truncatedTo(ChronoUnit.SECONDS).toString());
                    ps.setLong(2, v.getUsuarioId());
                    ps.setLong(3, v.getCaixaId());
                    ps.setString(4, v.getDesconto().toPlainString());
                    ps.setString(5, v.getTotal().toPlainString());
                    ps.setString(6, v.getFormaPagamento());
                    ps.setString(7, v.getValorRecebido().toPlainString());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        id = k.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO item_venda(venda_id, produto_id, descricao, unidade, quantidade, preco_unit, subtotal) VALUES (?,?,?,?,?,?,?)")) {
                    for (ItemVenda i : v.getItens()) {
                        ps.setLong(1, id);
                        ps.setLong(2, i.getProdutoId());
                        ps.setString(3, i.getDescricao());
                        ps.setString(4, i.getUnidade());
                        ps.setLong(5, Database.milesimos(i.getQuantidade()));
                        ps.setString(6, i.getPreco().toPlainString());
                        ps.setString(7, i.getSubtotal().toPlainString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                for (ItemVenda i : v.getItens())
                    EstoqueDAO.aplicar(c, i.getProdutoId(), TipoMovimento.VENDA, i.getQuantidade(), "Venda #" + id, v.getUsuarioId());
                c.commit();
                v.setId(id);
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private static String statusCaixa(Connection c, long caixaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM caixa WHERE id = ?")) {
            ps.setLong(1, caixaId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    private static void exigirCaixaAberto(Connection c, long caixaId) throws SQLException {
        if (!"ABERTO".equals(statusCaixa(c, caixaId)))
            throw new NegocioException("Caixa fechado. Abra o caixa antes de vender.");
    }

    public List<Venda> listar(LocalDate de, LocalDate ate) {
        List<Venda> lista = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SELECT_VENDA + " WHERE v.data_hora >= ? AND v.data_hora < ? ORDER BY v.id DESC")) {
            ps.setString(1, de.atStartOfDay().toString());
            ps.setString(2, ate.plusDays(1).atStartOfDay().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(map(rs));
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }

    /** Itens de vendas concluídas no período (relatório de produtos mais vendidos). */
    public List<ItemVendido> itensVendidos(LocalDate de, LocalDate ate) {
        String sql = "SELECT i.produto_id, i.descricao, i.unidade, i.quantidade, i.subtotal FROM item_venda i "
                + "JOIN venda v ON v.id = i.venda_id WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ?";
        List<ItemVendido> lista = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, de.atStartOfDay().toString());
            ps.setString(2, ate.plusDays(1).atStartOfDay().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new ItemVendido(rs.getLong(1), rs.getString(2), rs.getString(3),
                            Database.qtd(rs.getLong(4)), new java.math.BigDecimal(rs.getString(5))));
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }

    public Optional<Venda> buscar(long id) {
        try (Connection c = Database.get()) {
            Venda v;
            try (PreparedStatement ps = c.prepareStatement(SELECT_VENDA + " WHERE v.id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    v = map(rs);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM item_venda WHERE venda_id = ? ORDER BY id")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        v.getItens().add(new ItemVenda(rs.getLong("produto_id"), "", rs.getString("descricao"),
                                rs.getString("unidade"), Database.qtd(rs.getLong("quantidade")), Database.dec(rs, "preco_unit")));
                }
            }
            return Optional.of(v);
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /** Cancela a venda e devolve os itens ao estoque. Não permitido se o caixa da venda já foi fechado. */
    public void cancelar(long vendaId, long usuarioId) {
        Venda v = buscar(vendaId).orElseThrow(() -> new NegocioException("Venda não encontrada."));
        if ("CANCELADA".equals(v.getStatus())) throw new NegocioException("Esta venda já foi cancelada.");
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                if (v.getCaixaId() != 0 && "FECHADO".equals(statusCaixa(c, v.getCaixaId())))
                    throw new NegocioException("O caixa desta venda já foi fechado; não é possível cancelá-la.");
                try (PreparedStatement ps = c.prepareStatement("UPDATE venda SET status = 'CANCELADA' WHERE id = ?")) {
                    ps.setLong(1, vendaId);
                    ps.executeUpdate();
                }
                for (ItemVenda i : v.getItens())
                    EstoqueDAO.aplicar(c, i.getProdutoId(), TipoMovimento.CANCELAMENTO, i.getQuantidade(),
                            "Cancelamento da venda #" + vendaId, usuarioId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private Venda map(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.setId(rs.getLong("id"));
        v.setDataHora(LocalDateTime.parse(rs.getString("data_hora")));
        v.setUsuarioId(rs.getLong("usuario_id"));
        v.setCaixaId(rs.getLong("caixa_id")); // 0 em vendas antigas (anteriores ao controle de caixa)
        v.setUsuarioNome(rs.getString("usuario_nome"));
        v.setDesconto(Database.dec(rs, "desconto"));
        v.setTotalGravado(Database.dec(rs, "total"));
        v.setFormaPagamento(rs.getString("forma_pagamento"));
        v.setValorRecebido(Database.dec(rs, "valor_recebido"));
        v.setStatus(rs.getString("status"));
        return v;
    }
}
