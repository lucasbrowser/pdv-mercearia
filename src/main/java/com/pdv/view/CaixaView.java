package com.pdv.view;

import com.pdv.controller.CaixaController;
import com.pdv.model.Caixa;
import com.pdv.model.Permissao;
import com.pdv.model.ResumoCaixa;
import com.pdv.model.TipoMovCaixa;
import com.pdv.util.Fmt;
import com.pdv.util.ImpressoraCupom;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/** Gestão do caixa: abertura (fundo de troco), sangria, suprimento e fechamento com conferência. */
public class CaixaView extends JDialog {
    private final CaixaController ctrl = new CaixaController();
    private final JLabel status = new JLabel(" ");
    private final JTextArea resumo = new JTextArea();

    public CaixaView(Window pai) {
        super(pai, "Caixa", ModalityType.APPLICATION_MODAL);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        status.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        resumo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resumo.setEditable(false);

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Abrir caixa", () -> { abrirCaixa(this); atualizar(); }));
        botoes.add(Ui.botao("Sangria", () -> { movimento(this, TipoMovCaixa.SANGRIA); atualizar(); }));
        botoes.add(Ui.botao("Suprimento", () -> { movimento(this, TipoMovCaixa.SUPRIMENTO); atualizar(); }));
        botoes.add(Ui.botao("Fechar caixa", () -> { fecharCaixa(this); atualizar(); }));
        botoes.add(Ui.botao("Imprimir resumo", () -> ImpressoraCupom.imprimirTexto(ImpressoraCupom.montarTextoCaixa(ctrl.resumoAberto()))));

        botoes.add(Ui.cancelar(this));

        add(status, BorderLayout.NORTH);
        add(new JScrollPane(resumo), BorderLayout.CENTER);
        add(botoes, BorderLayout.SOUTH);
        setSize(700, 640);
        setLocationRelativeTo(pai);
        atualizar();
    }

    private void atualizar() {
        Optional<Caixa> c = ctrl.caixaAberto();
        if (c.isPresent()) {
            status.setText("CAIXA #" + c.get().id() + " ABERTO desde " + c.get().dataAbertura().format(Fmt.DATA_HORA)
                    + " por " + c.get().abertoPor());
            status.setForeground(new Color(0x2E7D32));
            resumo.setText(ImpressoraCupom.montarTextoCaixa(ctrl.resumoAberto()));
        } else {
            status.setText("CAIXA FECHADO");
            status.setForeground(new Color(0xC62828));
            resumo.setText("Nenhum caixa aberto.\nClique em \"Abrir caixa\" para iniciar o turno.");
        }
        resumo.setCaretPosition(0);
    }

    // ---------------------------------------------------------------- ações reutilizáveis (também usadas pelo PDV)

    /** Garante caixa aberto antes de vender; oferece abrir se o usuário tiver permissão. */
    public static boolean garantirCaixaAberto(Window pai) {
        CaixaController c = new CaixaController();
        if (c.caixaAberto().isPresent()) return true;
        if (!Sessao.pode(Permissao.GERENCIAR_CAIXA)) {
            Ui.info(pai, "Não há caixa aberto. Solicite a abertura a um responsável.");
            return false;
        }
        return Ui.confirmar(pai, "Não há caixa aberto. Deseja abrir agora?") && abrirCaixa(pai);
    }

    public static boolean abrirCaixa(Window pai) {
        JTextField fundo = new JTextField("0,00", 10);
        Object[] msg = {"Fundo de troco (dinheiro inicial na gaveta, R$):", fundo};
        if (JOptionPane.showConfirmDialog(pai, msg, "Abertura de caixa", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return false;
        Caixa c = new CaixaController().abrir(fundo.getText());
        Ui.info(pai, "Caixa #" + c.id() + " aberto com fundo de troco de " + Fmt.moeda(c.valorAbertura()) + ".");
        return true;
    }

    public static void movimento(Window pai, TipoMovCaixa tipo) {
        JTextField valor = new JTextField(10);
        JTextField motivo = new JTextField(24);
        Object[] msg = {"Valor (R$):", valor, "Motivo (obrigatório):", motivo};
        if (JOptionPane.showConfirmDialog(pai, msg, tipo.getRotulo(), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        CaixaController ctrl = new CaixaController();
        Caixa caixa = ctrl.movimentar(tipo, valor.getText(), motivo.getText());
        tentarImprimir(pai, ImpressoraCupom.montarTextoMovimento(caixa, tipo, valor.getText().trim(), motivo.getText().trim(),
                Sessao.atual().nome()));
        Ui.info(pai, tipo == TipoMovCaixa.SANGRIA ? "Sangria registrada." : "Suprimento registrado.");
    }

    public static void fecharCaixa(Window pai) {
        CaixaController ctrl = new CaixaController();
        ResumoCaixa r = ctrl.resumoAberto();
        JTextArea texto = new JTextArea(ImpressoraCupom.montarTextoCaixa(r), 18, 50);
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        texto.setEditable(false);
        JTextField contado = new JTextField(10);
        JTextField obs = new JTextField(30);
        Object[] msg = {new JScrollPane(texto), "Dinheiro CONTADO na gaveta (R$):", contado,
                "Observação (obrigatória se houver diferença):", obs};
        if (JOptionPane.showConfirmDialog(pai, msg, "Fechamento de caixa", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ResumoCaixa fechado = ctrl.fechar(contado.getText(), obs.getText());
        String dif = fechado.caixa().diferenca().signum() == 0 ? "Caixa conferido, sem diferença."
                : (fechado.caixa().diferenca().signum() > 0 ? "SOBRA de " : "FALTA de ") + Fmt.moeda(fechado.caixa().diferenca().abs());
        Ui.info(pai, "Caixa #" + fechado.caixa().id() + " fechado.\n" + dif);
        tentarImprimir(pai, ImpressoraCupom.montarTextoCaixa(fechado));
    }

    private static void tentarImprimir(Window pai, String texto) {
        try {
            ImpressoraCupom.imprimirTexto(texto);
        } catch (NegocioException ex) {
            Ui.erro(pai, new NegocioException("Operação registrada, mas a impressão falhou: " + ex.getMessage()));
        }
    }
}
