package com.pdv.util;

import java.math.BigDecimal;
import java.sql.*;

/** Acesso ao SQLite local (~/pdv-mercado/pdv.db) e criação do esquema. */
public final class Database {
    private static final String URL = "jdbc:sqlite:" + Config.diretorio().resolve("pdv.db");

    private Database() { }

    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        return c;
    }

    public static BigDecimal dec(ResultSet rs, String coluna) throws SQLException {
        return new BigDecimal(rs.getString(coluna));
    }

    public static RuntimeException erro(SQLException e) {
        if (e.getMessage() != null && e.getMessage().contains("UNIQUE"))
            return new NegocioException("Já existe um registro com esse login / código de barras.");
        return new IllegalStateException("Erro de banco de dados: " + e.getMessage(), e);
    }

    public static void init() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, "
                + "login TEXT NOT NULL UNIQUE, senha_hash TEXT NOT NULL, admin INTEGER NOT NULL DEFAULT 0, "
                + "ativo INTEGER NOT NULL DEFAULT 1, permissoes TEXT NOT NULL DEFAULT '')",
            "CREATE TABLE IF NOT EXISTS produto (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo_barras TEXT NOT NULL UNIQUE, "
                + "descricao TEXT NOT NULL, unidade TEXT NOT NULL DEFAULT 'UN', preco TEXT NOT NULL, "
                + "estoque INTEGER NOT NULL DEFAULT 0, estoque_minimo INTEGER NOT NULL DEFAULT 0, ativo INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS venda (id INTEGER PRIMARY KEY AUTOINCREMENT, data_hora TEXT NOT NULL, "
                + "usuario_id INTEGER NOT NULL REFERENCES usuario(id), desconto TEXT NOT NULL, total TEXT NOT NULL, "
                + "forma_pagamento TEXT NOT NULL, valor_recebido TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'CONCLUIDA')",
            "CREATE TABLE IF NOT EXISTS item_venda (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "venda_id INTEGER NOT NULL REFERENCES venda(id), produto_id INTEGER NOT NULL REFERENCES produto(id), "
                + "descricao TEXT NOT NULL, quantidade INTEGER NOT NULL, preco_unit TEXT NOT NULL, subtotal TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS movimento_estoque (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "produto_id INTEGER NOT NULL REFERENCES produto(id), tipo TEXT NOT NULL, quantidade INTEGER NOT NULL, "
                + "motivo TEXT, usuario_id INTEGER REFERENCES usuario(id), data_hora TEXT NOT NULL)",
            "CREATE INDEX IF NOT EXISTS idx_venda_data ON venda(data_hora)"
        };
        try (Connection c = get(); Statement s = c.createStatement()) {
            for (String sql : ddl) s.executeUpdate(sql);
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM usuario")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO usuario(nome, login, senha_hash, admin, ativo, permissoes) VALUES (?,?,?,1,1,'')")) {
                        ps.setString(1, "Administrador");
                        ps.setString(2, "admin");
                        ps.setString(3, Seguranca.hash("admin123"));
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inicializar o banco de dados", e);
        }
    }
}
