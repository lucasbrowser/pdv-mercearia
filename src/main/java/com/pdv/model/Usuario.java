package com.pdv.model;

import java.util.Set;

public record Usuario(long id, String nome, String login, boolean admin, boolean ativo, Set<Permissao> permissoes) {
    /** Administrador possui todas as permissões. */
    public boolean pode(Permissao p) { return ativo && (admin || permissoes.contains(p)); }
}
