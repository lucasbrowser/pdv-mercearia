package com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venda {
    private long id;
    private LocalDateTime dataHora = LocalDateTime.now();
    private long usuarioId;
    private long caixaId;
    private String usuarioNome = "";
    private final List<ItemVenda> itens = new ArrayList<>();
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal totalGravado = BigDecimal.ZERO; // usado quando os itens não foram carregados (listagens)
    private String formaPagamento = "";
    private BigDecimal valorRecebido = BigDecimal.ZERO;
    private String status = "CONCLUIDA";

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
    public long getCaixaId() { return caixaId; }
    public void setCaixaId(long caixaId) { this.caixaId = caixaId; }
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    public List<ItemVenda> getItens() { return itens; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    public void setTotalGravado(BigDecimal total) { this.totalGravado = total; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public BigDecimal getValorRecebido() { return valorRecebido; }
    public void setValorRecebido(BigDecimal valorRecebido) { this.valorRecebido = valorRecebido; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSubtotal() {
        return itens.stream().map(ItemVenda::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotal() {
        return itens.isEmpty() ? totalGravado : getSubtotal().subtract(desconto);
    }

    public BigDecimal getTroco() { return valorRecebido.subtract(getTotal()).max(BigDecimal.ZERO); }
}
