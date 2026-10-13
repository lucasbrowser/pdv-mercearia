package com.pdv.dao;

import com.pdv.model.Fornecedor;
import com.pdv.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FornecedorDAO {

    public List<Fornecedor> pesquisar(String termo, boolean apenasAtivos) {
        String sql = "SELECT * FROM fornecedor WHERE (nome LIKE ? OR documento LIKE ? OR contato LIKE ?)"
                + (apenasAtivos ? " AND ativo = 1" : "") + " ORDER BY nome";
        List<Fornecedor> lista = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + (termo == null ? "" : termo.trim()) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(map(rs));
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }

    public void inserir(Fornecedor f) {
        String sql = "INSERT INTO fornecedor(nome, documento, contato, telefone, email, endereco, observacao, ativo) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, f);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public void atualizar(Fornecedor f) {
        String sql = "UPDATE fornecedor SET nome=?, documento=?, contato=?, telefone=?, email=?, endereco=?, observacao=?, ativo=? WHERE id=?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, f);
            ps.setLong(9, f.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private void preencher(PreparedStatement ps, Fornecedor f) throws SQLException {
        ps.setString(1, f.nome());
        ps.setString(2, f.documento());
        ps.setString(3, f.contato());
        ps.setString(4, f.telefone());
        ps.setString(5, f.email());
        ps.setString(6, f.endereco());
        ps.setString(7, f.observacao());
        ps.setInt(8, f.ativo() ? 1 : 0);
    }

    private Fornecedor map(ResultSet rs) throws SQLException {
        return new Fornecedor(rs.getLong("id"), rs.getString("nome"), nv(rs.getString("documento")), nv(rs.getString("contato")),
                nv(rs.getString("telefone")), nv(rs.getString("email")), nv(rs.getString("endereco")),
                nv(rs.getString("observacao")), rs.getInt("ativo") == 1);
    }

    private static String nv(String s) { return s == null ? "" : s; }
}
