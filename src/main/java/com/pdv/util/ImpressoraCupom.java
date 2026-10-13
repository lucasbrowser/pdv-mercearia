package com.pdv.util;

import com.pdv.model.*;

import javax.print.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.nio.charset.Charset;

/**
 * Cupom NÃO FISCAL em ESC/POS (Epson e compatíveis: Elgin, Bematech, Daruma, Tanca, Epson TM...).
 * Envia bytes brutos via javax.print — não requer bibliotecas externas.
 */
public final class ImpressoraCupom {
    private static final Charset CP850 = Charset.forName("Cp850");

    private ImpressoraCupom() { }

    public static void imprimir(Venda v) { imprimirTexto(montarTexto(v)); }

    /** Envia qualquer texto (cupom, fechamento, comprovante de sangria) para a térmica. */
    public static void imprimirTexto(String texto) {
        if (!Config.getBool("impressora.ativa")) return;
        PrintService servico = localizar();
        if (servico == null)
            throw new NegocioException("Nenhuma impressora encontrada. Ajuste 'impressora.nome' em " + Config.arquivo());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{0x1B, '@'});            // ESC @   : inicializa
            out.write(new byte[]{0x1B, 't', 2});          // ESC t 2 : página de código PC850 (acentos)
            out.write(texto.getBytes(CP850));
            out.write("\n\n\n\n".getBytes(CP850));
            if (Config.getBool("impressora.corte")) out.write(new byte[]{0x1D, 'V', 66, 0}); // GS V B 0 : corte parcial
            servico.createPrintJob().print(new SimpleDoc(out.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null), null);
        } catch (PrintException | java.io.IOException e) {
            throw new NegocioException("Falha ao imprimir: " + e.getMessage());
        }
    }

    private static PrintService localizar() {
        String nome = Config.get("impressora.nome").trim();
        if (nome.isEmpty()) return PrintServiceLookup.lookupDefaultPrintService();
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null))
            if (ps.getName().equalsIgnoreCase(nome) || ps.getName().toLowerCase().contains(nome.toLowerCase())) return ps;
        return null;
    }

    /** Texto do cupom (também usado na visualização em tela). */
    public static String montarTexto(Venda v) {
        int w = Config.getInt("impressora.colunas", 48);
        String tracos = "-".repeat(w) + "\n";
        StringBuilder sb = new StringBuilder();
        centro(sb, Config.get("mercado.nome").toUpperCase(), w);
        centro(sb, Config.get("mercado.endereco"), w);
        if (!Config.get("mercado.cnpj").isBlank()) centro(sb, "CNPJ: " + Config.get("mercado.cnpj"), w);
        if (!Config.get("mercado.telefone").isBlank()) centro(sb, "Tel: " + Config.get("mercado.telefone"), w);
        sb.append(tracos);
        centro(sb, "CUPOM NAO FISCAL", w);
        if ("CANCELADA".equals(v.getStatus())) centro(sb, "*** VENDA CANCELADA ***", w);
        sb.append(tracos);
        sb.append(par("Venda: " + v.getId(), v.getDataHora().format(Fmt.DATA_HORA), w));
        sb.append("Operador: ").append(v.getUsuarioNome()).append('\n');
        sb.append(tracos);
        int n = 1;
        for (ItemVenda i : v.getItens()) {
            sb.append(cortar(String.format("%03d %s", n++, i.getDescricao()), w)).append('\n');
            sb.append(par("    " + Fmt.qtd(i.getQuantidade()) + " " + i.getUnidade() + " x " + Fmt.num(i.getPreco()), Fmt.num(i.getSubtotal()), w));
        }
        sb.append(tracos);
        sb.append(par("SUBTOTAL", Fmt.num(v.getSubtotal()), w));
        if (v.getDesconto().signum() > 0) sb.append(par("DESCONTO", "-" + Fmt.num(v.getDesconto()), w));
        sb.append(par("TOTAL R$", Fmt.num(v.getTotal()), w));
        sb.append(par("Pagamento", v.getFormaPagamento(), w));
        if (FormaPagamento.DINHEIRO.equals(v.getFormaPagamento())) {
            sb.append(par("Valor recebido", Fmt.num(v.getValorRecebido()), w));
            sb.append(par("Troco", Fmt.num(v.getTroco()), w));
        }
        sb.append(tracos);
        centro(sb, "Obrigado pela preferencia!", w);
        centro(sb, "Este cupom nao e documento fiscal", w);
        return sb.toString();
    }

    /** Resumo/fechamento de caixa. */
    public static String montarTextoCaixa(ResumoCaixa r) {
        int w = Config.getInt("impressora.colunas", 48);
        String tracos = "-".repeat(w) + "\n";
        Caixa c = r.caixa();
        StringBuilder sb = new StringBuilder();
        centro(sb, Config.get("mercado.nome").toUpperCase(), w);
        centro(sb, c.aberto() ? "RESUMO DE CAIXA (ABERTO)" : "FECHAMENTO DE CAIXA", w);
        sb.append(tracos);
        sb.append(par("Caixa", "#" + c.id(), w));
        sb.append(par("Abertura", c.dataAbertura().format(Fmt.DATA_HORA), w));
        sb.append("Aberto por: ").append(c.abertoPor()).append('\n');
        if (!c.aberto()) {
            sb.append(par("Fechamento", c.dataFechamento().format(Fmt.DATA_HORA), w));
            sb.append("Fechado por: ").append(c.fechadoPor()).append('\n');
        }
        sb.append(tracos);
        sb.append("VENDAS (").append(r.qtdVendas()).append(" concluidas)\n");
        r.vendasPorForma().forEach((f, v) -> sb.append(par(" " + f, Fmt.num(v), w)));
        sb.append(par("TOTAL DE VENDAS", Fmt.num(r.totalVendas()), w));
        if (r.qtdCanceladas() > 0) sb.append("Vendas canceladas: ").append(r.qtdCanceladas()).append('\n');
        sb.append(tracos);
        sb.append(par("Fundo de troco", Fmt.num(c.valorAbertura()), w));
        sb.append(par("(+) Vendas em dinheiro", Fmt.num(r.vendasPorForma().get(FormaPagamento.DINHEIRO)), w));
        sb.append(par("(+) Suprimentos", Fmt.num(r.suprimentos()), w));
        sb.append(par("(-) Sangrias", Fmt.num(r.sangrias()), w));
        sb.append(par("(-) Pagto de contas", Fmt.num(r.pagamentosContas()), w));
        sb.append(par("= DINHEIRO ESPERADO", Fmt.num(r.esperadoDinheiro()), w));
        if (c.valorContado() != null) {
            sb.append(par("Dinheiro contado", Fmt.num(c.valorContado()), w));
            BigDecimal d = c.diferenca();
            sb.append(par(d.signum() > 0 ? "SOBRA" : d.signum() < 0 ? "FALTA" : "DIFERENCA", (d.signum() > 0 ? "+" : "") + Fmt.num(d), w));
            if (c.observacao() != null && !c.observacao().isBlank()) sb.append("Obs: ").append(c.observacao()).append('\n');
        }
        if (!r.movimentos().isEmpty()) {
            sb.append(tracos).append("MOVIMENTOS DE CAIXA\n");
            for (MovimentoCaixa m : r.movimentos()) {
                sb.append(par(m.dataHora().format(DateTimeFormatter.ofPattern("HH:mm")) + " " + m.tipo().getCurto(),
                        Fmt.num(m.valor()), w));
                sb.append("   ").append(cortar(m.motivo() + " (" + m.usuario() + ")", w - 3)).append('\n');
            }
        }
        if (!c.aberto()) sb.append("\n\n").append("_".repeat(w / 2)).append("\nResponsavel\n");
        return sb.toString();
    }

    /** Comprovante de sangria/suprimento. */
    public static String montarTextoMovimento(Caixa c, TipoMovCaixa tipo, String valor, String motivo, String usuario) {
        int w = Config.getInt("impressora.colunas", 48);
        StringBuilder sb = new StringBuilder();
        centro(sb, Config.get("mercado.nome").toUpperCase(), w);
        centro(sb, "COMPROVANTE DE " + (tipo == TipoMovCaixa.PAGAMENTO_CONTA ? "PAGAMENTO DE CONTA" : tipo.getCurto().toUpperCase()), w);
        sb.append("-".repeat(w)).append('\n');
        sb.append(par("Caixa", "#" + c.id(), w));
        sb.append(par("Data", java.time.LocalDateTime.now().format(Fmt.DATA_HORA), w));
        sb.append(par("Valor R$", valor, w));
        sb.append("Motivo: ").append(motivo).append('\n').append("Operador: ").append(usuario).append('\n');
        sb.append("\n\n").append("_".repeat(w / 2)).append("\nAssinatura\n");
        return sb.toString();
    }

    private static String par(String esq, String dir, int w) {
        return cortar(esq, w - dir.length() - 1) + " ".repeat(Math.max(1, w - esq.length() - dir.length())) + dir + "\n";
    }

    private static void centro(StringBuilder sb, String s, int w) {
        if (s == null || s.isBlank()) return;
        s = cortar(s, w);
        sb.append(" ".repeat(Math.max(0, (w - s.length()) / 2))).append(s).append('\n');
    }

    private static String cortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(0, max));
    }
}
