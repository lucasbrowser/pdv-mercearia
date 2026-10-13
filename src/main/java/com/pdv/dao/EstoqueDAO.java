package com.pdv.dao;

import com.pdv.model.MovimentoEstoque;
import com.pdv.model.TipoMovimento;
import com.pdv.util.Database;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {

    /** AJUSTE: "quantidade" é a nova contagem. Demais tipos: quantidade positiva a somar/subtrair. */
    public void movimentar(long produtoId, TipoMovimento tipo, BigDecimal quantidade, String motivo, long usuarioId) {
        if (tipo == TipoMovimento.AJUSTE ? quantidade.signum() < 0 : quantidade.signum() <= 0)
            throw new NegocioException("Quantidade inválida.");
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                aplicar(c, produtoId, tipo, quantidade, motivo, usuarioId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /** Aplica a movimentação dentro de uma transação já aberta (usado também por vendas/cancelamentos). */
    static void aplicar(Connection c, long produtoId, TipoMovimento tipo, BigDecimal quantidade, String motivo, long usuarioId)
            throws SQLException {
        long atual;
        String desc;
        try (PreparedStatement ps = c.prepareStatement("SELECT estoque, descricao FROM produto WHERE id = ?")) {
            ps.setLong(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new NegocioException("Produto não encontrado.");
                atual = rs.getLong(1);
                desc = rs.getString(2);
            }
        }
        long q = Database.milesimos(quantidade);
        long delta = switch (tipo) {
            case ENTRADA, CANCELAMENTO -> q;
            case SAIDA, VENDA -> -q;
            case AJUSTE -> q - atual;
        };
        if (atual + delta < 0)
            throw new NegocioException("Estoque insuficiente para \"" + desc + "\" (disponível: " + Fmt.qtd(Database.qtd(atual)) + ").");
        try (PreparedStatement ps = c.prepareStatement("UPDATE produto SET estoque = estoque + ? WHERE id = ?")) {
            ps.setLong(1, delta);
            ps.setLong(2, produtoId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO movimento_estoque(produto_id, tipo, quantidade, motivo, usuario_id, data_hora) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, produtoId);
            ps.setString(2, tipo.name());
            ps.setLong(3, delta);
            ps.setString(4, motivo);
            ps.setLong(5, usuarioId);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public List<MovimentoEstoque> historico(int limite) {
        String sql = "SELECT m.data_hora, p.descricao, m.tipo, m.quantidade, m.motivo, u.nome FROM movimento_estoque m "
                + "JOIN produto p ON p.id = m.produto_id LEFT JOIN usuario u ON u.id = m.usuario_id "
                + "ORDER BY m.id DESC LIMIT ?";
        List<MovimentoEstoque> lista = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new MovimentoEstoque(LocalDateTime.parse(rs.getString(1)), rs.getString(2),
                            TipoMovimento.valueOf(rs.getString(3)), Database.qtd(rs.getLong(4)), rs.getString(5), rs.getString(6)));
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }
}
