package com.pdv.model;

import java.math.BigDecimal;

public record Produto(long id, String codigoBarras, String descricao, String unidade,
                      BigDecimal preco, BigDecimal estoque, BigDecimal estoqueMinimo, boolean ativo) {

    /** Unidades vendidas em fração (peso/volume). */
    public static boolean fracionada(String unidade) { return "KG".equals(unidade) || "LT".equals(unidade); }

    public boolean fracionado() { return fracionada(unidade); }

    public boolean baixoEstoque() { return estoque.compareTo(estoqueMinimo) <= 0; }
}
