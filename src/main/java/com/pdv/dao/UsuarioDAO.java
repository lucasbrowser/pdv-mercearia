package com.pdv.dao;

import com.pdv.model.Permissao;
import com.pdv.model.Usuario;
import com.pdv.util.Database;
import com.pdv.util.Seguranca;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class UsuarioDAO {

    public Optional<Usuario> autenticar(String login, String senha) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM usuario WHERE login = ? AND ativo = 1")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Seguranca.confere(senha, rs.getString("senha_hash"))) return Optional.of(map(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM usuario ORDER BY nome")) {
            while (rs.next()) lista.add(map(rs));
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }

    public void inserir(Usuario u, String senha) {
        String sql = "INSERT INTO usuario(nome, login, senha_hash, admin, ativo, permissoes) VALUES (?,?,?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.nome());
            ps.setString(2, u.login());
            ps.setString(3, Seguranca.hash(senha));
            ps.setInt(4, u.admin() ? 1 : 0);
            ps.setInt(5, u.ativo() ? 1 : 0);
            ps.setString(6, serializar(u.permissoes()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /** Se novaSenha for vazia, a senha atual é mantida. */
    public void atualizar(Usuario u, String novaSenha) {
        boolean trocaSenha = novaSenha != null && !novaSenha.isEmpty();
        String sql = "UPDATE usuario SET nome=?, login=?, admin=?, ativo=?, permissoes=?"
                + (trocaSenha ? ", senha_hash=?" : "") + " WHERE id=?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.nome());
            ps.setString(i++, u.login());
            ps.setInt(i++, u.admin() ? 1 : 0);
            ps.setInt(i++, u.ativo() ? 1 : 0);
            ps.setString(i++, serializar(u.permissoes()));
            if (trocaSenha) ps.setString(i++, Seguranca.hash(novaSenha));
            ps.setLong(i, u.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private static String serializar(Set<Permissao> p) {
        return p.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private Usuario map(ResultSet rs) throws SQLException {
        Set<Permissao> perms = EnumSet.noneOf(Permissao.class);
        String s = rs.getString("permissoes");
        if (s != null && !s.isBlank()) for (String x : s.split(",")) perms.add(Permissao.valueOf(x.trim()));
        return new Usuario(rs.getLong("id"), rs.getString("nome"), rs.getString("login"),
                rs.getInt("admin") == 1, rs.getInt("ativo") == 1, perms);
    }
}
