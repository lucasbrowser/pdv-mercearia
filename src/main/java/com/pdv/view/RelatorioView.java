package com.pdv.view;

import com.pdv.controller.RelatorioController;
import com.pdv.controller.RelatorioController.Relatorio;
import com.pdv.model.TipoRelatorio;
import com.pdv.util.Csv;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RelatorioView extends JDialog {
    private final RelatorioController ctrl = new RelatorioController();
    private final JComboBox<TipoRelatorio> tipo = new JComboBox<>(TipoRelatorio.values());
    private final DatePicker de = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker ate = new DatePicker(LocalDate.now());
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = Ui.tabela(modelo);
    private final JLabel titulo = new JLabel(" ");
    private final JLabel rodape = new JLabel(" ");
    private Relatorio atual;

    public RelatorioView(Window pai) {
        super(pai, "Relatórios", ModalityType.APPLICATION_MODAL);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 15f));
        rodape.setFont(rodape.getFont().deriveFont(Font.BOLD));
        rodape.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Relatório:"));
        filtros.add(tipo);
        filtros.add(new JLabel("De:"));
        filtros.add(de);
        filtros.add(new JLabel("Até:"));
        filtros.add(ate);
        filtros.add(Ui.botao("Gerar", "relatorios", 'G', this::gerar));
        filtros.add(Ui.botao("Exportar CSV", "exportar", 'X', this::exportar));
        filtros.add(Ui.cancelar(this));
        Ui.igualar(filtros);
        tipo.addActionListener(e -> {
            boolean p = ((TipoRelatorio) tipo.getSelectedItem()).usaPeriodo();
            de.setEnabled(p);
            ate.setEnabled(p);
        });

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(filtros, BorderLayout.NORTH);
        topo.add(titulo, BorderLayout.SOUTH);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 10));

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        setSize(1150, 600);
        setLocationRelativeTo(pai);
    }

    private void gerar() {
        LocalDate d1, d2;
        try {
            d1 = LocalDate.parse(de.getText().trim(), Fmt.DATA);
            d2 = LocalDate.parse(ate.getText().trim(), Fmt.DATA);
        } catch (DateTimeParseException e) {
            throw new NegocioException("Data inválida. Use o formato dd/MM/aaaa.");
        }
        atual = ctrl.gerar((TipoRelatorio) tipo.getSelectedItem(), d1, d2);
        modelo.setDataVector(atual.linhas().toArray(new Object[0][]), atual.colunas());
        for (int c = 0; c < tabela.getColumnCount(); c++) tabela.getColumnModel().getColumn(c).setPreferredWidth(c == 1 ? 260 : 120);
        titulo.setText(atual.titulo() + "  —  " + atual.linhas().size() + " linha(s)");
        rodape.setText(atual.rodape());
    }

    private void exportar() {
        if (atual == null) throw new NegocioException("Gere um relatório antes de exportar.");
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(atual.titulo().replaceAll("[^A-Za-z0-9]+", "_") + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Csv.escrever(fc.getSelectedFile().toPath(), atual.colunas(), atual.linhas());
        Ui.info(this, "Arquivo salvo em:\n" + fc.getSelectedFile());
    }
}
