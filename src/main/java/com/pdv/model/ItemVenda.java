package com.pdv.model;

import java.math.BigDecimal;

public class ItemVenda {
    private final long produtoId;
    private final String codigoBarras;
    private final String descricao;
    private int quantidade;
    private final BigDecimal preco;

    public ItemVenda(long produtoId, String codigoBarras, String descricao, int quantidade, BigDecimal preco) {
        this.produtoId = produtoId;
        this.codigoBarras = codigoBarras;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.preco = preco;
    }

    public long getProdutoId() { return produtoId; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getDescricao() { return descricao; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPreco() { return preco; }
    public BigDecimal getSubtotal() { return preco.multiply(BigDecimal.valueOf(quantidade)); }
}
