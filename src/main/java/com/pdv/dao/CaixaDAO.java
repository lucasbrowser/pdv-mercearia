package com.pdv.dao;

import com.pdv.model.*;
import com.pdv.util.Database;
import com.pdv.util.NegocioException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class CaixaDAO {
    private static final String SEL = "SELECT c.*, ua.nome AS aberto_por, uf.nome AS fechado_por FROM caixa c "
            + "JOIN usuario ua ON ua.id = c.usuario_abertura_id LEFT JOIN usuario uf ON uf.id = c.usuario_fechamento_id";

    public Optional<Caixa> buscarAberto() { return um(SEL + " WHERE c.status = 'ABERTO' ORDER BY c.id DESC LIMIT 1", null); }

    public Optional<Caixa> buscar(long id) { return um(SEL + " WHERE c.id = ?", id); }

    /** Só permite um caixa aberto por vez (terminal único). */
    public long abrir(BigDecimal valorAbertura, long usuarioId) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM caixa WHERE status = 'ABERTO'")) {
                    rs.next();
                    if (rs.getInt(1) > 0) throw new NegocioException("Já existe um caixa aberto.");
                }
                long id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO caixa(usuario_abertura_id, data_abertura, valor_abertura, status) VALUES (?,?,?,'ABERTO')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, usuarioId);
                    ps.setString(2, LocalDateTime.now().withNano(0).toString());
                    ps.setString(3, valorAbertura.toPlainString());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        id = k.getLong(1);
                    }
                }
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public void registrarMovimento(long caixaId, TipoMovCaixa tipo, BigDecimal valor, String motivo, long usuarioId) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("SELECT status FROM caixa WHERE id = ?")) {
                    ps.setLong(1, caixaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"ABERTO".equals(rs.getString(1))) throw new NegocioException("O caixa não está aberto.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO movimento_caixa(caixa_id, tipo, valor, motivo, usuario_id, data_hora) VALUES (?,?,?,?,?,?)")) {
                    ps.setLong(1, caixaId);
                    ps.setString(2, tipo.name());
                    ps.setString(3, valor.toPlainString());
                    ps.setString(4, motivo);
                    ps.setLong(5, usuarioId);
                    ps.setString(6, LocalDateTime.now().withNano(0).toString());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public void fechar(long caixaId, BigDecimal esperado, BigDecimal contado, String obs, long usuarioId) {
        String sql = "UPDATE caixa SET status='FECHADO', data_fechamento=?, usuario_fechamento_id=?, valor_esperado=?, "
                + "valor_contado=?, diferenca=?, observacao=? WHERE id=? AND status='ABERTO'";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().withNano(0).toString());
            ps.setLong(2, usuarioId);
            ps.setString(3, esperado.toPlainString());
            ps.setString(4, contado.toPlainString());
            ps.setString(5, contado.subtract(esperado).toPlainString());
            ps.setString(6, obs);
            ps.setLong(7, caixaId);
            if (ps.executeUpdate() == 0) throw new NegocioException("Este caixa já foi fechado.");
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public List<Caixa> listar(LocalDate de, LocalDate ate) {
        List<Caixa> lista = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SEL + " WHERE c.data_abertura >= ? AND c.data_abertura < ? ORDER BY c.id DESC")) {
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

    /** Consolida vendas por forma de pagamento, sangrias/suprimentos e o dinheiro esperado na gaveta. */
    public ResumoCaixa resumir(long caixaId) {
        Caixa caixa = buscar(caixaId).orElseThrow(() -> new NegocioException("Caixa não encontrado."));
        Map<String, BigDecimal> porForma = new LinkedHashMap<>();
        for (String f : FormaPagamento.TODAS) porForma.put(f, BigDecimal.ZERO);
        int vendas = 0, canceladas = 0;
        BigDecimal totalVendas = BigDecimal.ZERO, suprimentos = BigDecimal.ZERO, sangrias = BigDecimal.ZERO, pagamentos = BigDecimal.ZERO;
        List<MovimentoCaixa> movs = new ArrayList<>();
        try (Connection c = Database.get()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT forma_pagamento, status, total FROM venda WHERE caixa_id = ?")) {
                ps.setLong(1, caixaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (!"CONCLUIDA".equals(rs.getString(2))) { canceladas++; continue; }
                        BigDecimal t = new BigDecimal(rs.getString(3));
                        vendas++;
                        totalVendas = totalVendas.add(t);
                        porForma.merge(rs.getString(1), t, BigDecimal::add);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT m.data_hora, m.tipo, m.valor, m.motivo, u.nome FROM movimento_caixa m "
                            + "JOIN usuario u ON u.id = m.usuario_id WHERE m.caixa_id = ? ORDER BY m.id")) {
                ps.setLong(1, caixaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TipoMovCaixa tipo = TipoMovCaixa.valueOf(rs.getString(2));
                        BigDecimal valor = new BigDecimal(rs.getString(3));
                        switch (tipo) {
                            case SANGRIA -> sangrias = sangrias.add(valor);
                            case SUPRIMENTO -> suprimentos = suprimentos.add(valor);
                            case PAGAMENTO_CONTA -> pagamentos = pagamentos.add(valor);
                        }
                        movs.add(new MovimentoCaixa(LocalDateTime.parse(rs.getString(1)), tipo, valor, rs.getString(4), rs.getString(5)));
                    }
                }
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        BigDecimal esperado = caixa.valorAbertura().add(porForma.get(FormaPagamento.DINHEIRO)).add(suprimentos).subtract(sangrias).subtract(pagamentos);
        return new ResumoCaixa(caixa, porForma, vendas, canceladas, totalVendas, suprimentos, sangrias, pagamentos, esperado, movs);
    }

    private Optional<Caixa> um(String sql, Object param) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    private Caixa map(ResultSet rs) throws SQLException {
        String fech = rs.getString("data_fechamento");
        return new Caixa(rs.getLong("id"), rs.getString("aberto_por"), LocalDateTime.parse(rs.getString("data_abertura")),
                Database.dec(rs, "valor_abertura"), rs.getString("status"), fech == null ? null : LocalDateTime.parse(fech),
                rs.getString("fechado_por"), decOuNulo(rs, "valor_esperado"), decOuNulo(rs, "valor_contado"),
                decOuNulo(rs, "diferenca"), rs.getString("observacao"));
    }

    private static BigDecimal decOuNulo(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? null : new BigDecimal(s);
    }
}
