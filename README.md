# PDV Mercado de Bairro (Java 21 + Swing + SQLite)

## Executar
Requisitos: JDK 21 e Maven.

    mvn clean package
    java -jar target/pdv-mercado-1.0.0.jar

Primeiro acesso: **admin / admin123** (troque a senha em Menu > Usuários).

Dados em `~/pdv-mercado/` (`pdv.db` = banco, `config.properties` = loja e impressora).

## Arquitetura MVC
    com.pdv
    ├── App.java                 ponto de entrada
    ├── model/                   entidades (Usuario, Produto, Venda, ItemVenda, Permissao, TipoMovimento...)
    ├── dao/                     acesso a dados JDBC (UsuarioDAO, ProdutoDAO, EstoqueDAO, VendaDAO)
    ├── controller/              regras de negócio + checagem de permissões
    ├── view/                    telas Swing (Login, Main, PDV, Produtos, Estoque, Usuários, Vendas)
    └── util/                    Config, Database, Seguranca (PBKDF2), Sessao, Fmt, ImpressoraCupom (ESC/POS)

A view nunca acessa DAO/SQL: ela chama o controller, que valida permissões (`Sessao.exigir`) e regras.

## Permissões
Administrador tem acesso total. Demais usuários recebem permissões individuais:
Operar caixa, Conceder desconto, Consultar vendas, Cancelar vendas, Gerenciar produtos,
**Administrar estoque**, Gerenciar usuários.

## Estoque
- O estoque só muda por movimentações registradas (entrada, saída/perda, ajuste de contagem,
  venda, cancelamento) com usuário, data e motivo (aba "Movimentações").
- Venda e baixa de estoque ocorrem na mesma transação; venda sem estoque suficiente é bloqueada.
- Cancelar uma venda devolve os itens ao estoque.
- Produtos com estoque <= mínimo aparecem em vermelho.

## Tela de vendas (PDV)
- O foco fica sempre no campo de código: o leitor USB (modo teclado) digita o código + Enter.
- `3*7891000100103` adiciona 3 unidades. Texto abre a pesquisa por descrição.
- F2 Pesquisar | F3 Alterar qtd | F4 Remover item | F5 Desconto (R$ ou %) | F8 Cancelar venda |
  F10 Finalizar (dinheiro com troco, débito, crédito, PIX) | Esc Sair.

## Impressora térmica (cupom NÃO fiscal)
Envia ESC/POS direto via `javax.print` (sem bibliotecas extras), com corte de papel.
Em `config.properties`:

    impressora.nome=        # nome da impressora instalada no SO (vazio = padrão)
    impressora.colunas=48   # 48 = 80mm | 32 = 58mm
    impressora.corte=true
    impressora.ativa=true

Windows: instale a impressora (driver do fabricante ou "Generic / Text Only") e informe o nome exato.
Linux: crie a fila no CUPS em modo raw. Reimprima/visualize cupons em Menu > Vendas.

## Venda por peso (v1.1)
- Unidades **KG** e **LT** são fracionadas (até 3 casas, ex.: 0,350 kg). O preço cadastrado é por KG/LT.
- No PDV, bipar um produto por peso abre a pergunta do peso lido na balança; ou digite `0,350*código`.
- Etiqueta de balança EAN-13 (`2 CCCC PPPPP D`, peso em gramas) é reconhecida: cadastre o produto com o
  código de 4 dígitos (CCCC). Configurável em `balanca.etiqueta` / `balanca.prefixo`.
- Subtotal arredondado em centavos; estoque e cupom mostram a quantidade com a unidade.
- Bancos da v1.0 são migrados automaticamente (quantidades gravadas em milésimos). **Faça backup de `pdv.db` antes de atualizar.**

## Caixa (v1.1)
- É preciso **abrir o caixa** (informando o fundo de troco) para vender; o PDV oferece abrir ao entrar.
- **Sangria** (F6 no PDV ou Menu > Caixa) exige motivo, não pode exceder o dinheiro da gaveta e imprime comprovante.
  **Suprimento** faz o reforço de troco.
- **Fechamento**: dinheiro esperado = fundo + vendas em dinheiro + suprimentos - sangrias. O operador informa o
  valor contado; sobra/falta é registrada (observação obrigatória se houver diferença) e o relatório é impresso.
- Vendas de um caixa já fechado não podem ser canceladas (fechamentos ficam imutáveis).
- Permissão nova: "Abrir/fechar caixa, sangria e suprimento" (usuários existentes precisam recebê-la).

## Relatórios (v1.1) - Menu > Relatórios (permissão "Consultar relatórios")
Vendas por dia, por forma de pagamento, por operador, produtos mais vendidos, posição/valor de estoque e
fechamentos de caixa (com sobras/faltas). Todos com filtro de período e **exportação CSV** (abre no Excel).

## Fornecedores e contas a pagar (v1.2)
- **Menu > Fornecedores**: cadastro (nome, CNPJ/CPF, contato, telefone, e-mail, endereço, observação; inativar em vez de excluir).
- **Menu > Contas a pagar**: nova conta com fornecedor, descrição, nº do documento, valor, vencimento e
  **parcelamento mensal** (1 a 60 parcelas; os centavos que sobram ficam na 1ª parcela). Editar/cancelar só enquanto em aberto.
  Linhas coloridas: vencida (vermelho), vence hoje (laranja), paga (verde). Totais de aberto/vencido/pago no rodapé.
- **Lembretes em forma de prancheta (clipboard)**: cada conta tem "avisar N dias antes" (padrão 3). Ao entrar no sistema
  abre uma prancheta com VENCIDAS / VENCEM HOJE / PRÓXIMOS VENCIMENTOS. O botão "Lembretes" no menu fica amarelo (ou vermelho
  se houver vencidas) e é reavaliado a cada 15 min; o PDV mostra "Contas a pagar: N" no cabeçalho.
- **Pagamento**: formas "Dinheiro do caixa", PIX/Transferência, Boleto/Banco, Cartão e Outros.
  - *Dinheiro do caixa*: exige caixa aberto e saldo na gaveta; a saída é lançada no caixa **na mesma transação**
    (movimento "Pagamento de conta"), reduz o dinheiro esperado no fechamento, aparece no resumo/relatório do caixa
    e imprime comprovante.
- Novo relatório: **Contas a pagar (por vencimento)**; o relatório de fechamentos soma sangrias + pagamentos de contas.
- Permissão nova: "Fornecedores e contas a pagar". Pagar com dinheiro do caixa exige também a permissão de caixa.
- O banco é migrado automaticamente (esquema v3). Faça backup do `pdv.db` antes de atualizar.

## Limitações conhecidas / próximos passos
- Cupom não fiscal; para NFC-e/SAT é necessário integração com SEFAZ/emissor.
- Terminal único (um caixa aberto por vez); sem custo de compra (relatório de estoque usa preço de venda).
- Pagamento de conta não pode ser estornado pela tela (corrija com ajuste/suprimento no caixa, se necessário).
