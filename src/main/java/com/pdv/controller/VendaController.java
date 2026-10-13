package com.pdv.controller;

import com.pdv.dao.CaixaDAO;
import com.pdv.dao.ProdutoDAO;
import com.pdv.dao.VendaDAO;
import com.pdv.model.*;
import com.pdv.util.Config;
import com.pdv.util.Fmt;
import com.pdv.util.NegocioException;
import com.pdv.util.Sessao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Controla a venda em andamento no PDV (carrinho) e a finalização. */
public class VendaController {
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final CaixaDAO caixaDAO = new CaixaDAO();
    private Venda venda = new Venda();

    public Venda getVenda() { return venda; }

    public Optional<Caixa> caixaAberto() { return caixaDAO.buscarAberto(); }

    /**
     * Aceita: "7891000100103", "3*7891000100103", "0,350*codigo" (peso) e etiqueta de balança EAN-13
     * (2 CCCC PPPPP D, peso em gramas). Produto por peso sem quantidade lança PesoObrigatorioException.
     */
    public ItemVenda adicionarPorCodigo(String entrada) {
        BigDecimal qtd = null;
        String cod = entrada.trim();
        int pos = cod.indexOf('*');
        if (pos > 0) {
            qtd = Fmt.quantidade(cod.substring(0, pos), "quantidade", true);
            cod = cod.substring(pos + 1).trim();
        }
        final String codigo = cod;
        Optional<Produto> exato = produtoDAO.porCodigo(codigo);
        if (exato.isEmpty() && qtd == null && ehEtiquetaBalanca(codigo)) {
            String item = codigo.substring(1, 5);
            BigDecimal peso = BigDecimal.valueOf(Long.parseLong(codigo.substring(5, 10)), 3);
            Produto p = produtoDAO.porCodigo(item)
                    .orElseThrow(() -> new NegocioException("Produto de balança não cadastrado (código " + item + ")."));
            return adicionar(p, peso);
        }
        Produto p = exato.orElseThrow(() -> new NegocioException("Produto não cadastrado: " + codigo));
        if (qtd == null) {
            if (p.fracionado()) throw new PesoObrigatorioException(p);
            qtd = BigDecimal.ONE;
        }
        return adicionar(p, qtd);
    }

    private boolean ehEtiquetaBalanca(String cod) {
        String prefixo = Config.get("balanca.prefixo").trim();
        return Config.getBool("balanca.etiqueta") && !prefixo.isEmpty() && cod.length() == 13
                && cod.startsWith(prefixo) && cod.chars().allMatch(Character::isDigit);
    }

    public ItemVenda adicionar(Produto escolhido, BigDecimal qtd) {
        Sessao.exigir(Permissao.VENDER);
        if (qtd.signum() <= 0) throw new NegocioException("Quantidade inválida.");
        Produto p = produtoDAO.porId(escolhido.id()).orElseThrow(() -> new NegocioException("Produto não encontrado."));
        if (!p.ativo()) throw new NegocioException("Produto inativo: " + p.descricao());
        if (!p.fracionado() && qtd.stripTrailingZeros().scale() > 0)
            throw new NegocioException(p.descricao() + " é vendido por unidade (quantidade inteira).");
        ItemVenda item = venda.getItens().stream().filter(i -> i.getProdutoId() == p.id()).findFirst().orElse(null);
        BigDecimal total = (item == null ? BigDecimal.ZERO : item.getQuantidade()).add(qtd);
        if (total.compareTo(p.estoque()) > 0)
            throw new NegocioException("Estoque insuficiente para " + p.descricao() + ". Disponível: " + Fmt.qtd(p.estoque()) + " " + p.unidade());
        if (item == null) {
            item = new ItemVenda(p.id(), p.codigoBarras(), p.descricao(), p.unidade(), qtd, p.preco());
            venda.getItens().add(item);
        } else {
            item.setQuantidade(total);
        }
        return item;
    }

    public void alterarQuantidade(int indice, BigDecimal qtd) {
        ItemVenda item = venda.getItens().get(indice);
        if (qtd.signum() <= 0) { removerItem(indice); return; }
        Produto p = produtoDAO.porId(item.getProdutoId()).orElseThrow(() -> new NegocioException("Produto não encontrado."));
        if (qtd.compareTo(p.estoque()) > 0)
            throw new NegocioException("Estoque insuficiente. Disponível: " + Fmt.qtd(p.estoque()) + " " + p.unidade());
        item.setQuantidade(qtd);
        ajustarDesconto();
    }

    public void removerItem(int indice) {
        venda.getItens().remove(indice);
        ajustarDesconto();
    }

    /** "10%" ou valor em reais ("5,00"). */
    public void aplicarDesconto(String entrada) {
        Sessao.exigir(Permissao.CONCEDER_DESCONTO);
        BigDecimal sub = venda.getSubtotal();
        String t = entrada.trim();
        BigDecimal d = t.endsWith("%")
                ? sub.multiply(Fmt.parse(t.substring(0, t.length() - 1))).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : Fmt.parse(t).setScale(2, RoundingMode.HALF_UP);
        if (d.signum() < 0 || d.compareTo(sub) > 0) throw new NegocioException("Desconto inválido.");
        venda.setDesconto(d);
    }

    public void cancelarVenda() { venda = new Venda(); }

    public Venda finalizar(String forma, String recebido) {
        Sessao.exigir(Permissao.VENDER);
        if (venda.getItens().isEmpty()) throw new NegocioException("Nenhum item na venda.");
        Caixa caixa = caixaDAO.buscarAberto().orElseThrow(() -> new NegocioException("Caixa fechado. Abra o caixa antes de vender."));
        BigDecimal total = venda.getTotal();
        BigDecimal valor = FormaPagamento.DINHEIRO.equals(forma) && recebido != null && !recebido.isBlank()
                ? Fmt.parse(recebido) : total;
        if (valor.compareTo(total) < 0) throw new NegocioException("Valor recebido menor que o total da venda.");
        Usuario u = Sessao.atual();
        venda.setUsuarioId(u.id());
        venda.setUsuarioNome(u.nome());
        venda.setCaixaId(caixa.id());
        venda.setFormaPagamento(forma);
        venda.setValorRecebido(valor);
        venda.setDataHora(LocalDateTime.now());
        vendaDAO.salvar(venda); // se falhar (ex.: estoque), o carrinho é preservado
        Venda concluida = venda;
        venda = new Venda();
        return concluida;
    }

    public List<Produto> pesquisarProdutos(String termo) { return produtoDAO.pesquisar(termo, true); }

    private void ajustarDesconto() {
        if (venda.getDesconto().compareTo(venda.getSubtotal()) > 0) venda.setDesconto(BigDecimal.ZERO);
    }
}
