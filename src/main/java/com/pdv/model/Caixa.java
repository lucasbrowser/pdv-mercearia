package com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Caixa(long id, String abertoPor, LocalDateTime dataAbertura, BigDecimal valorAbertura, String status,
                    LocalDateTime dataFechamento, String fechadoPor, BigDecimal valorEsperado,
                    BigDecimal valorContado, BigDecimal diferenca, String observacao) {
    public boolean aberto() { return "ABERTO".equals(status); }
}
