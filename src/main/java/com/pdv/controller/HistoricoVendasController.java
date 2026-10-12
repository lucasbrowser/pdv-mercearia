package com.pdv.controller;

import com.pdv.dao.VendaDAO;
import com.pdv.model.Permissao;
import com.pdv.model.Venda;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.time.LocalDate;
import java.util.List;

public class HistoricoVendasController {
    private final VendaDAO dao = new VendaDAO();

    public List<Venda> listar(LocalDate de, LocalDate ate) {
        Sessao.exigir(Permissao.CONSULTAR_VENDAS);
        if (ate.isBefore(de)) throw new NegocioException("A data final deve ser maior ou igual à inicial.");
        return dao.listar(de, ate);
    }

    public Venda buscar(long id) {
        Sessao.exigir(Permissao.CONSULTAR_VENDAS);
        return dao.buscar(id).orElseThrow(() -> new NegocioException("Venda não encontrada."));
    }

    public void cancelar(long id) {
        Sessao.exigir(Permissao.CANCELAR_VENDA);
        dao.cancelar(id, Sessao.atual().id());
    }
}
