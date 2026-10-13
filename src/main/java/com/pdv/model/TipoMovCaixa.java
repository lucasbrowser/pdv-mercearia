package com.pdv.model;

public enum TipoMovCaixa {
    SANGRIA("Sangria (retirada)"), SUPRIMENTO("Suprimento (reforço)");

    private final String rotulo;

    TipoMovCaixa(String rotulo) { this.rotulo = rotulo; }

    public String getRotulo() { return rotulo; }
}
