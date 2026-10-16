package com.pdv.view;

import com.pdv.util.Fmt;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.Locale;

/** Campo de data dd/MM/aaaa com máscara e calendário (botão ao lado). Dá para digitar ou escolher no calendário. */
public class DatePicker extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Color AZUL = new Color(0x1565C0);
    private static final String[] SEMANA = {"D", "S", "T", "Q", "Q", "S", "S"};

    private final JFormattedTextField campo;
    private final JButton botao = new JButton(Icones.get("contas", 16));
    private final JPopupMenu popup = new JPopupMenu();
    private YearMonth mes = YearMonth.now();

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
        botao.setToolTipText("Escolher a data no calendário");
        botao.setFocusable(false);
        botao.setMargin(new Insets(1, 4, 1, 4));
        botao.addActionListener(e -> alternarCalendario());
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

    // ---------------------------------------------------------------- calendário
    private void alternarCalendario() {
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        LocalDate d = getDate();
        mes = YearMonth.from(d != null ? d : LocalDate.now());
        montar();
        popup.show(botao, botao.getWidth() - popup.getPreferredSize().width, botao.getHeight());
    }

    private void montar() {
        popup.removeAll();
        popup.add(criarCalendario());
        popup.pack();
        popup.repaint();
    }

    /** Painel do calendário do mês corrente (visível para testes no mesmo pacote). */
    JComponent criarCalendario() {
        LocalDate selecionada = getDate(), hoje = LocalDate.now();

        String nomeMes = mes.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
        JLabel titulo = new JLabel(Character.toUpperCase(nomeMes.charAt(0)) + nomeMes.substring(1) + " " + mes.getYear(), SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));

        JPanel esquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        esquerda.setOpaque(false);
        esquerda.add(navegar("«", "Ano anterior", -12));
        esquerda.add(navegar("‹", "Mês anterior", -1));
        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        direita.setOpaque(false);
        direita.add(navegar("›", "Próximo mês", 1));
        direita.add(navegar("»", "Próximo ano", 12));
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        cabecalho.add(esquerda, BorderLayout.WEST);
        cabecalho.add(titulo, BorderLayout.CENTER);
        cabecalho.add(direita, BorderLayout.EAST);

        JPanel grade = new JPanel(new GridLayout(0, 7, 1, 1));
        grade.setOpaque(false);
        for (String s : SEMANA) {
            JLabel l = new JLabel(s, SwingConstants.CENTER);
            l.setForeground(Color.GRAY);
            grade.add(l);
        }
        int deslocamento = mes.atDay(1).getDayOfWeek().getValue() % 7; // domingo = 0
        for (int i = 0; i < deslocamento; i++) grade.add(new JLabel(" "));
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate data = mes.atDay(dia);
            DiaButton b = new DiaButton(String.valueOf(dia), data.equals(selecionada), data.equals(hoje));
            b.addActionListener(e -> {
                setDate(data);
                popup.setVisible(false);
            });
            grade.add(b);
        }

        JButton btnHoje = new DiaButton("Hoje", false, false);
        btnHoje.setPreferredSize(new Dimension(70, 28));
        btnHoje.setForeground(AZUL);
        btnHoje.addActionListener(e -> {
            setDate(LocalDate.now());
            popup.setVisible(false);
        });
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        rodape.setOpaque(false);
        rodape.add(btnHoje);

        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
        painel.add(cabecalho, BorderLayout.NORTH);
        painel.add(grade, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private JButton navegar(String texto, String dica, int meses) {
        DiaButton b = new DiaButton(texto, false, false);
        b.setPreferredSize(new Dimension(30, 28));
        b.setToolTipText(dica);
        b.addActionListener(e -> {
            mes = mes.plusMonths(meses);
            montar();
        });
        return b;
    }

    /** Botão plano que se desenha sozinho (o Nimbus ignora cores de fundo de botões). */
    private static final class DiaButton extends JButton {
        private final boolean selecionado, hoje;

        DiaButton(String texto, boolean selecionado, boolean hoje) {
            super(texto);
            this.selecionado = selecionado;
            this.hoje = hoje;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setMargin(new Insets(2, 2, 2, 2));
            setPreferredSize(new Dimension(34, 28));
            setForeground(selecionado ? Color.WHITE : Color.BLACK);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selecionado) {
                g.setColor(AZUL);
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            } else if (getModel().isRollover()) {
                g.setColor(new Color(0xE3F2FD));
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            }
            if (hoje && !selecionado) {
                g.setColor(AZUL);
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
