package com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentoCaixa(LocalDateTime dataHora, TipoMovCaixa tipo, BigDecimal valor, String motivo, String usuario) { }
