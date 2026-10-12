package com.pdv.controller;

import com.pdv.dao.EstoqueDAO;
import com.pdv.dao.ProdutoDAO;
import com.pdv.model.Permissao;
import com.pdv.model.Produto;
import com.pdv.model.TipoMovimento;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ProdutoController {
    private final ProdutoDAO dao = new ProdutoDAO();
    private final EstoqueDAO estoqueDAO = new EstoqueDAO();

    public List<Produto> pesquisar(String termo) {
        Sessao.exigir(Permissao.GERENCIAR_PRODUTOS);
        return dao.pesquisar(termo, false);
    }

    public void salvar(long id, String codigo, String descricao, String unidade, String preco,
                       String estoqueInicial, String minimo, boolean ativo) {
        Sessao.exigir(Permissao.GERENCIAR_PRODUTOS);
        if (codigo.isBlank()) throw new NegocioException("Informe o código de barras.");
        if (descricao.isBlank()) throw new NegocioException("Informe a descrição.");
        BigDecimal valor = Fmt.parse(preco).setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) throw new NegocioException("O preço deve ser maior que zero.");
        int min = Fmt.inteiro(minimo, "estoque mínimo");
        if (min < 0) throw new NegocioException("O estoque mínimo não pode ser negativo.");

        Produto p = new Produto(id, codigo.trim(), descricao.trim(), unidade, valor, 0, min, ativo);
        if (id == 0) {
            int est = Fmt.inteiro(estoqueInicial, "estoque inicial");
            if (est < 0) throw new NegocioException("O estoque inicial não pode ser negativo.");
            long novoId = dao.inserir(p);
            if (est > 0) estoqueDAO.movimentar(novoId, TipoMovimento.ENTRADA, est, "Estoque inicial", Sessao.atual().id());
        } else {
            dao.atualizar(p);
        }
    }
}
