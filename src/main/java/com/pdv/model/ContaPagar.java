package com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record ContaPagar(long id, long fornecedorId, String fornecedorNome, String descricao, String documento,
                         BigDecimal valor, LocalDate vencimento, String status, LocalDateTime dataPagamento,
                         BigDecimal valorPago, String formaPagamento, String observacao, int lembreteDias) {

    public static final List<String> FORMAS = List.of("Dinheiro do caixa", "PIX / Transferência", "Boleto / Banco", "Cartão", "Outros");
    public static final String FORMA_CAIXA = FORMAS.get(0);

    public enum Situacao {
        PAGA("Paga"), CANCELADA("Cancelada"), VENCIDA("Vencida"), VENCE_HOJE("Vence hoje"), A_VENCER("A vencer");

        private final String rotulo;

        Situacao(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }
    }

    public boolean aberta() { return "ABERTA".equals(status); }

    /** Negativo = dias de atraso. */
    public long diasParaVencer(LocalDate hoje) { return ChronoUnit.DAYS.between(hoje, vencimento); }

    public Situacao situacao(LocalDate hoje) {
        if ("PAGA".equals(status)) return Situacao.PAGA;
        if ("CANCELADA".equals(status)) return Situacao.CANCELADA;
        long d = diasParaVencer(hoje);
        return d < 0 ? Situacao.VENCIDA : d == 0 ? Situacao.VENCE_HOJE : Situacao.A_VENCER;
    }
}
