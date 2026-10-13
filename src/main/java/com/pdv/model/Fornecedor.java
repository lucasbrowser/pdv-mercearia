package com.pdv.model;

public record Fornecedor(long id, String nome, String documento, String contato, String telefone,
                         String email, String endereco, String observacao, boolean ativo) {
    /** Item "sem fornecedor" para combos. */
    public static final Fornecedor SEM = new Fornecedor(0, "(sem fornecedor)", "", "", "", "", "", "", true);

    @Override public String toString() { return nome; }
}
