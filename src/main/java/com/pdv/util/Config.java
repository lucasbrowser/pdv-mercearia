package com.pdv.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configurações em ~/pdv-mercado/config.properties (criado automaticamente). */
public final class Config {
    private static final Path DIR = Path.of(System.getProperty("user.home"), "pdv-mercado");
    private static final Path ARQUIVO = DIR.resolve("config.properties");
    private static final Properties P = new Properties();

    static {
        P.setProperty("mercado.nome", "MERCADO DO BAIRRO");
        P.setProperty("mercado.cnpj", "00.000.000/0000-00");
        P.setProperty("mercado.endereco", "Rua Exemplo, 100 - Centro");
        P.setProperty("mercado.telefone", "(11) 0000-0000");
        P.setProperty("balanca.etiqueta", "true"); // aceita etiqueta EAN-13 de balança: 2 CCCC PPPPP D (peso em gramas)
        P.setProperty("balanca.prefixo", "2");
        P.setProperty("impressora.ativa", "true");
        P.setProperty("impressora.nome", "");      // vazio = impressora padrão do sistema
        P.setProperty("impressora.colunas", "48"); // 48 = bobina 80mm | 32 = bobina 58mm
        P.setProperty("impressora.corte", "true");
        try {
            Files.createDirectories(DIR);
            if (Files.exists(ARQUIVO)) {
                try (var in = Files.newBufferedReader(ARQUIVO)) { P.load(in); }
            } else {
                try (var out = Files.newBufferedWriter(ARQUIVO)) { P.store(out, "Configuracoes do PDV"); }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Config() { }

    public static Path diretorio() { return DIR; }
    public static Path arquivo() { return ARQUIVO; }
    public static String get(String chave) { return P.getProperty(chave, ""); }
    public static boolean getBool(String chave) { return Boolean.parseBoolean(get(chave).trim()); }

    public static int getInt(String chave, int padrao) {
        try { return Integer.parseInt(get(chave).trim()); } catch (NumberFormatException e) { return padrao; }
    }
}
