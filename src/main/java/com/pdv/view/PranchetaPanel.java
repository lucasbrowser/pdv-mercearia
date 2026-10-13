package com.pdv.view;

import javax.swing.*;
import java.awt.*;

/** Painel em forma de prancheta (clipboard): fundo de madeira, presilha metálica e folha de papel. */
public class PranchetaPanel extends JPanel {

    public PranchetaPanel(JComponent papel) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(56, 22, 22, 22));
        papel.setOpaque(true);
        papel.setBackground(new Color(0xFFFDE7));
        papel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD7CCC8)), BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        add(papel, BorderLayout.CENTER);
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(0x8D6E63));
        g.fillRoundRect(0, 14, w, h - 14, 30, 30);
        g.setColor(new Color(0x5D4037));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(1, 15, w - 3, h - 17, 30, 30);
        int cw = 170, cx = (w - cw) / 2;
        g.setColor(new Color(0x9E9E9E));
        g.fillRoundRect(cx, 0, cw, 50, 16, 16);
        g.setColor(new Color(0x616161));
        g.drawRoundRect(cx, 0, cw, 50, 16, 16);
        g.setColor(new Color(0xCFCFCF));
        g.fillRoundRect(cx + 24, 13, cw - 48, 13, 10, 10);
        g.setColor(new Color(0x757575));
        g.drawRoundRect(cx + 24, 13, cw - 48, 13, 10, 10);
        g.dispose();
    }
}
