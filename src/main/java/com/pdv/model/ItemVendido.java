package com.pdv.model;

import java.math.BigDecimal;

/** Linha de item de venda concluída, usada nos relatórios. */
public record ItemVendido(long produtoId, String descricao, String unidade, BigDecimal quantidade, BigDecimal total) { }
