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

## Limitações conhecidas / próximos passos
- Quantidades inteiras (sem venda por peso em decimais).
- Cupom não fiscal; para NFC-e/SAT é necessário integração com SEFAZ/emissor.
- Sem fechamento/sangria de caixa e sem relatórios além do histórico de vendas.
