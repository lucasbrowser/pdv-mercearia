package com.pdv.dao;

import com.pdv.model.ItemVenda;
import com.pdv.model.TipoMovimento;
import com.pdv.model.Venda;
import com.pdv.util.Database;
import com.pdv.util.NegocioException;

import java.math.BigDecimal;
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

    /** Grava venda + itens e baixa o estoque na MESMA transação (tudo ou nada). */
    public long salvar(Venda v) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO venda(data_hora, usuario_id, desconto, total, forma_pagamento, valor_recebido, status) "
                                + "VALUES (?,?,?,?,?,?,'CONCLUIDA')", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, v.getDataHora().truncatedTo(ChronoUnit.SECONDS).toString());
                    ps.setLong(2, v.getUsuarioId());
                    ps.setString(3, v.getDesconto().toPlainString());
                    ps.setString(4, v.getTotal().toPlainString());
                    ps.setString(5, v.getFormaPagamento());
                    ps.setString(6, v.getValorRecebido().toPlainString());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        id = k.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO item_venda(venda_id, produto_id, descricao, quantidade, preco_unit, subtotal) VALUES (?,?,?,?,?,?)")) {
                    for (ItemVenda i : v.getItens()) {
                        ps.setLong(1, id);
                        ps.setLong(2, i.getProdutoId());
                        ps.setString(3, i.getDescricao());
                        ps.setInt(4, i.getQuantidade());
                        ps.setString(5, i.getPreco().toPlainString());
                        ps.setString(6, i.getSubtotal().toPlainString());
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
                                rs.getInt("quantidade"), Database.dec(rs, "preco_unit")));
                }
            }
            return Optional.of(v);
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /** Marca a venda como cancelada e devolve os itens ao estoque (transação única). */
    public void cancelar(long vendaId, long usuarioId) {
        Venda v = buscar(vendaId).orElseThrow(() -> new NegocioException("Venda não encontrada."));
        if ("CANCELADA".equals(v.getStatus())) throw new NegocioException("Esta venda já foi cancelada.");
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
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
        v.setUsuarioNome(rs.getString("usuario_nome"));
        v.setDesconto(Database.dec(rs, "desconto"));
        v.setTotalGravado(Database.dec(rs, "total"));
        v.setFormaPagamento(rs.getString("forma_pagamento"));
        v.setValorRecebido(Database.dec(rs, "valor_recebido"));
        v.setStatus(rs.getString("status"));
        return v;
    }
}
