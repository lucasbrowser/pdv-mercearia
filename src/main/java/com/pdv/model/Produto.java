package com.pdv.model;

import java.math.BigDecimal;

public record Produto(long id, String codigoBarras, String descricao, String unidade,
                      BigDecimal preco, int estoque, int estoqueMinimo, boolean ativo) { }
