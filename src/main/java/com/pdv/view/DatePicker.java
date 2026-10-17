package com.pdv.view;

import com.pdv.util.Fmt;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/** Campo de data dd/MM/aaaa com máscara e calendário (botão ao lado). Dá para digitar ou escolher no calendário. */
public class DatePicker extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final JFormattedTextField campo;
    private final JButton botao = new JButton(Icones.get("contas", 16));
    private final JPopupMenu popup = new JPopupMenu();
    private final CalendarioPanel calendario = new CalendarioPanel(d -> {
        setDate(d);
        popup.setVisible(false);
    });
    private long fechouEm;

    public DatePicker() { this(null); }

    public DatePicker(LocalDate inicial) {
        super(new BorderLayout(3, 0));
        try {
            MaskFormatter mascara = new MaskFormatter("##/##/####");
            mascara.setPlaceholderCharacter('_');
            campo = new JFormattedTextField(mascara);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
        campo.setColumns(8);
        campo.setFocusLostBehavior(JFormattedTextField.PERSIST);

        int h = campo.getPreferredSize().height; // botão quadrado, da mesma altura do campo
        botao.setPreferredSize(new Dimension(h + 4, h));
        botao.setMargin(new Insets(0, 0, 0, 0));
        botao.setFocusable(false);
        botao.setToolTipText("Escolher a data no calendário");
        botao.addActionListener(e -> alternarCalendario());

        popup.setBorder(BorderFactory.createLineBorder(new Color(0x90A4AE)));
        popup.add(calendario);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) { }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { fechouEm = System.currentTimeMillis(); }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { }
        });

        add(campo, BorderLayout.CENTER);
        add(botao, BorderLayout.EAST);
        setDate(inicial);
    }

    /** Data válida ou null se vazio/incompleto/inexistente (ex.: 31/02). */
    public LocalDate getDate() {
        String t = campo.getText();
        if (t == null || t.contains("_")) return null;
        try {
            return LocalDate.parse(t.trim(), FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public void setDate(LocalDate d) { campo.setValue(d == null ? null : d.format(FMT)); }

    /** Texto dd/MM/aaaa (compatível com Fmt.data) ou "" se não houver data válida. */
    public String getText() {
        LocalDate d = getDate();
        return d == null ? "" : d.format(Fmt.DATA);
    }

    public void setText(String texto) {
        try {
            setDate(LocalDate.parse(texto.trim(), FMT));
        } catch (RuntimeException e) {
            setDate(null);
        }
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (campo != null) campo.setEnabled(enabled);
        botao.setEnabled(enabled);
    }

    @Override public void requestFocus() { campo.requestFocus(); }

    @Override public boolean requestFocusInWindow() { return campo.requestFocusInWindow(); }

    /** Visível para testes no mesmo pacote. */
    CalendarioPanel calendario() { return calendario; }

    private void alternarCalendario() {
        if (System.currentTimeMillis() - fechouEm < 250) return; // o clique que fechou o popup não deve reabri-lo
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        calendario.mostrar(getDate());
        popup.show(botao, botao.getWidth() - CalendarioPanel.W - 2, botao.getHeight());
    }

    /**
     * Calendário desenhado diretamente (Graphics2D), com tamanho fixo de 6 semanas: não depende de botões do
     * look-and-feel, então o visual é sempre igual e a altura não muda ao trocar de mês.
     */
    static final class CalendarioPanel extends JPanel {
        static final int PAD = 10, CW = 36, CH = 32, CAB = 34, SEM = 24, RODAPE = 30;
        static final int W = PAD * 2 + 7 * CW, H = PAD * 2 + CAB + SEM + 6 * CH + RODAPE;
        private static final int NAV = 100, HOJE = 200;
        private static final Color AZUL = new Color(0x1565C0), HOVER = new Color(0xE3F2FD);
        private static final String[] SEMANA = {"D", "S", "T", "Q", "Q", "S", "S"};

        private final Consumer<LocalDate> aoEscolher;
        private YearMonth mes = YearMonth.now();
        private LocalDate selecionada;
        private int hover = -1;

        CalendarioPanel(Consumer<LocalDate> aoEscolher) {
            this.aoEscolher = aoEscolher;
            setPreferredSize(new Dimension(W, H));
            setOpaque(true);
            setBackground(Color.WHITE);
            MouseAdapter m = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { definirHover(hit(e.getX(), e.getY())); }
                @Override public void mouseExited(MouseEvent e) { definirHover(-1); }
                @Override public void mouseReleased(MouseEvent e) { clique(e.getX(), e.getY()); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void mostrar(LocalDate sel) {
            selecionada = sel;
            mes = YearMonth.from(sel != null ? sel : LocalDate.now());
            hover = -1;
            repaint();
        }

        private LocalDate primeiroDaGrade() {
            LocalDate p = mes.atDay(1);
            return p.minusDays(p.getDayOfWeek().getValue() % 7); // semana começa no domingo
        }

        private void definirHover(int h) {
            if (h != hover) {
                hover = h;
                repaint();
            }
        }

        /** 0..41 = dia da grade; 100..103 = navegação; 200 = "Hoje"; -1 = nada. */
        int hit(int x, int y) {
            int yNav = PAD + 3;
            if (y >= yNav && y < yNav + 28) {
                int[] xs = {PAD, PAD + 30, W - PAD - 60, W - PAD - 30};
                for (int i = 0; i < 4; i++) if (x >= xs[i] && x < xs[i] + 28) return NAV + i;
                return -1;
            }
            int gy = PAD + CAB + SEM;
            if (y >= gy && y < gy + 6 * CH && x >= PAD && x < PAD + 7 * CW) return ((y - gy) / CH) * 7 + (x - PAD) / CW;
            int fy = gy + 6 * CH;
            if (y >= fy && y < fy + RODAPE && Math.abs(x - W / 2) <= 40) return HOJE;
            return -1;
        }

        void clique(int x, int y) {
            int h = hit(x, y);
            if (h < 0) return;
            if (h == HOJE) {
                aoEscolher.accept(LocalDate.now());
            } else if (h >= NAV) {
                mes = mes.plusMonths(new int[]{-12, -1, 1, 12}[h - NAV]);
                repaint();
            } else {
                aoEscolher.accept(primeiroDaGrade().plusDays(h));
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            Font base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

            // navegação e título
            int yNav = PAD + 3;
            int[] xs = {PAD, PAD + 30, W - PAD - 60, W - PAD - 30};
            String[] simbolos = {"«", "‹", "›", "»"};
            for (int i = 0; i < 4; i++) {
                if (hover == NAV + i) {
                    g.setColor(HOVER);
                    g.fillRoundRect(xs[i], yNav, 28, 28, 8, 8);
                }
                g.setFont(base.deriveFont(Font.BOLD, 17f));
                g.setColor(AZUL);
                centrar(g, simbolos[i], xs[i], yNav, 28, 28);
            }
            String nome = mes.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
            g.setFont(base.deriveFont(Font.BOLD, 14f));
            g.setColor(new Color(0x263238));
            centrar(g, Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " " + mes.getYear(), PAD + 60, yNav, W - 2 * PAD - 120, 28);

            // dias da semana
            int ySem = PAD + CAB;
            g.setFont(base.deriveFont(Font.BOLD, 12f));
            g.setColor(Color.GRAY);
            for (int c = 0; c < 7; c++) centrar(g, SEMANA[c], PAD + c * CW, ySem, CW, SEM);

            // dias (6 semanas)
            int gy = PAD + CAB + SEM;
            LocalDate d0 = primeiroDaGrade(), hoje = LocalDate.now();
            for (int i = 0; i < 42; i++) {
                LocalDate d = d0.plusDays(i);
                int x = PAD + (i % 7) * CW, y = gy + (i / 7) * CH;
                boolean doMes = YearMonth.from(d).equals(mes), sel = d.equals(selecionada);
                if (sel) {
                    g.setColor(AZUL);
                    g.fillRoundRect(x + 2, y + 2, CW - 4, CH - 4, 10, 10);
                } else if (hover == i) {
                    g.setColor(HOVER);
                    g.fillRoundRect(x + 2, y + 2, CW - 4, CH - 4, 10, 10);
                }
                if (d.equals(hoje) && !sel) {
                    g.setColor(AZUL);
                    g.drawRoundRect(x + 2, y + 2, CW - 5, CH - 5, 10, 10);
                }
                g.setFont(base);
                g.setColor(sel ? Color.WHITE : doMes ? new Color(0x212121) : new Color(0xBDBDBD));
                centrar(g, String.valueOf(d.getDayOfMonth()), x, y, CW, CH);
            }

            // rodapé
            int fy = gy + 6 * CH;
            g.setFont(base.deriveFont(Font.BOLD));
            g.setColor(AZUL);
            centrar(g, "Hoje", W / 2 - 40, fy, 80, RODAPE);
            if (hover == HOJE) {
                int tw = g.getFontMetrics().stringWidth("Hoje");
                g.drawLine(W / 2 - tw / 2, fy + RODAPE / 2 + 8, W / 2 + tw / 2, fy + RODAPE / 2 + 8);
            }
            g.dispose();
        }

        private static void centrar(Graphics2D g, String texto, int x, int y, int w, int h) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }
}
