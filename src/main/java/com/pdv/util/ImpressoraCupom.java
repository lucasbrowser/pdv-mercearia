package com.pdv.util;

import com.pdv.model.ItemVenda;
import com.pdv.model.Venda;

import javax.print.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Cupom NÃO FISCAL em ESC/POS (Epson e compatíveis: Elgin, Bematech, Daruma, Tanca, Epson TM...).
 * Envia bytes brutos via javax.print — não requer bibliotecas externas.
 */
public final class ImpressoraCupom {
    private static final Charset CP850 = Charset.forName("Cp850");

    private ImpressoraCupom() { }

    public static void imprimir(Venda v) {
        if (!Config.getBool("impressora.ativa")) return;
        PrintService servico = localizar();
        if (servico == null)
            throw new NegocioException("Nenhuma impressora encontrada. Ajuste 'impressora.nome' em " + Config.arquivo());
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{0x1B, '@'});            // ESC @   : inicializa
            out.write(new byte[]{0x1B, 't', 2});          // ESC t 2 : página de código PC850 (acentos)
            out.write(montarTexto(v).getBytes(CP850));
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
            sb.append(par("    " + i.getQuantidade() + " x " + Fmt.num(i.getPreco()), Fmt.num(i.getSubtotal()), w));
        }
        sb.append(tracos);
        sb.append(par("SUBTOTAL", Fmt.num(v.getSubtotal()), w));
        if (v.getDesconto().signum() > 0) sb.append(par("DESCONTO", "-" + Fmt.num(v.getDesconto()), w));
        sb.append(par("TOTAL R$", Fmt.num(v.getTotal()), w));
        sb.append(par("Pagamento", v.getFormaPagamento(), w));
        if ("Dinheiro".equals(v.getFormaPagamento())) {
            sb.append(par("Valor recebido", Fmt.num(v.getValorRecebido()), w));
            sb.append(par("Troco", Fmt.num(v.getTroco()), w));
        }
        sb.append(tracos);
        centro(sb, "Obrigado pela preferencia!", w);
        centro(sb, "Este cupom nao e documento fiscal", w);
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
