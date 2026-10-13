package com.pdv.model;

public enum TipoRelatorio {
    VENDAS_DIA("Vendas por dia"),
    FORMA_PAGAMENTO("Vendas por forma de pagamento"),
    OPERADOR("Vendas por operador"),
    MAIS_VENDIDOS("Produtos mais vendidos"),
    ESTOQUE("Posição de estoque (valor em estoque)"),
    FECHAMENTOS("Fechamentos de caixa"),
    CONTAS_PAGAR("Contas a pagar (por vencimento)");

    private final String rotulo;

    TipoRelatorio(String rotulo) { this.rotulo = rotulo; }

    public boolean usaPeriodo() { return this != ESTOQUE; }

    @Override public String toString() { return rotulo; }
}
