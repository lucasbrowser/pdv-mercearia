package com.pdv.util;

import com.pdv.model.Permissao;
import com.pdv.model.Usuario;

/** Usuário logado e verificação de permissões. */
public final class Sessao {
    private static Usuario atual;

    private Sessao() { }

    public static void iniciar(Usuario u) { atual = u; }
    public static void encerrar() { atual = null; }
    public static Usuario atual() { return atual; }
    public static boolean pode(Permissao p) { return atual != null && atual.pode(p); }

    public static void exigir(Permissao p) {
        if (!pode(p))
            throw new NegocioException("Acesso negado: seu usuário não possui a permissão \"" + p.getDescricao() + "\".");
    }
}
