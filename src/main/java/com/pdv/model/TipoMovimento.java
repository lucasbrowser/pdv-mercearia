package com.pdv.model;

public enum TipoMovimento {
    ENTRADA("Entrada"), SAIDA("Saída"), AJUSTE("Ajuste (contagem)"), VENDA("Venda"), CANCELAMENTO("Cancelamento de venda");

    private final String rotulo;

    TipoMovimento(String rotulo) { this.rotulo = rotulo; }

    public String getRotulo() { return rotulo; }
}
