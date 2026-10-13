package com.pdv.controller;

import com.pdv.dao.FornecedorDAO;
import com.pdv.model.Fornecedor;
import com.pdv.model.Permissao;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.util.List;

public class FornecedorController {
    private final FornecedorDAO dao = new FornecedorDAO();

    public List<Fornecedor> pesquisar(String termo, boolean apenasAtivos) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        return dao.pesquisar(termo, apenasAtivos);
    }

    public void salvar(long id, String nome, String documento, String contato, String telefone,
                       String email, String endereco, String observacao, boolean ativo) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        if (nome.isBlank()) throw new NegocioException("Informe o nome do fornecedor.");
        if (!email.isBlank() && !email.contains("@")) throw new NegocioException("E-mail inválido.");
        Fornecedor f = new Fornecedor(id, nome.trim(), documento.trim(), contato.trim(), telefone.trim(),
                email.trim(), endereco.trim(), observacao.trim(), ativo);
        if (id == 0) dao.inserir(f); else dao.atualizar(f);
    }
}
