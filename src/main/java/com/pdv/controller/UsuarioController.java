package com.pdv.controller;

import com.pdv.dao.UsuarioDAO;
import com.pdv.model.Permissao;
import com.pdv.model.Usuario;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class UsuarioController {
    private final UsuarioDAO dao = new UsuarioDAO();

    public List<Usuario> listar() {
        Sessao.exigir(Permissao.GERENCIAR_USUARIOS);
        return dao.listar();
    }

    public void salvar(long id, String nome, String login, String senha, boolean admin, boolean ativo, Set<Permissao> permissoes) {
        Sessao.exigir(Permissao.GERENCIAR_USUARIOS);
        Usuario logado = Sessao.atual();
        if (nome.isBlank() || login.isBlank()) throw new NegocioException("Informe nome e login.");
        if ((id == 0 || !senha.isEmpty()) && senha.length() < 4)
            throw new NegocioException("A senha deve ter ao menos 4 caracteres.");
        if (id == logado.id() && (!ativo || (logado.admin() && !admin)))
            throw new NegocioException("Você não pode desativar nem rebaixar o seu próprio usuário.");
        if (!logado.admin()) { // evita escalonamento de privilégio
            if (admin) throw new NegocioException("Somente administradores podem conceder perfil de administrador.");
            if (id != 0 && dao.listar().stream().anyMatch(u -> u.id() == id && u.admin()))
                throw new NegocioException("Somente administradores podem alterar outro administrador.");
        }
        Set<Permissao> perms = permissoes.isEmpty() ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(permissoes);
        Usuario u = new Usuario(id, nome.trim(), login.trim().toLowerCase(), admin, ativo, perms);
        if (id == 0) dao.inserir(u, senha); else dao.atualizar(u, senha);
    }
}
