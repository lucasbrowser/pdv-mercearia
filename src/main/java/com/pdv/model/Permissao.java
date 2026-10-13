package com.pdv.model;

public enum Permissao {
    VENDER("Operar o caixa (vendas)"),
    CONCEDER_DESCONTO("Conceder descontos"),
    CONSULTAR_VENDAS("Consultar histórico de vendas"),
    CANCELAR_VENDA("Cancelar vendas finalizadas"),
    GERENCIAR_CAIXA("Abrir/fechar caixa, sangria e suprimento"),
    VER_RELATORIOS("Consultar relatórios"),
    GERENCIAR_PRODUTOS("Cadastrar e editar produtos"),
    GERENCIAR_ESTOQUE("Administrar estoque"),
    GERENCIAR_USUARIOS("Gerenciar usuários");

    private final String descricao;

    Permissao(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
