package com.pdv.controller;

import com.pdv.model.Produto;
import com.pdv.util.NegocioException;

/** Produto vendido por peso foi lido sem quantidade: a tela deve pedir o peso ao operador. */
public class PesoObrigatorioException extends NegocioException {
    private final Produto produto;

    public PesoObrigatorioException(Produto produto) {
        super("Informe o peso de " + produto.descricao() + ".");
        this.produto = produto;
    }

    public Produto getProduto() { return produto; }
}
