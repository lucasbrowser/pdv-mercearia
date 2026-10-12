package com.pdv.controller;

import com.pdv.dao.UsuarioDAO;
import com.pdv.model.Usuario;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

public class AuthController {
    private final UsuarioDAO dao = new UsuarioDAO();

    public Usuario entrar(String login, String senha) {
        if (login.isBlank() || senha.isEmpty()) throw new NegocioException("Informe login e senha.");
        Usuario u = dao.autenticar(login.trim().toLowerCase(), senha)
                .orElseThrow(() -> new NegocioException("Login ou senha inválidos."));
        Sessao.iniciar(u);
        return u;
    }

    public void sair() { Sessao.encerrar(); }
}
