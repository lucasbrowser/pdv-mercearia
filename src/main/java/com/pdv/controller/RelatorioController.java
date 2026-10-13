package com.pdv.controller;

import com.pdv.dao.CaixaDAO;
import com.pdv.dao.ProdutoDAO;
import com.pdv.dao.VendaDAO;
import com.pdv.model.*;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

public class RelatorioController {
    /** Tabela já formatada (texto) para exibir e exportar. */
    public record Relatorio(String titulo, String[] colunas, List<Object[]> linhas, String rodape) { }

    private static final class Acc {
        int n;
        BigDecimal total = BigDecimal.ZERO;
        void add(BigDecimal t) { n++; total = total.add(t); }
    }

    private final VendaDAO vendaDAO = new VendaDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final CaixaDAO caixaDAO = new CaixaDAO();

    public Relatorio gerar(TipoRelatorio tipo, LocalDate de, LocalDate ate) {
        Sessao.exigir(Permissao.VER_RELATORIOS);
        if (tipo.usaPeriodo() && ate.isBefore(de)) throw new NegocioException("A data final deve ser maior ou igual à inicial.");
        return switch (tipo) {
            case VENDAS_DIA -> vendas(tipo, de, ate, v -> v.getDataHora().toLocalDate().toString(),
                    k -> LocalDate.parse(k).format(Fmt.DATA), "Data");
            case FORMA_PAGAMENTO -> vendas(tipo, de, ate, Venda::getFormaPagamento, k -> k, "Forma de pagamento");
            case OPERADOR -> vendas(tipo, de, ate, Venda::getUsuarioNome, k -> k, "Operador");
            case MAIS_VENDIDOS -> maisVendidos(de, ate);
            case ESTOQUE -> estoque();
            case FECHAMENTOS -> fechamentos(de, ate);
        };
    }

    private Relatorio vendas(TipoRelatorio tipo, LocalDate de, LocalDate ate, Function<Venda, String> chave,
                             Function<String, String> rotulo, String colunaChave) {
        List<Venda> todas = vendaDAO.listar(de, ate);
        Map<String, Acc> grupos = new TreeMap<>();
        Acc geral = new Acc();
        int canceladas = 0;
        for (Venda v : todas) {
            if (!"CONCLUIDA".equals(v.getStatus())) { canceladas++; continue; }
            grupos.computeIfAbsent(chave.apply(v), k -> new Acc()).add(v.getTotal());
            geral.add(v.getTotal());
        }
        List<Object[]> linhas = new ArrayList<>();
        grupos.forEach((k, a) -> linhas.add(new Object[]{rotulo.apply(k), a.n, Fmt.num(a.total), Fmt.num(media(a.total, a.n))}));
        String rodape = "Total: " + geral.n + " venda(s) = " + Fmt.moeda(geral.total) + "  |  Ticket médio: "
                + Fmt.moeda(media(geral.total, geral.n)) + "  |  Canceladas (fora do total): " + canceladas;
        return new Relatorio(tipo + " (" + de.format(Fmt.DATA) + " a " + ate.format(Fmt.DATA) + ")",
                new String[]{colunaChave, "Vendas", "Total (R$)", "Ticket médio (R$)"}, linhas, rodape);
    }

    private Relatorio maisVendidos(LocalDate de, LocalDate ate) {
        Map<Long, Object[]> mapa = new LinkedHashMap<>(); // id -> {descricao, unidade, qtd, total}
        BigDecimal geral = BigDecimal.ZERO;
        for (ItemVendido i : vendaDAO.itensVendidos(de, ate)) {
            Object[] a = mapa.computeIfAbsent(i.produtoId(), k -> new Object[]{i.descricao(), i.unidade(), BigDecimal.ZERO, BigDecimal.ZERO});
            a[2] = ((BigDecimal) a[2]).add(i.quantidade());
            a[3] = ((BigDecimal) a[3]).add(i.total());
            geral = geral.add(i.total());
        }
        List<Object[]> ordenado = new ArrayList<>(mapa.values());
        ordenado.sort((x, y) -> ((BigDecimal) y[3]).compareTo((BigDecimal) x[3]));
        List<Object[]> linhas = new ArrayList<>();
        int pos = 1;
        for (Object[] a : ordenado.subList(0, Math.min(100, ordenado.size()))) {
            BigDecimal pct = geral.signum() == 0 ? BigDecimal.ZERO
                    : ((BigDecimal) a[3]).multiply(BigDecimal.valueOf(100)).divide(geral, 1, RoundingMode.HALF_UP);
            linhas.add(new Object[]{pos++, a[0], a[1], Fmt.qtd((BigDecimal) a[2]), Fmt.num((BigDecimal) a[3]),
                    pct.toPlainString().replace('.', ',') + "%"});
        }
        return new Relatorio("Produtos mais vendidos (" + de.format(Fmt.DATA) + " a " + ate.format(Fmt.DATA) + ")",
                new String[]{"#", "Produto", "Un.", "Qtd vendida", "Receita (R$)", "% da receita"}, linhas,
                "Receita total no período: " + Fmt.moeda(geral) + "  |  Exibindo até 100 produtos por receita");
    }

    private Relatorio estoque() {
        List<Object[]> linhas = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;
        int baixos = 0;
        for (Produto p : produtoDAO.pesquisar("", true, 100_000)) {
            BigDecimal valor = p.preco().multiply(p.estoque()).setScale(2, RoundingMode.HALF_UP);
            valorTotal = valorTotal.add(valor);
            String situacao = p.estoque().signum() == 0 ? "ZERADO" : p.baixoEstoque() ? "BAIXO" : "OK";
            if (!situacao.equals("OK")) baixos++;
            linhas.add(new Object[]{p.codigoBarras(), p.descricao(), p.unidade(), Fmt.qtd(p.estoque()),
                    Fmt.qtd(p.estoqueMinimo()), Fmt.num(p.preco()), Fmt.num(valor), situacao});
        }
        return new Relatorio("Posição de estoque em " + LocalDate.now().format(Fmt.DATA),
                new String[]{"Código", "Produto", "Un.", "Estoque", "Mínimo", "Preço (R$)", "Valor em estoque (R$)", "Situação"},
                linhas, "Valor total em estoque (a preço de venda): " + Fmt.moeda(valorTotal) + "  |  Produtos com estoque baixo/zerado: " + baixos);
    }

    private Relatorio fechamentos(LocalDate de, LocalDate ate) {
        List<Object[]> linhas = new ArrayList<>();
        BigDecimal vendas = BigDecimal.ZERO, difs = BigDecimal.ZERO;
        for (Caixa c : caixaDAO.listar(de, ate)) {
            ResumoCaixa r = caixaDAO.resumir(c.id());
            vendas = vendas.add(r.totalVendas());
            if (c.diferenca() != null) difs = difs.add(c.diferenca());
            linhas.add(new Object[]{c.id(), c.dataAbertura().format(Fmt.DATA_HORA), c.abertoPor(),
                    c.dataFechamento() == null ? "-" : c.dataFechamento().format(Fmt.DATA_HORA), Fmt.num(c.valorAbertura()),
                    Fmt.num(r.totalVendas()), Fmt.num(r.sangrias()),
                    c.valorEsperado() == null ? Fmt.num(r.esperadoDinheiro()) : Fmt.num(c.valorEsperado()),
                    c.valorContado() == null ? "-" : Fmt.num(c.valorContado()),
                    c.diferenca() == null ? "-" : Fmt.num(c.diferenca()), c.aberto() ? "ABERTO" : "FECHADO"});
        }
        return new Relatorio("Fechamentos de caixa (" + de.format(Fmt.DATA) + " a " + ate.format(Fmt.DATA) + ")",
                new String[]{"Caixa", "Abertura", "Aberto por", "Fechamento", "Fundo (R$)", "Vendas (R$)", "Sangrias (R$)",
                        "Esperado (R$)", "Contado (R$)", "Diferença (R$)", "Status"},
                linhas, "Vendas no período: " + Fmt.moeda(vendas) + "  |  Soma das diferenças (sobras/faltas): " + Fmt.moeda(difs));
    }

    private static BigDecimal media(BigDecimal total, int n) {
        return n == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }
}
