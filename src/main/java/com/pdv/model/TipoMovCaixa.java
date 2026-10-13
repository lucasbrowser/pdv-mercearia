package com.pdv.model;

public enum TipoMovCaixa {
    SANGRIA("Sangria (retirada)", "Sangria"),
    SUPRIMENTO("Suprimento (reforço)", "Suprimento"),
    PAGAMENTO_CONTA("Pagamento de conta a pagar", "Pagto conta");

    private final String rotulo;
    private final String curto;

    TipoMovCaixa(String rotulo, String curto) { this.rotulo = rotulo; this.curto = curto; }

    public String getRotulo() { return rotulo; }
    public String getCurto() { return curto; }
}
