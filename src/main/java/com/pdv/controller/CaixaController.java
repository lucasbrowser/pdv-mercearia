package com.pdv.controller;

import com.pdv.dao.CaixaDAO;
import com.pdv.model.*;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Abertura, sangria/suprimento e fechamento de caixa. */
public class CaixaController {
    private final CaixaDAO dao = new CaixaDAO();

    public Optional<Caixa> caixaAberto() { return dao.buscarAberto(); }

    public Caixa abrir(String fundoTroco) {
        Sessao.exigir(Permissao.GERENCIAR_CAIXA);
        BigDecimal valor = Fmt.parse(fundoTroco.isBlank() ? "0" : fundoTroco).setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() < 0) throw new NegocioException("O fundo de troco não pode ser negativo.");
        return dao.buscar(dao.abrir(valor, Sessao.atual().id())).orElseThrow();
    }

    public ResumoCaixa resumoAberto() {
        Caixa c = dao.buscarAberto().orElseThrow(() -> new NegocioException("Não há caixa aberto."));
        return dao.resumir(c.id());
    }

    public ResumoCaixa resumo(long caixaId) { return dao.resumir(caixaId); }

    /** Retorna o movimento registrado (para o comprovante). */
    public Caixa movimentar(TipoMovCaixa tipo, String valorTexto, String motivo) {
        Sessao.exigir(Permissao.GERENCIAR_CAIXA);
        if (tipo == TipoMovCaixa.PAGAMENTO_CONTA)
            throw new NegocioException("Pagamentos de contas são feitos em Contas a pagar.");
        BigDecimal valor = Fmt.parse(valorTexto).setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) throw new NegocioException("Informe um valor maior que zero.");
        if (motivo.isBlank()) throw new NegocioException("Informe o motivo da " + tipo.getRotulo().toLowerCase() + ".");
        ResumoCaixa r = resumoAberto();
        if (tipo == TipoMovCaixa.SANGRIA && valor.compareTo(r.esperadoDinheiro()) > 0)
            throw new NegocioException("Sangria maior que o dinheiro disponível na gaveta (" + Fmt.moeda(r.esperadoDinheiro()) + ").");
        dao.registrarMovimento(r.caixa().id(), tipo, valor, motivo.trim(), Sessao.atual().id());
        return r.caixa();
    }

    /** Fecha o caixa comparando o dinheiro contado com o esperado. */
    public ResumoCaixa fechar(String contadoTexto, String observacao) {
        Sessao.exigir(Permissao.GERENCIAR_CAIXA);
        ResumoCaixa r = resumoAberto();
        BigDecimal contado = Fmt.parse(contadoTexto).setScale(2, RoundingMode.HALF_UP);
        if (contado.signum() < 0) throw new NegocioException("O valor contado não pode ser negativo.");
        BigDecimal dif = contado.subtract(r.esperadoDinheiro());
        if (dif.signum() != 0 && observacao.isBlank())
            throw new NegocioException("Há diferença no caixa (" + Fmt.moeda(dif) + "). Informe uma observação.");
        dao.fechar(r.caixa().id(), r.esperadoDinheiro(), contado, observacao.trim(), Sessao.atual().id());
        return dao.resumir(r.caixa().id());
    }
}
