package com.pdv.view;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Controla o "jogo de botões" das telas de cadastro e evita perda acidental de dados.
 *
 * <pre>
 * Modo         Campos      Tabela/busca   Novo   Editar              Salvar   Cancelar
 * CONSULTA     bloqueados  livres         sim    se há seleção       não      não
 * NOVO         liberados   bloqueadas     não    não                 sim      sim
 * EDICAO       liberados   bloqueadas     não    não                 sim      sim
 * </pre>
 * Enquanto edita, a tabela fica bloqueada (não dá para trocar de registro) e sair da tela com dados
 * alterados pede confirmação.
 */
public class ModoCadastro {
    public enum Modo { CONSULTA, NOVO, EDICAO }

    private final JButton novo, editar, salvar, cancelar;
    private final JComponent[] campos, somenteNovo, navegacao;
    private final JLabel rotulo = new JLabel(" ");
    private Runnable aoMudar = () -> { };
    private String dicaEdicao = "Editando o registro. Clique em Salvar para gravar ou em Cancelar para desfazer.";
    private Modo modo = Modo.CONSULTA;
    private boolean temSelecao, sujo, carregando;

    /**
     * @param campos      liberados em NOVO e EDICAO
     * @param somenteNovo liberados apenas em NOVO (ex.: estoque inicial)
     * @param navegacao   bloqueados enquanto edita (tabela, busca)
     */
    public ModoCadastro(JButton novo, JButton editar, JButton salvar, JButton cancelar,
                        JComponent[] campos, JComponent[] somenteNovo, JComponent[] navegacao) {
        this.novo = novo;
        this.editar = editar;
        this.salvar = salvar;
        this.cancelar = cancelar;
        this.campos = campos;
        this.somenteNovo = somenteNovo;
        this.navegacao = navegacao;
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD));
        rotulo.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
        aplicar();
    }

    public JLabel getRotulo() { return rotulo; }
    public Modo getModo() { return modo; }
    public boolean editando() { return modo != Modo.CONSULTA; }
    public void aoMudar(Runnable r) { aoMudar = r; aplicar(); }
    public void dicaEdicao(String texto) { dicaEdicao = texto; aplicar(); }

    /** Marca como "alterado" quando o usuário mexe nos campos (mudanças programáticas são ignoradas). */
    public void observar(JComponent... comps) {
        for (JComponent c : comps) {
            if (c instanceof JTextComponent t) Ui.aoDigitar(t, this::alterou);
            else if (c instanceof JComboBox<?> cb) cb.addActionListener(e -> alterou());
            else if (c instanceof AbstractButton b) b.addActionListener(e -> alterou());
        }
    }

    /** Executa um preenchimento programático sem contar como alteração do usuário. */
    public void carregando(Runnable r) {
        boolean anterior = carregando;
        carregando = true;
        try { r.run(); } finally { carregando = anterior; }
    }

    public void consulta(boolean temSelecao) { modo = Modo.CONSULTA; this.temSelecao = temSelecao; sujo = false; aplicar(); }
    public void novo() { modo = Modo.NOVO; sujo = false; aplicar(); }
    public void edicao() { modo = Modo.EDICAO; sujo = false; aplicar(); }

    /** true se pode abandonar a edição (sem alterações ou o usuário confirmou o descarte). */
    public boolean podeSair(Component pai) {
        return !editando() || !sujo || Ui.confirmar(pai, "Há dados digitados que ainda não foram salvos.\nDeseja descartá-los?");
    }

    /**
     * Instala o comportamento da janela: Ctrl+N / Ctrl+E / Ctrl+S acionam Novo / Editar / Salvar (além do Alt+letra
     * dos botões); Esc cancela a edição ou, em consulta, fecha a tela; o X da janela pede confirmação se há dados não salvos.
     * (Ctrl+C é reservado para copiar, por isso Cancelar usa Alt+C e Esc.)
     */
    public void instalar(JDialog d) {
        d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { if (podeSair(d)) d.dispose(); }
        });
        Ui.atalho(d.getRootPane(), "ctrl N", novo::doClick);
        Ui.atalho(d.getRootPane(), "ctrl E", editar::doClick);
        Ui.atalho(d.getRootPane(), "ctrl S", salvar::doClick);
        Ui.atalho(d.getRootPane(), "ESCAPE", () -> { if (editando()) cancelar.doClick(); else d.dispose(); });
        novo.setToolTipText("Novo cadastro  (Alt+N ou Ctrl+N)");
        editar.setToolTipText("Editar o registro selecionado  (Alt+E ou Ctrl+E)");
        salvar.setToolTipText("Salvar  (Alt+S ou Ctrl+S)");
        cancelar.setToolTipText("Cancelar a edição e desfazer as alterações  (Alt+C ou Esc)");
    }

    private void alterou() { if (!carregando && editando()) sujo = true; }

    private void aplicar() {
        boolean ed = editando();
        for (JComponent c : campos) c.setEnabled(ed);
        for (JComponent c : somenteNovo) c.setEnabled(modo == Modo.NOVO);
        for (JComponent c : navegacao) c.setEnabled(!ed);
        novo.setEnabled(!ed);
        editar.setEnabled(modo == Modo.CONSULTA && temSelecao);
        salvar.setEnabled(ed);
        cancelar.setEnabled(ed);
        switch (modo) {
            case CONSULTA -> {
                rotulo.setText(temSelecao ? "Consulta: clique em Editar para alterar ou em Novo para cadastrar."
                        : "Selecione um registro na tabela ou clique em Novo.");
                rotulo.setForeground(Color.GRAY);
            }
            case NOVO -> {
                rotulo.setText("Novo cadastro: preencha os campos e clique em Salvar.");
                rotulo.setForeground(new Color(0x2E7D32));
            }
            case EDICAO -> {
                rotulo.setText(dicaEdicao);
                rotulo.setForeground(new Color(0xE65100));
            }
        }
        aoMudar.run();
    }
}
