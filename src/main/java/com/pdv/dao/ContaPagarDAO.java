package com.pdv.dao;

import com.pdv.model.ContaPagar;
import com.pdv.model.TipoMovCaixa;
import com.pdv.util.Database;
import com.pdv.util.NegocioException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContaPagarDAO {
    private static final String SEL = "SELECT c.*, f.nome AS fornecedor_nome FROM conta_pagar c "
            + "LEFT JOIN fornecedor f ON f.id = c.fornecedor_id";

    /** Grava todas as parcelas numa única transação. */
    public void inserir(List<ContaPagar> contas) {
        String sql = "INSERT INTO conta_pagar(fornecedor_id, descricao, documento, valor, vencimento, status, observacao, lembrete_dias, criada_em) "
                + "VALUES (?,?,?,?,?,'ABERTA',?,?,?)";
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (ContaPagar k : contas) {
                    fornecedor(ps, 1, k.fornecedorId());
                    ps.setString(2, k.descricao());
                    ps.setString(3, k.documento());
                    ps.setString(4, k.valor().toPlainString());
                    ps.setString(5, k.vencimento().toString());
                    ps.setString(6, k.observacao());
                    ps.setInt(7, k.lembreteDias());
                    ps.setString(8, LocalDateTime.now().withNano(0).toString());
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public void atualizar(ContaPagar k) {
        String sql = "UPDATE conta_pagar SET fornecedor_id=?, descricao=?, documento=?, valor=?, vencimento=?, observacao=?, lembrete_dias=? "
                + "WHERE id=? AND status='ABERTA'";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            fornecedor(ps, 1, k.fornecedorId());
            ps.setString(2, k.descricao());
            ps.setString(3, k.documento());
            ps.setString(4, k.valor().toPlainString());
            ps.setString(5, k.vencimento().toString());
            ps.setString(6, k.observacao());
            ps.setInt(7, k.lembreteDias());
            ps.setLong(8, k.id());
            if (ps.executeUpdate() == 0) throw new NegocioException("Somente contas em aberto podem ser editadas.");
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    public Optional<ContaPagar> buscar(long id) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(SEL + " WHERE c.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /** Por data de vencimento (todas as situações). */
    public List<ContaPagar> listar(LocalDate de, LocalDate ate) {
        return consultar(SEL + " WHERE c.vencimento >= ? AND c.vencimento <= ? ORDER BY c.vencimento, c.id",
                de.toString(), ate.toString());
    }

    /** Contas em aberto que vencem até a data-limite (o filtro por "dias de aviso" de cada conta é feito no controller). */
    public List<ContaPagar> abertasAte(LocalDate limite) {
        return consultar(SEL + " WHERE c.status = 'ABERTA' AND c.vencimento <= ? ORDER BY c.vencimento, c.id", limite.toString());
    }

    private List<ContaPagar> consultar(String sql, String... params) {
        List<ContaPagar> lista = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(map(rs));
            }
        } catch (SQLException e) {
            throw Database.erro(e);
        }
        return lista;
    }

    public void cancelar(long id) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE conta_pagar SET status='CANCELADA' WHERE id=? AND status='ABERTA'")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) throw new NegocioException("Somente contas em aberto podem ser canceladas.");
        } catch (SQLException e) {
            throw Database.erro(e);
        }
    }

    /**
     * Baixa a conta. Se caixaId != null, a saída de dinheiro é registrada no caixa NA MESMA transação
     * (movimento do tipo PAGAMENTO_CONTA), reduzindo o dinheiro esperado na gaveta.
     */
    public void pagar(long contaId, BigDecimal valorPago, String forma, long usuarioId, Long caixaId,
                      String observacao, String motivoCaixa) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                String agora = LocalDateTime.now().withNano(0).toString();
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE conta_pagar SET status='PAGA', data_pagamento=?, valor_pago=?, forma_pagamento=?, "
                                + "usuario_pagamento_id=?, caixa_id=?, observacao=COALESCE(?, observacao) WHERE id=? AND status='ABERTA'")) {
                    ps.setString(1, agora);
                    ps.setString(2, valorPago.toPlainString());
                    ps.setString(3, forma);
                    ps.setLong(4, usuarioId);
                    if (caixaId == null) ps.setNull(5, Types.INTEGER); else ps.setLong(5, caixaId);
                    ps.setString(6, observacao == null || observacao.isBlank() ? null : observacao.trim());
                    ps.setLong(7, contaId);
                    if (ps.executeUpdate() == 0) throw new NegocioException("Esta conta não está mais em aberto.");
                }
                if (caixaId != null) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT status FROM caixa WHERE id = ?")) {
                        ps.setLong(1, caixaId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !"ABERTO".equals(rs.getString(1))) throw new NegocioException("O caixa não está aberto.");
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO movimento_caixa(caixa_id, tipo, valor, motivo, usuario_id, data_hora, conta_id) VALUES (?,?,?,?,?,?,?)")) {
                        ps.setLong(1, caixaId);
                        ps.setString(2, TipoMovCaixa.PAGAMENTO_CONTA.name());
                        ps.setString(3, valorPago.toPlainString());
                        ps.setString(4, motivoCaixa);
                        ps.setLong(5, usuarioId);
                        ps.setString(6, agora);
                        ps.setLong(7, contaId);
                        ps.executeUpdate();
                    }
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

    private static void fornecedor(PreparedStatement ps, int idx, long fornecedorId) throws SQLException {
        if (fornecedorId == 0) ps.setNull(idx, Types.INTEGER); else ps.setLong(idx, fornecedorId);
    }

    private ContaPagar map(ResultSet rs) throws SQLException {
        String pg = rs.getString("data_pagamento");
        String vp = rs.getString("valor_pago");
        String nome = rs.getString("fornecedor_nome");
        return new ContaPagar(rs.getLong("id"), rs.getLong("fornecedor_id"), nome == null ? "" : nome,
                rs.getString("descricao"), nz(rs.getString("documento")), Database.dec(rs, "valor"),
                LocalDate.parse(rs.getString("vencimento")), rs.getString("status"),
                pg == null ? null : LocalDateTime.parse(pg), vp == null ? null : new BigDecimal(vp),
                nz(rs.getString("forma_pagamento")), nz(rs.getString("observacao")), rs.getInt("lembrete_dias"));
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
