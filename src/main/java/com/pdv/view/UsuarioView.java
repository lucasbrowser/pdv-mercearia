package com.pdv.view;

import com.pdv.controller.UsuarioController;
import com.pdv.model.Permissao;
import com.pdv.model.Usuario;
import com.pdv.util.NegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UsuarioView extends JDialog {
    private final UsuarioController ctrl = new UsuarioController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Nome", "Login", "Admin", "Ativo", "Permissões"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = Ui.tabela(modelo);
    private final JTextField nome = new JTextField(), login = new JTextField();
    private final JPasswordField senha = new JPasswordField();
    private final JCheckBox admin = new JCheckBox("Administrador (acesso total)");
    private final JCheckBox ativo = new JCheckBox("Usuário ativo", true);
    private final Map<Permissao, JCheckBox> checks = new EnumMap<>(Permissao.class);
    private final ModoCadastro modo;
    private List<Usuario> lista = List.of();
    private long idAtual = 0;

    public UsuarioView(Window pai) {
        super(pai, "Cadastro de Usuários", ModalityType.APPLICATION_MODAL);

        JPanel perms = new JPanel(new GridLayout(0, 1));
        perms.setBorder(BorderFactory.createTitledBorder("Permissões"));
        for (Permissao p : Permissao.values()) {
            JCheckBox cb = new JCheckBox(p.getDescricao());
            checks.put(p, cb);
            perms.add(cb);
        }

        JButton bNovo = Ui.botao("Novo", this::novo);
        JButton bEditar = Ui.botao("Editar", this::editar);
        JButton bSalvar = Ui.botao("Salvar", this::salvar);
        JButton bDescartar = Ui.botao("Descartar", this::descartar);
        modo = new ModoCadastro(bNovo, bEditar, bSalvar, bDescartar,
                new JComponent[]{nome, login, senha, admin, ativo}, new JComponent[0], new JComponent[]{tabela});
        modo.observar(nome, login, senha, admin, ativo);
        modo.observar(checks.values().toArray(new JComponent[0]));
        modo.dicaEdicao("Editando o usuário. Senha em branco mantém a senha atual.");
        modo.aoMudar(this::atualizarChecks); // as permissões dependem do modo e do perfil Administrador
        admin.addActionListener(e -> atualizarChecks());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Dados do usuário"));
        Ui.linha(form, 0, "Nome:", nome);
        Ui.linha(form, 1, "Login:", login);
        Ui.linha(form, 2, "Senha:", senha);
        Ui.linha(form, 3, "", admin);
        Ui.linha(form, 4, "", ativo);

        JPanel botoes = new JPanel();
        botoes.add(bNovo);
        botoes.add(bEditar);
        botoes.add(bSalvar);
        botoes.add(bDescartar);
        botoes.add(Ui.cancelar(this, () -> modo.podeSair(this)));

        JPanel direita = new JPanel(new BorderLayout());
        direita.setPreferredSize(new Dimension(430, 0));
        direita.add(modo.getRotulo(), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(form, BorderLayout.NORTH);
        centro.add(perms, BorderLayout.CENTER);
        centro.add(botoes, BorderLayout.SOUTH);
        direita.add(centro, BorderLayout.CENTER);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            int r = tabela.getSelectedRow();
            if (e.getValueIsAdjusting() || modo.editando() || r < 0) return;
            Usuario u = lista.get(r);
            modo.carregando(() -> preencher(u));
            modo.consulta(true);
        });

        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setSize(1050, 540);
        setLocationRelativeTo(pai);
        carregar();
    }

    private void carregar() {
        lista = ctrl.listar();
        modelo.setRowCount(0);
        for (Usuario u : lista)
            modelo.addRow(new Object[]{u.nome(), u.login(), u.admin() ? "Sim" : "Não", u.ativo() ? "Sim" : "Não",
                    u.admin() ? "Todas" : u.permissoes().stream().map(Permissao::getDescricao).collect(Collectors.joining(", "))});
        if (!modo.editando()) {
            idAtual = 0;
            modo.carregando(this::limpar);
            modo.consulta(false);
        }
    }

    /** Perfil Administrador marca e trava todas as permissões; fora de edição tudo fica bloqueado. */
    private void atualizarChecks() {
        checks.values().forEach(cb -> {
            if (admin.isSelected()) cb.setSelected(true);
            cb.setEnabled(modo.editando() && !admin.isSelected());
        });
    }

    private void limpar() {
        nome.setText("");
        login.setText("");
        senha.setText("");
        admin.setSelected(false);
        ativo.setSelected(true);
        checks.forEach((p, cb) -> cb.setSelected(p == Permissao.VENDER));
    }

    private void preencher(Usuario u) {
        idAtual = u.id();
        nome.setText(u.nome());
        login.setText(u.login());
        senha.setText(""); // vazio = mantém a senha atual
        admin.setSelected(u.admin());
        ativo.setSelected(u.ativo());
        checks.forEach((p, cb) -> cb.setSelected(u.permissoes().contains(p)));
    }

    private void novo() {
        tabela.clearSelection();
        idAtual = 0;
        modo.carregando(this::limpar);
        modo.novo();
        nome.requestFocusInWindow();
    }

    private void editar() {
        if (idAtual == 0) throw new NegocioException("Selecione um usuário na tabela para editar.");
        modo.edicao();
        nome.requestFocusInWindow();
    }

    private void salvar() {
        Set<Permissao> sel = EnumSet.noneOf(Permissao.class);
        checks.forEach((p, cb) -> { if (cb.isSelected()) sel.add(p); });
        ctrl.salvar(idAtual, nome.getText(), login.getText(), new String(senha.getPassword()),
                admin.isSelected(), ativo.isSelected(), sel);
        Ui.info(this, "Usuário salvo com sucesso.");
        modo.consulta(false);
        carregar();
    }

    private void descartar() {
        if (!modo.podeSair(this)) return;
        int r = tabela.getSelectedRow();
        boolean voltaParaRegistro = idAtual != 0 && r >= 0;
        modo.consulta(voltaParaRegistro);
        if (voltaParaRegistro) {
            Usuario u = lista.get(r);
            modo.carregando(() -> preencher(u));
        } else {
            idAtual = 0;
            modo.carregando(this::limpar);
        }
        atualizarChecks();
    }
}
