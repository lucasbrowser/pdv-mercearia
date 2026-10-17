package com.pdv.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Ícones vetoriais desenhados em Java2D (sem arquivos de imagem), em qualquer tamanho. */
public final class Icones {
    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static final Color AZUL = new Color(0x1565C0), VERDE = new Color(0x2E7D32), VERMELHO = new Color(0xC62828),
            LARANJA = new Color(0xEF6C00), MARROM = new Color(0x6D4C41), CINZA = new Color(0x455A64);

    private Icones() { }

    public static Icon get(String nome, int tamanho) {
        return CACHE.computeIfAbsent(nome + "@" + tamanho, k -> new ImageIcon(desenhar(nome, tamanho)));
    }

    private static BufferedImage desenhar(String nome, int tam) {
        BufferedImage img = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(tam / 32.0, tam / 32.0);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (nome) {
            case "pesquisar" -> {
                g.setColor(AZUL);
                g.draw(new Ellipse2D.Double(5, 5, 16, 16));
                g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(18, 18, 27, 27));
            }
            case "carrinho" -> {
                g.setColor(AZUL);
                g.draw(poli(false, 2, 5, 7, 5, 10, 21, 25, 21));
                g.fill(poli(true, 8, 9, 29, 9, 25, 18, 10, 18));
                g.setColor(CINZA);
                g.fill(new Ellipse2D.Double(10, 24, 5, 5));
                g.fill(new Ellipse2D.Double(21, 24, 5, 5));
            }
            case "finalizar" -> {
                g.setColor(VERDE);
                g.fill(new Ellipse2D.Double(2, 2, 28, 28));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(poli(false, 9, 17, 14, 22, 23, 11));
            }
            case "cancelar" -> {
                g.setColor(VERMELHO);
                g.fill(new Ellipse2D.Double(2, 2, 28, 28));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(10, 10, 22, 22));
                g.draw(new Line2D.Double(22, 10, 10, 22));
            }
            case "sangria" -> {
                g.setColor(VERMELHO);
                g.draw(new RoundRectangle2D.Double(3, 19, 26, 10, 4, 4));
                g.draw(new Line2D.Double(16, 3, 16, 14));
                g.fill(poli(true, 9, 10, 16, 17, 23, 10));
            }
            case "caixa" -> {
                g.setColor(CINZA);
                g.fill(new RoundRectangle2D.Double(3, 13, 26, 15, 5, 5));
                g.setColor(AZUL);
                g.fill(new RoundRectangle2D.Double(8, 4, 16, 9, 3, 3));
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                g.drawString("$", 12, 25);
            }
            case "produtos" -> {
                g.setColor(new Color(0xBCAAA4));
                g.fill(poli(true, 16, 3, 29, 9, 16, 15, 3, 9));
                g.setColor(MARROM);
                g.fill(poli(true, 3, 9, 16, 15, 16, 29, 3, 23));
                g.setColor(new Color(0x4E342E));
                g.fill(poli(true, 29, 9, 16, 15, 16, 29, 29, 23));
            }
            case "estoque" -> {
                Color[] cores = {AZUL, new Color(0x1E88E5), new Color(0x64B5F6)};
                for (int i = 0; i < 3; i++) {
                    g.setColor(cores[i]);
                    g.fill(new RoundRectangle2D.Double(3, 3 + i * 9.5, 26, 7.5, 3, 3));
                    g.setColor(Color.WHITE);
                    g.fill(new Rectangle2D.Double(7, 5.5 + i * 9.5, 8, 2.5));
                }
            }
            case "vendas" -> {
                g.setColor(Color.WHITE);
                g.fill(poli(true, 6, 3, 26, 3, 26, 29, 22, 26, 19, 29, 16, 26, 13, 29, 10, 26, 6, 29));
                g.setColor(CINZA);
                g.draw(poli(true, 6, 3, 26, 3, 26, 29, 22, 26, 19, 29, 16, 26, 13, 29, 10, 26, 6, 29));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int y : new int[]{9, 14, 19}) g.draw(new Line2D.Double(10, y, 22, y));
            }
            case "fornecedores" -> {
                g.setColor(AZUL);
                g.fill(new RoundRectangle2D.Double(1, 8, 18, 15, 3, 3));
                g.setColor(new Color(0x0D47A1));
                g.fill(poli(true, 20, 13, 26, 13, 31, 18, 31, 23, 20, 23));
                g.setColor(new Color(0x263238));
                g.fill(new Ellipse2D.Double(5, 20, 7, 7));
                g.fill(new Ellipse2D.Double(21, 20, 7, 7));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(7, 22, 3, 3));
                g.fill(new Ellipse2D.Double(23, 22, 3, 3));
            }
            case "contas" -> {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(3, 5, 26, 24, 5, 5));
                g.setColor(VERMELHO);
                g.fill(new RoundRectangle2D.Double(3, 5, 26, 8, 5, 5));
                g.fill(new Rectangle2D.Double(3, 9, 26, 4));
                g.draw(new RoundRectangle2D.Double(3, 5, 26, 24, 5, 5));
                g.setColor(CINZA);
                for (int r = 0; r < 2; r++)
                    for (int c = 0; c < 3; c++) g.fill(new Rectangle2D.Double(7 + c * 7.5, 16 + r * 6.5, 4.5, 4));
            }
            case "relatorios" -> {
                g.setColor(AZUL);
                g.fill(new Rectangle2D.Double(4, 18, 6, 11));
                g.setColor(VERDE);
                g.fill(new Rectangle2D.Double(13, 11, 6, 18));
                g.setColor(LARANJA);
                g.fill(new Rectangle2D.Double(22, 4, 6, 25));
                g.setColor(CINZA);
                g.draw(new Line2D.Double(2, 30, 30, 30));
            }
            case "usuarios" -> {
                g.setColor(AZUL);
                g.fill(new Ellipse2D.Double(10, 3, 12, 12));
                g.setClip(0, 0, 32, 32);
                g.fill(new Ellipse2D.Double(4, 18, 24, 22));
            }
            case "lembretes" -> {
                g.setColor(MARROM);
                g.fill(new RoundRectangle2D.Double(5, 5, 22, 26, 5, 5));
                g.setColor(new Color(0xFFFDE7));
                g.fill(new Rectangle2D.Double(8, 10, 16, 18));
                g.setColor(CINZA);
                g.fill(new RoundRectangle2D.Double(11, 2, 10, 7, 3, 3));
                g.setColor(LARANJA);
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int y : new int[]{15, 20, 25}) g.draw(new Line2D.Double(11, y, 21, y));
            }
            case "novo" -> {
                g.setColor(VERDE);
                g.fill(new Ellipse2D.Double(2, 2, 28, 28));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(16, 9, 16, 23));
                g.draw(new Line2D.Double(9, 16, 23, 16));
            }
            case "editar" -> {
                g.setColor(LARANJA);
                g.fill(poli(true, 8, 21, 21, 8, 26, 13, 13, 26));
                g.setColor(new Color(0x5D4037));
                g.fill(poli(true, 8, 21, 13, 26, 4, 29));
                g.setColor(new Color(0xBDBDBD));
                g.fill(poli(true, 21, 8, 24, 5, 27, 8, 26, 13));
            }
            case "salvar" -> {
                g.setColor(AZUL);
                g.fill(new RoundRectangle2D.Double(3, 3, 26, 26, 5, 5));
                g.setColor(Color.WHITE);
                g.fill(new Rectangle2D.Double(9, 3, 14, 9));
                g.fill(new RoundRectangle2D.Double(8, 17, 16, 12, 2, 2));
                g.setColor(CINZA);
                g.fill(new Rectangle2D.Double(18, 5, 3, 5));
            }
            case "imprimir" -> {
                g.setColor(Color.WHITE);
                g.fill(new Rectangle2D.Double(9, 3, 14, 10));
                g.setColor(CINZA);
                g.draw(new Rectangle2D.Double(9, 3, 14, 10));
                g.fill(new RoundRectangle2D.Double(3, 12, 26, 12, 4, 4));
                g.setColor(Color.WHITE);
                g.fill(new Rectangle2D.Double(9, 19, 14, 10));
                g.setColor(CINZA);
                g.draw(new Rectangle2D.Double(9, 19, 14, 10));
                g.setColor(VERDE);
                g.fill(new Ellipse2D.Double(22, 15, 3, 3));
            }
            case "exportar" -> {
                g.setColor(AZUL);
                g.draw(poli(false, 5, 17, 5, 27, 27, 27, 27, 17));
                g.draw(new Line2D.Double(16, 22, 16, 5));
                g.fill(poli(true, 9, 12, 16, 4, 23, 12));
            }
            case "filtrar" -> {
                g.setColor(AZUL);
                g.fill(poli(true, 3, 5, 29, 5, 19, 17, 19, 28, 13, 25, 13, 17));
            }
            case "ver" -> {
                g.setColor(CINZA);
                g.draw(new Ellipse2D.Double(2, 8, 28, 16));
                g.setColor(AZUL);
                g.fill(new Ellipse2D.Double(11, 11, 10, 10));
            }
            case "entrada" -> {
                g.setColor(VERDE);
                g.draw(poli(false, 5, 18, 5, 27, 27, 27, 27, 18));
                g.draw(new Line2D.Double(16, 4, 16, 19));
                g.fill(poli(true, 9, 13, 16, 21, 23, 13));
            }
            case "saida" -> {
                g.setColor(VERMELHO);
                g.draw(poli(false, 5, 17, 5, 27, 27, 27, 27, 17));
                g.draw(new Line2D.Double(16, 22, 16, 6));
                g.fill(poli(true, 9, 12, 16, 4, 23, 12));
            }
            case "ajuste" -> {
                g.setColor(CINZA);
                for (int y : new int[]{8, 16, 24}) g.draw(new Line2D.Double(4, y, 28, y));
                g.setColor(AZUL);
                g.fill(new Ellipse2D.Double(6, 4.5, 7, 7));
                g.fill(new Ellipse2D.Double(19, 12.5, 7, 7));
                g.fill(new Ellipse2D.Double(10, 20.5, 7, 7));
            }
            case "cadeado" -> {
                g.setColor(LARANJA);
                g.draw(new Arc2D.Double(10, 3, 12, 14, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(10, 10, 10, 15));
                g.draw(new Line2D.Double(22, 10, 22, 15));
                g.fill(new RoundRectangle2D.Double(5, 14, 22, 15, 4, 4));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(14, 19, 4, 4));
            }
            case "abrir" -> {
                g.setColor(VERDE);
                g.draw(new Arc2D.Double(14, 1, 12, 14, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(26, 8, 26, 14));
                g.fill(new RoundRectangle2D.Double(5, 14, 22, 15, 4, 4));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(14, 19, 4, 4));
            }
            case "pagar" -> {
                g.setColor(new Color(0xF9A825));
                g.fill(new Ellipse2D.Double(2, 2, 28, 28));
                g.setColor(new Color(0xE65100));
                g.draw(new Ellipse2D.Double(4.5, 4.5, 23, 23));
                g.setColor(new Color(0x5D4037));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                g.drawString("$", 11, 22);
            }
            default -> {
                g.setColor(CINZA);
                g.draw(new Ellipse2D.Double(3, 3, 26, 26));
            }
        }
        g.dispose();
        return img;
    }

    private static Path2D poli(boolean fechar, double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        if (fechar) path.closePath();
        return path;
    }
}
