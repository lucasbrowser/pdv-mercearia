package com.pdv.model;

import java.time.LocalDateTime;

public record MovimentoEstoque(LocalDateTime dataHora, String produto, TipoMovimento tipo,
                               int quantidade, String motivo, String usuario) { }
