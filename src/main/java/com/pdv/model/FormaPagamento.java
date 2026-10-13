package com.pdv.model;

import java.util.List;

public final class FormaPagamento {
    public static final String DINHEIRO = "Dinheiro";
    public static final List<String> TODAS = List.of(DINHEIRO, "Cartão de Débito", "Cartão de Crédito", "PIX");

    private FormaPagamento() { }
}
