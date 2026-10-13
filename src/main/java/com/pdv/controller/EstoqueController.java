package com.pdv.controller;

import com.pdv.dao.EstoqueDAO;
import com.pdv.dao.ProdutoDAO;
import com.pdv.model.MovimentoEstoque;
import com.pdv.model.Permissao;
import com.pdv.model.Produto;
import com.pdv.model.TipoMovimento;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.math.BigDecimal;
import java.util.List;

public class EstoqueController {
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final EstoqueDAO estoqueDAO = new EstoqueDAO();

    public List<Produto> listar(String termo, boolean somenteAbaixoDoMinimo) {
        Sessao.exigir(Permissao.GERENCIAR_ESTOQUE);
        List<Produto> lista = produtoDAO.pesquisar(termo, true);
        return somenteAbaixoDoMinimo ? lista.stream().filter(Produto::baixoEstoque).toList() : lista;
    }

    public void movimentar(Produto p, TipoMovimento tipo, String quantidade, String motivo) {
        Sessao.exigir(Permissao.GERENCIAR_ESTOQUE);
        if (tipo != TipoMovimento.ENTRADA && tipo != TipoMovimento.SAIDA && tipo != TipoMovimento.AJUSTE)
            throw new NegocioException("Tipo de movimentação inválido.");
        BigDecimal qtd = Fmt.quantidade(quantidade, "quantidade", p.fracionado());
        if ((tipo != TipoMovimento.ENTRADA) && motivo.isBlank())
            throw new NegocioException("Informe o motivo da " + tipo.getRotulo().toLowerCase() + ".");
        estoqueDAO.movimentar(p.id(), tipo, qtd, motivo.isBlank() ? "Reposição" : motivo.trim(), Sessao.atual().id());
    }

    public List<MovimentoEstoque> historico() {
        Sessao.exigir(Permissao.GERENCIAR_ESTOQUE);
        return estoqueDAO.historico(300);
    }
}
