package com.pdv.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ItemVenda {
    private final long produtoId;
    private final String codigoBarras;
    private final String descricao;
    private final String unidade;
    private BigDecimal quantidade;
    private final BigDecimal preco;

    public ItemVenda(long produtoId, String codigoBarras, String descricao, String unidade,
                     BigDecimal quantidade, BigDecimal preco) {
        this.produtoId = produtoId;
        this.codigoBarras = codigoBarras;
        this.descricao = descricao;
        this.unidade = unidade;
        this.quantidade = quantidade;
        this.preco = preco;
    }

    public long getProdutoId() { return produtoId; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getDescricao() { return descricao; }
    public String getUnidade() { return unidade; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPreco() { return preco; }

    /** Preço x quantidade, arredondado em centavos (ex.: 0,350 kg x 12,90 = 4,52). */
    public BigDecimal getSubtotal() { return preco.multiply(quantidade).setScale(2, RoundingMode.HALF_UP); }
}
