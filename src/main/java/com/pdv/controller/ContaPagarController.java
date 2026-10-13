package com.pdv.controller;

import com.pdv.dao.CaixaDAO;
import com.pdv.dao.ContaPagarDAO;
import com.pdv.model.*;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Contas a pagar, parcelamento, lembretes de vencimento e pagamento (inclusive com dinheiro do caixa). */
public class ContaPagarController {
    public enum Filtro {
        ABERTAS("Em aberto"), VENCIDAS("Vencidas"), PAGAS("Pagas"), CANCELADAS("Canceladas"), TODAS("Todas");

        private final String rotulo;

        Filtro(String rotulo) { this.rotulo = rotulo; }

        @Override public String toString() { return rotulo; }
    }

    private final ContaPagarDAO dao = new ContaPagarDAO();
    private final CaixaDAO caixaDAO = new CaixaDAO();

    public List<ContaPagar> listar(LocalDate de, LocalDate ate, Filtro filtro) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        if (ate.isBefore(de)) throw new NegocioException("A data final deve ser maior ou igual à inicial.");
        LocalDate hoje = LocalDate.now();
        return dao.listar(de, ate).stream().filter(c -> switch (filtro) {
            case ABERTAS -> c.aberta();
            case VENCIDAS -> c.situacao(hoje) == ContaPagar.Situacao.VENCIDA;
            case PAGAS -> c.situacao(hoje) == ContaPagar.Situacao.PAGA;
            case CANCELADAS -> c.situacao(hoje) == ContaPagar.Situacao.CANCELADA;
            case TODAS -> true;
        }).toList();
    }

    /** Cria 1..60 parcelas mensais; a diferença de centavos fica na primeira parcela. */
    public void criar(long fornecedorId, String descricao, String documento, String valor, String vencimento,
                      String parcelas, String lembreteDias, String observacao) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        BigDecimal total = validarValor(valor);
        LocalDate venc = Fmt.data(vencimento);
        int n = Fmt.inteiro(parcelas, "parcelas");
        if (n < 1 || n > 60) throw new NegocioException("O número de parcelas deve estar entre 1 e 60.");
        if (descricao.isBlank()) throw new NegocioException("Informe a descrição da conta.");
        int lembrete = lembrete(lembreteDias);
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal primeira = total.subtract(base.multiply(BigDecimal.valueOf(n - 1)));
        List<ContaPagar> contas = new ArrayList<>();
        for (int i = 0; i < n; i++)
            contas.add(new ContaPagar(0, fornecedorId, "", n > 1 ? descricao.trim() + " (" + (i + 1) + "/" + n + ")" : descricao.trim(),
                    documento.trim(), i == 0 ? primeira : base, venc.plusMonths(i), "ABERTA", null, null, "",
                    observacao.trim(), lembrete));
        dao.inserir(contas);
    }

    public void atualizar(long id, long fornecedorId, String descricao, String documento, String valor,
                          String vencimento, String lembreteDias, String observacao) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        if (descricao.isBlank()) throw new NegocioException("Informe a descrição da conta.");
        dao.atualizar(new ContaPagar(id, fornecedorId, "", descricao.trim(), documento.trim(), validarValor(valor),
                Fmt.data(vencimento), "ABERTA", null, null, "", observacao.trim(), lembrete(lembreteDias)));
    }

    public void cancelar(long id) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        dao.cancelar(id);
    }

    /** Dinheiro disponível na gaveta, se houver caixa aberto. */
    public Optional<BigDecimal> dinheiroNoCaixa() {
        return caixaDAO.buscarAberto().map(c -> caixaDAO.resumir(c.id()).esperadoDinheiro());
    }

    /**
     * Paga a conta. Com "Dinheiro do caixa" exige caixa aberto e saldo na gaveta, e registra a saída no caixa.
     * Retorna o caixa afetado (para imprimir o comprovante) ou vazio se o pagamento foi por outro meio.
     */
    public Optional<Caixa> pagar(long contaId, String valorPago, String forma, String observacao) {
        Sessao.exigir(Permissao.GERENCIAR_CONTAS);
        ContaPagar conta = dao.buscar(contaId).orElseThrow(() -> new NegocioException("Conta não encontrada."));
        if (!conta.aberta()) throw new NegocioException("Esta conta não está em aberto.");
        if (!ContaPagar.FORMAS.contains(forma)) throw new NegocioException("Forma de pagamento inválida.");
        BigDecimal pago = validarValor(valorPago);
        Caixa caixa = null;
        if (ContaPagar.FORMA_CAIXA.equals(forma)) {
            Sessao.exigir(Permissao.GERENCIAR_CAIXA);
            caixa = caixaDAO.buscarAberto().orElseThrow(() -> new NegocioException("Não há caixa aberto para retirar o dinheiro."));
            BigDecimal disponivel = caixaDAO.resumir(caixa.id()).esperadoDinheiro();
            if (pago.compareTo(disponivel) > 0)
                throw new NegocioException("Dinheiro insuficiente na gaveta (disponível: " + Fmt.moeda(disponivel) + ").");
        }
        String quem = conta.fornecedorNome().isBlank() ? "" : " - " + conta.fornecedorNome();
        dao.pagar(contaId, pago, forma, Sessao.atual().id(), caixa == null ? null : caixa.id(), observacao,
                "Conta #" + contaId + " " + conta.descricao() + quem);
        return Optional.ofNullable(caixa);
    }

    /** Contas em aberto cujo prazo de aviso chegou (inclui vencidas). Vazio se o usuário não tem a permissão. */
    public List<ContaPagar> lembretes() {
        if (!Sessao.pode(Permissao.GERENCIAR_CONTAS)) return List.of();
        LocalDate hoje = LocalDate.now();
        return dao.abertasAte(hoje.plusDays(30)).stream().filter(c -> c.diasParaVencer(hoje) <= c.lembreteDias()).toList();
    }

    private static int lembrete(String s) {
        int d = Fmt.inteiro(s, "dias de aviso");
        if (d < 0 || d > 30) throw new NegocioException("Os dias de aviso devem estar entre 0 e 30.");
        return d;
    }

    private static BigDecimal validarValor(String s) {
        BigDecimal v = Fmt.parse(s).setScale(2, RoundingMode.HALF_UP);
        if (v.signum() <= 0) throw new NegocioException("O valor deve ser maior que zero.");
        return v;
    }
}
