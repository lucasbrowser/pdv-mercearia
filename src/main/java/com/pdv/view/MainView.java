package com.pdv.view;

import com.pdv.controller.*;
import com.pdv.model.*;
import com.pdv.util.Config;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tela principal: abre maximizada, com barra de menus, barra de atalhos (ícones), painel de resumo e barra de status.
 * Todas as telas são abertas a partir daqui; o botão Cancelar de cada uma devolve o controle a esta tela.
 */
public class MainView extends JFrame {
    private enum Tipo {
        INFO(0x37474F, 0xECEFF1), OK(0x2E7D32, 0xE8F5E9), AVISO(0xE65100, 0xFFF3E0), ERRO(0xC62828, 0xFFEBEE);

        final Color fg, bg;

        Tipo(int fg, int bg) { this.fg = new Color(fg); this.bg = new Color(bg); }
    }

    /** Cartão do painel de resumo. */
    private static final class Card extends JPanel {
        private final JLabel valor = new JLabel(" ");
        private final JLabel sub = new JLabel(" ");

        Card(String titulo, Color cor) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xDDDDDD)),
                    BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 6, 0, 0, cor),
                            BorderFactory.createEmptyBorder(18, 20, 18, 20))));
            JLabel t = new JLabel(titulo);
            t.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            t.setForeground(Color.GRAY);
            valor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            valor.setForeground(cor);
            sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            sub.setForeground(Color.DARK_GRAY);
            add(t);
            add(Box.createVerticalStrut(10));
            add(valor);
            add(Box.createVerticalStrut(8));
            add(sub);
        }

        void set(String v, String s) { valor.setText(v); sub.setText(s); }
    }

    private final CaixaController caixaCtrl = new CaixaController();
    private final ContaPagarController contas = new ContaPagarController();
    private final HistoricoVendasController vendasCtrl = new HistoricoVendasController();
    private final EstoqueController estoqueCtrl = new EstoqueController();

    private final Card cCaixa = new Card("CAIXA", new Color(0x1565C0));
    private final Card cVendas = new Card("VENDAS DE HOJE", new Color(0x2E7D32));
    private final Card cContas = new Card("CONTAS A PAGAR", new Color(0xE65100));
    private final Card cEstoque = new Card("ESTOQUE", new Color(0x6A1B9A));

    private final JLabel lblMsg = new JLabel(" ");
    private final JLabel lblCaixa = new JLabel(" ");
    private final JLabel lblContas = new JLabel(" ");
    private final JLabel lblRelogio = new JLabel(" ");
    private final javax.swing.Timer statusTimer = new javax.swing.Timer(12000, e -> statusPronto());
    private final javax.swing.Timer relogioTimer = new javax.swing.Timer(1000,
            e -> lblRelogio.setText(LocalDateTime.now().format(Fmt.DATA_HORA)));
    private final javax.swing.Timer painelTimer = new javax.swing.Timer(15 * 60 * 1000, e -> atualizarPainel());
    private JButton btnLembretes;

    private Optional<Caixa> caixaAtual = Optional.empty();
    private List<ContaPagar> lembretes = List.of();

    public MainView() {
        super("PDV Mercado de Bairro - Menu principal");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1200, 760);                 // tamanho ao "restaurar" a janela
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);   // abre maximizada
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { sair(); }
            @Override public void windowOpened(WindowEvent e) { setExtendedState(MAXIMIZED_BOTH); }
        });
        setJMenuBar(montarMenu());
        montarTela();
        statusTimer.setRepeats(false);
        relogioTimer.setInitialDelay(0);
        relogioTimer.start();
        painelTimer.start();
        atualizarPainel();
        statusInicial();
    }

    // ================================================================== menu
    private JMenuBar montarMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu arquivo = menu("Arquivo", 'A');
        item(arquivo, "Trocar usuário", "usuarios", "ctrl L", null, this::trocarUsuario);
        arquivo.addSeparator();
        item(arquivo, "Sair do sistema", null, "ctrl Q", null, this::sair);

        JMenu vendas = menu("Vendas", 'V');
        item(vendas, "PDV - Frente de caixa", "carrinho", "F2", Permissao.VENDER, this::abrirPdv);
        item(vendas, "Histórico de vendas...", "vendas", null, Permissao.CONSULTAR_VENDAS, () -> tela(() -> new VendasView(this).setVisible(true)));

        JMenu caixa = menu("Caixa", 'C');
        item(caixa, "Abrir caixa", "caixa", null, Permissao.GERENCIAR_CAIXA, () -> tela(() -> CaixaView.abrirCaixa(this)));
        item(caixa, "Sangria (retirada)", "sangria", null, Permissao.GERENCIAR_CAIXA, () -> tela(() -> CaixaView.movimento(this, TipoMovCaixa.SANGRIA)));
        item(caixa, "Suprimento (reforço de troco)", null, null, Permissao.GERENCIAR_CAIXA, () -> tela(() -> CaixaView.movimento(this, TipoMovCaixa.SUPRIMENTO)));
        item(caixa, "Fechar caixa", null, null, Permissao.GERENCIAR_CAIXA, () -> tela(() -> CaixaView.fecharCaixa(this)));
        caixa.addSeparator();
        item(caixa, "Resumo / gerenciar caixa...", "caixa", null, Permissao.GERENCIAR_CAIXA, () -> tela(() -> new CaixaView(this).setVisible(true)));

        JMenu cadastros = menu("Cadastros", 'D');
        item(cadastros, "Produtos...", "produtos", null, Permissao.GERENCIAR_PRODUTOS, () -> tela(() -> new ProdutoView(this).setVisible(true)));
        item(cadastros, "Fornecedores...", "fornecedores", null, Permissao.GERENCIAR_CONTAS, () -> tela(() -> new FornecedorView(this).setVisible(true)));
        item(cadastros, "Usuários...", "usuarios", null, Permissao.GERENCIAR_USUARIOS, () -> tela(() -> new UsuarioView(this).setVisible(true)));

        JMenu estoque = menu("Estoque", 'E');
        item(estoque, "Controle de estoque...", "estoque", null, Permissao.GERENCIAR_ESTOQUE, () -> tela(() -> new EstoqueView(this).setVisible(true)));

        JMenu financeiro = menu("Financeiro", 'F');
        item(financeiro, "Contas a pagar...", "contas", null, Permissao.GERENCIAR_CONTAS, () -> tela(() -> new ContaPagarView(this).setVisible(true)));
        item(financeiro, "Lembretes (prancheta)", "lembretes", null, Permissao.GERENCIAR_CONTAS, this::aLembretes);

        JMenu consultas = menu("Relatórios", 'R');
        item(consultas, "Relatórios gerenciais...", "relatorios", null, Permissao.VER_RELATORIOS, () -> tela(() -> new RelatorioView(this).setVisible(true)));

        JMenu ajuda = menu("Ajuda", 'J');
        item(ajuda, "Sobre", null, null, null, () -> Ui.info(this, "PDV Mercado de Bairro\nJava 21 + Swing + SQLite"));

        for (JMenu m : new JMenu[]{arquivo, vendas, caixa, cadastros, estoque, financeiro, consultas, ajuda}) mb.add(m);
        return mb;
    }

    private static JMenu menu(String texto, char mnemonico) {
        JMenu m = new JMenu(texto);
        m.setMnemonic(mnemonico);
        return m;
    }

    private void item(JMenu m, String texto, String icone, String acelerador, Permissao exigida, Runnable acao) {
        JMenuItem it = new JMenuItem(texto);
        if (icone != null) it.setIcon(Icones.get(icone, 16));
        if (acelerador != null) it.setAccelerator(KeyStroke.getKeyStroke(acelerador));
        boolean ok = exigida == null || Sessao.pode(exigida);
        it.setEnabled(ok);
        if (!ok) it.setToolTipText("Sem permissão: " + exigida.getDescricao());
        it.addActionListener(e -> acao.run());
        m.add(it);
    }

    // ================================================================== barra de atalhos
    private JToolBar montarToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setRollover(true);
        atalho(tb, "PDV", "carrinho", "PDV - Frente de caixa (F2)", Permissao.VENDER, this::abrirPdv);
        atalho(tb, "Caixa", "caixa", "Abertura, sangria e fechamento de caixa", Permissao.GERENCIAR_CAIXA,
                () -> tela(() -> new CaixaView(this).setVisible(true)));
        atalho(tb, "Sangria", "sangria", "Sangria do caixa", Permissao.GERENCIAR_CAIXA,
                () -> tela(() -> CaixaView.movimento(this, TipoMovCaixa.SANGRIA)));
        tb.addSeparator();
        atalho(tb, "Produtos", "produtos", "Cadastro de produtos", Permissao.GERENCIAR_PRODUTOS,
                () -> tela(() -> new ProdutoView(this).setVisible(true)));
        atalho(tb, "Estoque", "estoque", "Controle de estoque", Permissao.GERENCIAR_ESTOQUE,
                () -> tela(() -> new EstoqueView(this).setVisible(true)));
        atalho(tb, "Vendas", "vendas", "Histórico de vendas", Permissao.CONSULTAR_VENDAS,
                () -> tela(() -> new VendasView(this).setVisible(true)));
        tb.addSeparator();
        atalho(tb, "Fornecedores", "fornecedores", "Cadastro de fornecedores", Permissao.GERENCIAR_CONTAS,
                () -> tela(() -> new FornecedorView(this).setVisible(true)));
        atalho(tb, "Contas a pagar", "contas", "Contas a pagar", Permissao.GERENCIAR_CONTAS,
                () -> tela(() -> new ContaPagarView(this).setVisible(true)));
        atalho(tb, "Relatórios", "relatorios", "Relatórios gerenciais", Permissao.VER_RELATORIOS,
                () -> tela(() -> new RelatorioView(this).setVisible(true)));
        tb.addSeparator();
        atalho(tb, "Usuários", "usuarios", "Cadastro de usuários e permissões", Permissao.GERENCIAR_USUARIOS,
                () -> tela(() -> new UsuarioView(this).setVisible(true)));
        btnLembretes = atalho(tb, "Lembretes", "lembretes", "Lembretes de contas a pagar", Permissao.GERENCIAR_CONTAS, this::aLembretes);
        return tb;
    }

    private JButton atalho(JToolBar tb, String texto, String icone, String dica, Permissao exigida, Runnable acao) {
        JButton b = new JButton(texto, Icones.get(icone, 32));
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        b.setFocusable(false);
        b.setToolTipText(dica);
        boolean ok = exigida == null || Sessao.pode(exigida);
        b.setEnabled(ok);
        if (!ok) b.setToolTipText("Sem permissão: " + exigida.getDescricao());
        b.addActionListener(e -> acao.run());
        tb.add(b);
        return b;
    }

    // ================================================================== layout
    private void montarTela() {
        // painel central (resumo) - substitui os antigos botões
        JLabel boasVindas = new JLabel("Olá, " + Sessao.atual().nome() + (Sessao.atual().admin() ? "  (Administrador)" : ""));
        boasVindas.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        JLabel mercado = new JLabel(Config.get("mercado.nome"));
        mercado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        mercado.setForeground(Color.GRAY);
        JPanel titulo = new JPanel(new GridLayout(0, 1));
        titulo.setOpaque(false);
        titulo.add(boasVindas);
        titulo.add(mercado);

        JPanel grade = new JPanel(new GridLayout(2, 2, 20, 20));
        grade.setOpaque(false);
        grade.setPreferredSize(new Dimension(900, 360));
        grade.add(cCaixa);
        grade.add(cVendas);
        grade.add(cContas);
        grade.add(cEstoque);
        JPanel centroGrade = new JPanel(new GridBagLayout());
        centroGrade.setOpaque(false);
        centroGrade.add(grade);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(new Color(0xF5F5F5));
        centro.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        centro.add(titulo, BorderLayout.NORTH);
        centro.add(centroGrade, BorderLayout.CENTER);

        // barra de status
        lblMsg.setOpaque(true);
        lblMsg.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        lblMsg.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        lblCaixa.setFont(lblCaixa.getFont().deriveFont(Font.BOLD));
        lblContas.setFont(lblContas.getFont().deriveFont(Font.BOLD));
        JPanel infos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        infos.add(lblCaixa);
        infos.add(new JLabel("Operador: " + Sessao.atual().nome()));
        infos.add(lblContas);
        infos.add(lblRelogio);
        JPanel barraStatus = new JPanel(new BorderLayout());
        barraStatus.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        barraStatus.add(lblMsg, BorderLayout.CENTER);
        barraStatus.add(infos, BorderLayout.EAST);

        add(montarToolbar(), BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(barraStatus, BorderLayout.SOUTH);
    }

    // ================================================================== status
    private void status(Tipo tipo, String msg) {
        lblMsg.setText(" " + msg.replace('\n', ' '));
        lblMsg.setForeground(tipo.fg);
        lblMsg.setBackground(tipo.bg);
        lblMsg.setFont(lblMsg.getFont().deriveFont(tipo == Tipo.ERRO ? Font.BOLD : Font.PLAIN));
        if (tipo == Tipo.OK || tipo == Tipo.INFO) statusTimer.restart(); else statusTimer.stop(); // erros/avisos permanecem
    }

    private void statusPronto() {
        status(Tipo.INFO, "Pronto.");
        statusTimer.stop();
    }

    private void statusInicial() {
        if (caixaAtual.isEmpty() && Sessao.pode(Permissao.VENDER))
            status(Tipo.AVISO, "Caixa fechado. Abra o caixa (menu Caixa) ou entre no PDV para iniciar as vendas.");
        else status(Tipo.OK, "Bem-vindo, " + Sessao.atual().nome() + "!");
    }

    // ================================================================== execução
    /** Executa uma ação; erros de regra aparecem na barra de status. */
    private void executar(Runnable acao) {
        statusPronto();
        try {
            acao.run();
        } catch (NegocioException ex) {
            Toolkit.getDefaultToolkit().beep();
            status(Tipo.ERRO, ex.getMessage());
        } catch (Exception ex) {
            status(Tipo.ERRO, "Erro inesperado: " + ex.getMessage());
            Ui.erro(this, ex);
        }
    }

    /** Abre outra tela (modal). Ao fechá-la ou cancelá-la, o controle volta a esta tela. */
    private void tela(Runnable abre) {
        executar(() -> {
            abre.run();
            posTela();
        });
    }

    private void posTela() {
        atualizarPainel();
        if (caixaAtual.isEmpty()) status(Tipo.AVISO, "De volta ao menu principal. Caixa fechado.");
        else status(Tipo.INFO, "De volta ao menu principal.");
    }

    private void abrirPdv() {
        executar(() -> {
            if (!CaixaView.garantirCaixaAberto(this)) {
                atualizarPainel();
                status(Tipo.AVISO, "O PDV não foi aberto: é necessário um caixa aberto.");
                return;
            }
            setVisible(false);
            new PdvView(() -> {
                setVisible(true);
                posTela();
            }).setVisible(true);
        });
    }

    /** Chamado logo após o login: abre a prancheta se houver contas vencidas/próximas do vencimento. */
    public void mostrarLembretesInicial() {
        if (!lembretes.isEmpty()) SwingUtilities.invokeLater(this::aLembretes);
    }

    private void aLembretes() {
        tela(() -> {
            lembretes = contas.lembretes();
            LembreteDialog.mostrar(this, lembretes, () -> new ContaPagarView(this).setVisible(true));
        });
    }

    private void sair() {
        if (!Ui.confirmar(this, "Deseja sair do sistema?")) return;
        encerrar();
        System.exit(0);
    }

    private void trocarUsuario() {
        new AuthController().sair();
        encerrar();
        new LoginView(new AuthController()).setVisible(true);
    }

    private void encerrar() {
        statusTimer.stop();
        relogioTimer.stop();
        painelTimer.stop();
        dispose();
    }

    // ================================================================== painel de resumo
    private void atualizarPainel() {
        LocalDate hoje = LocalDate.now();
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("dd/MM HH:mm");

        try {
            caixaAtual = caixaCtrl.caixaAberto();
        } catch (RuntimeException e) {
            caixaAtual = Optional.empty();
        }
        caixaAtual.ifPresentOrElse(
                c -> cCaixa.set("ABERTO #" + c.id(), "Desde " + c.dataAbertura().format(hora) + " por " + c.abertoPor()),
                () -> cCaixa.set("FECHADO", "Abra o caixa para registrar vendas"));
        lblCaixa.setText(caixaAtual.map(c -> "Caixa #" + c.id() + " ABERTO").orElse("CAIXA FECHADO"));
        lblCaixa.setForeground(caixaAtual.isPresent() ? new Color(0x2E7D32) : new Color(0xC62828));

        try {
            List<Venda> doDia = vendasCtrl.listar(hoje, hoje).stream().filter(v -> "CONCLUIDA".equals(v.getStatus())).toList();
            BigDecimal total = doDia.stream().map(Venda::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            cVendas.set(Fmt.moeda(total), doDia.size() + " venda(s) concluída(s)");
        } catch (RuntimeException e) {
            cVendas.set("—", "Sem permissão para consultar vendas");
        }

        try {
            lembretes = contas.lembretes();
        } catch (RuntimeException e) {
            lembretes = List.of();
        }
        boolean podeContas = Sessao.pode(Permissao.GERENCIAR_CONTAS);
        long vencidas = lembretes.stream().filter(c -> c.situacao(hoje) == ContaPagar.Situacao.VENCIDA).count();
        if (!podeContas) cContas.set("—", "Sem permissão para contas a pagar");
        else if (lembretes.isEmpty()) cContas.set("Em dia", "Nenhuma conta vencida ou próxima do vencimento");
        else cContas.set(lembretes.size() + " conta(s)", vencidas > 0 ? vencidas + " vencida(s) - atenção!" : "vencendo nos próximos dias");
        lblContas.setText(!podeContas || lembretes.isEmpty() ? " " : "Contas a pagar: " + lembretes.size()
                + (vencidas > 0 ? " (" + vencidas + " vencida(s))" : ""));
        lblContas.setForeground(vencidas > 0 ? new Color(0xC62828) : new Color(0xE65100));
        if (btnLembretes != null) {
            btnLembretes.setText(lembretes.isEmpty() ? "Lembretes" : "Lembretes (" + lembretes.size() + ")");
            btnLembretes.setOpaque(!lembretes.isEmpty());
            btnLembretes.setContentAreaFilled(lembretes.isEmpty());
            btnLembretes.setBackground(vencidas > 0 ? new Color(0xFFCDD2) : new Color(0xFFF59D));
            btnLembretes.repaint();
        }

        try {
            long baixos = estoqueCtrl.listar("", true).stream().filter(Produto::baixoEstoque).count();
            cEstoque.set(baixos + " produto(s)", baixos > 0 ? "abaixo do estoque mínimo" : "Estoque em dia");
        } catch (RuntimeException e) {
            cEstoque.set("—", "Sem permissão para o estoque");
        }
    }
}
