package com.pdv.dao;

import com.pdv.model.Produto;
import com.pdv.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoDAO {

    public Optional<Produto> porCodigo(String codigo) {
        return um("SELECT * FROM produto WHERE codigo_barras = ? AND ativo = 1", codigo);
    }

    public Optional<Produto> porId(long id) {
        return um("SELECT * FROM produto WHERE id = ?", id);
    }

    public List<Produto> pesquisar(String termo, boolean apenasAtivos) {
        return pesquisar(termo, apenasAtivos, 500);
    }

    public List<Produto> pesquisar(String termo, boolean apenasAtivos, int limite) {
        String sql = "SELECT * FROM produto WHERE (descricao LIKE ? OR codigo_barras LIKE ?)"
                + (apenasAtivos ? " AND ativo = 1" : "") + " ORDER BY descricao LIMIT " + limite;
        List<Produto> lista = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + (termo == null ? "" : termo.trim()) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(map(rs));
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }

    /** O estoque inicial é gravado como 0; use EstoqueDAO para movimentar. */
    public long inserir(Produto p) {
        String sql = "INSERT INTO produto(codigo_barras, descricao, unidade, preco, estoque, estoque_minimo, ativo) VALUES (?,?,?,?,0,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.codigoBarras());
            ps.setString(2, p.descricao());
            ps.setString(3, p.unidade());
            ps.setString(4, p.preco().toPlainString());
            ps.setLong(5, Database.milesimos(p.estoqueMinimo()));
            ps.setInt(6, p.ativo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /** Não altera o estoque (somente via movimentações). */
    public void atualizar(Produto p) {
        String sql = "UPDATE produto SET codigo_barras=?, descricao=?, unidade=?, preco=?, estoque_minimo=?, ativo=? WHERE id=?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.codigoBarras());
            ps.setString(2, p.descricao());
            ps.setString(3, p.unidade());
            ps.setString(4, p.preco().toPlainString());
            ps.setLong(5, Database.milesimos(p.estoqueMinimo()));
            ps.setInt(6, p.ativo() ? 1 : 0);
            ps.setLong(7, p.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private Optional<Produto> um(String sql, Object param) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private Produto map(ResultSet rs) throws SQLException {
        return new Produto(rs.getLong("id"), rs.getString("codigo_barras"), rs.getString("descricao"),
                rs.getString("unidade"), Database.dec(rs, "preco"), Database.qtd(rs.getLong("estoque")),
                Database.qtd(rs.getLong("estoque_minimo")), rs.getInt("ativo") == 1);
    }
}
