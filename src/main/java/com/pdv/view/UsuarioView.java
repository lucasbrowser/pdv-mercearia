package com.pdv.view;

import com.pdv.controller.UsuarioController;
import com.pdv.model.Permissao;
import com.pdv.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.EnumSet;
import java.util.EnumMap;
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
        admin.addActionListener(e -> atualizarChecks());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Dados do usuário"));
        Ui.linha(form, 0, "Nome:", nome);
        Ui.linha(form, 1, "Login:", login);
        Ui.linha(form, 2, "Senha:", senha);
        Ui.linha(form, 3, "", admin);
        Ui.linha(form, 4, "", ativo);

        JPanel botoes = new JPanel();
        botoes.add(Ui.botao("Novo", this::novo));
        botoes.add(Ui.botao("Salvar", this::salvar));

        JPanel direita = new JPanel(new BorderLayout());
        direita.setPreferredSize(new Dimension(380, 0));
        direita.add(form, BorderLayout.NORTH);
        direita.add(perms, BorderLayout.CENTER);
        direita.add(botoes, BorderLayout.SOUTH);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabela.getSelectedRow() >= 0) preencher(lista.get(tabela.getSelectedRow()));
        });

        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(direita, BorderLayout.EAST);
        setSize(1000, 520);
        setLocationRelativeTo(pai);
        carregar();
        novo();
    }

    private void carregar() {
        lista = ctrl.listar();
        modelo.setRowCount(0);
        for (Usuario u : lista)
            modelo.addRow(new Object[]{u.nome(), u.login(), u.admin() ? "Sim" : "Não", u.ativo() ? "Sim" : "Não",
                    u.admin() ? "Todas" : u.permissoes().stream().map(Permissao::getDescricao).collect(Collectors.joining(", "))});
    }

    private void atualizarChecks() {
        checks.values().forEach(cb -> { cb.setEnabled(!admin.isSelected()); if (admin.isSelected()) cb.setSelected(true); });
    }

    private void novo() {
        idAtual = 0;
        tabela.clearSelection();
        nome.setText(""); login.setText(""); senha.setText("");
        admin.setSelected(false);
        ativo.setSelected(true);
        checks.forEach((p, cb) -> cb.setSelected(p == Permissao.VENDER));
        atualizarChecks();
        nome.requestFocusInWindow();
    }

    private void preencher(Usuario u) {
        idAtual = u.id();
        nome.setText(u.nome());
        login.setText(u.login());
        senha.setText(""); // vazio = mantém a senha atual
        admin.setSelected(u.admin());
        ativo.setSelected(u.ativo());
        checks.forEach((p, cb) -> cb.setSelected(u.permissoes().contains(p)));
        atualizarChecks();
    }

    private void salvar() {
        Set<Permissao> sel = EnumSet.noneOf(Permissao.class);
        checks.forEach((p, cb) -> { if (cb.isSelected()) sel.add(p); });
        ctrl.salvar(idAtual, nome.getText(), login.getText(), new String(senha.getPassword()),
                admin.isSelected(), ativo.isSelected(), sel);
        Ui.info(this, "Usuário salvo. Em edição, deixar a senha em branco mantém a senha atual.");
        carregar();
        novo();
    }
}
