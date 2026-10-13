package com.pdv.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CSV com ';' e BOM UTF-8 (abre direto no Excel em português). */
public final class Csv {
    private Csv() { }

    public static void escrever(Path destino, String[] colunas, List<Object[]> linhas) {
        try (Writer w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(String.join(";", java.util.Arrays.stream(colunas).map(Csv::esc).toList()) + "\r\n");
            for (Object[] l : linhas)
                w.write(String.join(";", java.util.Arrays.stream(l).map(o -> esc(String.valueOf(o))).toList()) + "\r\n");
        } catch (IOException e) {
            throw new NegocioException("Não foi possível gravar o arquivo: " + e.getMessage());
        }
    }

    private static String esc(String s) {
        return s.contains(";") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
