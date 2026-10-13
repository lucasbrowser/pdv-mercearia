package com.pdv.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResumoCaixa(Caixa caixa, Map<String, BigDecimal> vendasPorForma, int qtdVendas, int qtdCanceladas,
                          BigDecimal totalVendas, BigDecimal suprimentos, BigDecimal sangrias,
                          BigDecimal pagamentosContas, BigDecimal esperadoDinheiro, List<MovimentoCaixa> movimentos) { }
