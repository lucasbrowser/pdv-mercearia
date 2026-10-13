package com.pdv.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {
    private static final Locale BR = Locale.of("pt", "BR");
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fmt() { }

    public static String num(BigDecimal v) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BR)).format(v);
    }

    public static String moeda(BigDecimal v) { return "R$ " + num(v); }

    /** Quantidade: inteira quando não há fração ("3"), com 3 casas quando fracionada ("0,350"). */
    public static String qtd(BigDecimal q) {
        if (q.stripTrailingZeros().scale() <= 0) return q.setScale(0, RoundingMode.DOWN).toPlainString();
        return new DecimalFormat("#,##0.000", DecimalFormatSymbols.getInstance(BR)).format(q);
    }

    /** Aceita "12,50", "1.234,56", "12.5" e "R$ 3,00". */
    public static BigDecimal parse(String s) {
        if (s == null || s.isBlank()) throw new NegocioException("Informe um valor numérico.");
        String t = s.trim().replace("R$", "").replace(" ", "");
        if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
        try { return new BigDecimal(t); }
        catch (NumberFormatException e) { throw new NegocioException("Valor inválido: " + s); }
    }

    /** Quantidade com até 3 casas (gramas). Se !permiteDecimal, exige número inteiro. */
    public static BigDecimal quantidade(String s, String campo, boolean permiteDecimal) {
        BigDecimal q = parse(s);
        int casas = q.stripTrailingZeros().scale();
        if (casas > 3) throw new NegocioException("Use no máximo 3 casas decimais em " + campo + ".");
        if (!permiteDecimal && casas > 0)
            throw new NegocioException("Este produto é vendido por unidade: informe " + campo + " inteira.");
        return q.setScale(3, RoundingMode.HALF_UP);
    }

    public static int inteiro(String s, String campo) {
        try { return Integer.parseInt(s.trim()); }
        catch (RuntimeException e) { throw new NegocioException("Valor inválido para " + campo + "."); }
    }
}
