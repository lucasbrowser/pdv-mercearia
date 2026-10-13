package com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentoEstoque(LocalDateTime dataHora, String produto, TipoMovimento tipo,
                               BigDecimal quantidade, String motivo, String usuario) { }
