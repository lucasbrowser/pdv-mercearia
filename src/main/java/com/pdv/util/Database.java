package com.pdv.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

/**
 * Acesso ao SQLite local (~/pdv-mercado/pdv.db), criação e migração do esquema.
 * Quantidades (estoque, itens, movimentos) são gravadas em milésimos (x1000) para permitir venda por peso sem erro de ponto flutuante.
 */
public final class Database {
    private static final String URL = "jdbc:sqlite:" + Config.diretorio().resolve("pdv.db");
    private static final int VERSAO = 3;

    private Database() { }

    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        return c;
    }

    public static BigDecimal dec(ResultSet rs, String coluna) throws SQLException {
        return new BigDecimal(rs.getString(coluna));
    }

    /** Milésimos gravados -> quantidade. */
    public static BigDecimal qtd(long milesimos) { return BigDecimal.valueOf(milesimos, 3); }

    /** Quantidade -> milésimos para gravar. */
    public static long milesimos(BigDecimal q) {
        return q.setScale(3, RoundingMode.HALF_UP).unscaledValue().longValueExact();
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
            "CREATE TABLE IF NOT EXISTS caixa (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario_abertura_id INTEGER NOT NULL REFERENCES usuario(id), data_abertura TEXT NOT NULL, "
                + "valor_abertura TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'ABERTO', data_fechamento TEXT, "
                + "usuario_fechamento_id INTEGER REFERENCES usuario(id), valor_esperado TEXT, valor_contado TEXT, "
                + "diferenca TEXT, observacao TEXT)",
            "CREATE TABLE IF NOT EXISTS movimento_caixa (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "caixa_id INTEGER NOT NULL REFERENCES caixa(id), tipo TEXT NOT NULL, valor TEXT NOT NULL, "
                + "motivo TEXT NOT NULL, usuario_id INTEGER NOT NULL REFERENCES usuario(id), data_hora TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS fornecedor (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, documento TEXT, "
                + "contato TEXT, telefone TEXT, email TEXT, endereco TEXT, observacao TEXT, ativo INTEGER NOT NULL DEFAULT 1)",
            "CREATE TABLE IF NOT EXISTS conta_pagar (id INTEGER PRIMARY KEY AUTOINCREMENT, fornecedor_id INTEGER REFERENCES fornecedor(id), "
                + "descricao TEXT NOT NULL, documento TEXT, valor TEXT NOT NULL, vencimento TEXT NOT NULL, "
                + "status TEXT NOT NULL DEFAULT 'ABERTA', data_pagamento TEXT, valor_pago TEXT, forma_pagamento TEXT, "
                + "usuario_pagamento_id INTEGER REFERENCES usuario(id), caixa_id INTEGER, observacao TEXT, "
                + "lembrete_dias INTEGER NOT NULL DEFAULT 3, criada_em TEXT NOT NULL)",
            "CREATE INDEX IF NOT EXISTS idx_conta_venc ON conta_pagar(vencimento)",
            "CREATE INDEX IF NOT EXISTS idx_venda_data ON venda(data_hora)",
            "CREATE INDEX IF NOT EXISTS idx_item_venda ON item_venda(venda_id)"
        };
        try (Connection c = get()) {
            boolean bancoAntigo = tabelaExiste(c, "usuario");
            try (Statement s = c.createStatement()) {
                for (String sql : ddl) s.executeUpdate(sql);
            }
            migrar(c, bancoAntigo);
            criarAdminSeVazio(c);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inicializar o banco de dados", e);
        }
    }

    private static void migrar(Connection c, boolean bancoAntigo) throws SQLException {
        int versao;
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA user_version")) {
            versao = rs.next() ? rs.getInt(1) : 0;
        }
        if (versao >= VERSAO) return;
        garantirColuna(c, "venda", "caixa_id", "INTEGER");
        garantirColuna(c, "item_venda", "unidade", "TEXT NOT NULL DEFAULT 'UN'");
        garantirColuna(c, "movimento_caixa", "conta_id", "INTEGER");
        try (Statement s = c.createStatement()) {
            if (bancoAntigo && versao < 2) { // v1 gravava quantidades inteiras: converte para milésimos
                s.executeUpdate("UPDATE produto SET estoque = estoque * 1000, estoque_minimo = estoque_minimo * 1000");
                s.executeUpdate("UPDATE item_venda SET quantidade = quantidade * 1000");
                s.executeUpdate("UPDATE movimento_estoque SET quantidade = quantidade * 1000");
            }
            s.executeUpdate("PRAGMA user_version = " + VERSAO);
        }
    }

    private static boolean tabelaExiste(Connection c, String tabela) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tabela);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void garantirColuna(Connection c, String tabela, String coluna, String tipo) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (rs.next()) if (coluna.equalsIgnoreCase(rs.getString("name"))) return;
        }
        try (Statement s = c.createStatement()) {
            s.executeUpdate("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + tipo);
        }
    }

    private static void criarAdminSeVazio(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM usuario")) {
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
    }
}
