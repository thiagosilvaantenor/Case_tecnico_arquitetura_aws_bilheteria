# Arquitetura AWS — Sistema de Bilheteria para Cinemas

## 1. Visão geral

Este projeto apresenta uma proposta de arquitetura AWS para um sistema de bilheteria, foi considerado o dominio especifico bilheteria de cinemas, permitindo a consulta de filmes e sessões, reserva de assentos, compra de ingressos e aquisição de produtos adicionais, como pipoca, chocolate e refrigerantes.

A solução foi planejada considerando escalabilidade, segurança, disponibilidade e consistência dos dados, principalmente nas operações de reserva e pagamento.

**Região considerada:** `sa-east-1` (São Paulo).

## 2. Arquitetura e tecnologias

A solução está dividida em camadas, conforme apresentado no diagrama arquitetural.

| Camada | Serviços e tecnologias | Responsabilidade |
|---|---|---|
| Apresentação | S3 e CloudFront | Hospedagem e distribuição  do frontend|
| Entrada | API Gateway e Cognito | Exposição das APIs REST e autenticação por JWT. |
| Aplicação | Java, Spring Boot e Lambda | BFF e módulos responsáveis pelas regras de negócio. |
| Dados | Aurora PostgreSQL, RDS Proxy, DynamoDB e Redis | Persistência transacional, carrinhos temporários e cache. |
| Assíncrona | EventBridge, SNS, SQS, Lambda e SES | Agendamentos, emissão de ingressos e notificações. |

### Camada de aplicação

O backend utiliza Java com Spring Boot, executado em AWS Lambda.

O BFF (Backend for Frontends) recebe as requisições do frontend e orquestra as operações dos módulos de domínio, separados por responsabilidade:

- **Catálogo:** consulta de filmes.
- **Sessões:** consulta de cinemas, salas e horários.
- **Reservas:** validação de disponibilidade e reserva de assentos.
- **Pedidos:** carrinho e produtos adicionais.
- **Pagamentos:** integração com o provedor externo.

Essa separação facilita a manutenção e permite evolução independente dos módulos.

## 3. Principais decisões arquiteturais

### Aurora PostgreSQL + DynamoDB

O Aurora PostgreSQL é o banco principal e a fonte da verdade para as operações transacionais, armazenando sessões, assentos, reservas, pedidos e pagamentos.

Sua escolha considera a necessidade de transações ACID, integridade referencial e controle de concorrência, especialmente para impedir que um mesmo assento seja reservado simultaneamente por clientes diferentes.

O RDS Proxy gerencia as conexões das funções Lambda com o banco, reduzindo a sobrecarga causada por execuções simultâneas.

O DynamoDB é utilizado para armazenar carrinhos temporários e os produtos adicionais selecionados pelo cliente. Essa separação evita utilizar o banco relacional para informações temporárias com padrões de acesso mais simples.

### ElastiCache for Redis

O Redis é utilizado para armazenar informações frequentemente consultadas e com menor volatilidade, como filmes em cartaz, cinemas e catálogo de produtos.

A estratégia adotada é Cache-Aside: a aplicação consulta o cache e, em caso de ausência, recupera os dados no Aurora e atualiza o Redis.

A disponibilidade dos assentos não utiliza o cache como fonte de verdade. A confirmação de uma reserva sempre realiza uma nova validação transacional no Aurora, evitando conflitos causados por informações desatualizadas.

### Processamento assíncrono — SNS e SQS

Após a confirmação de um pedido, o evento `OrderConfirmed` é publicado no SNS, que distribui a mensagem para duas filas SQS independentes:

- **Ingressos:** aciona uma Lambda responsável pela emissão e armazenamento dos ingressos no S3.
- **Notificações:** aciona uma Lambda responsável pelo envio de e-mails utilizando o SES.

Essa abordagem desacopla as atividades, permitindo que falhas no envio de notificações não interrompam a emissão dos ingressos.

As filas possuem políticas de retentativa e DLQ. Os consumidores devem implementar idempotência, pois uma mesma mensagem pode ser entregue mais de uma vez.

### Transactional Outbox

Para evitar inconsistências entre a confirmação de um pedido e a publicação do evento correspondente, é utilizado o padrão Transactional Outbox.

O pedido e seu evento são registrados na mesma transação do Aurora, em uma tabela chamada `OutboxEvents`.

O EventBridge Scheduler aciona periodicamente a Lambda Outbox Publisher, que consulta os eventos pendentes por meio do RDS Proxy, publica as mensagens no SNS e atualiza seu estado no banco.

Essa abordagem reduz o risco de um pedido ser confirmado sem que os processos assíncronos sejam notificados.

Como uma falha pode ocorrer após a publicação e antes da atualização do registro, o mecanismo considera possíveis publicações duplicadas. Por isso, os consumidores devem ser idempotentes.

### Expiração de reservas

O EventBridge Scheduler também aciona uma Lambda responsável por localizar reservas expiradas e liberar os respectivos assentos no Aurora.

Além dessa rotina periódica, a validade das reservas é verificada durante novas solicitações, garantindo que reservas vencidas não sejam tratadas como válidas enquanto aguardam o próximo agendamento.

A frequência de execução e o volume de registros processados devem ser dimensionados conforme a demanda.

## 4. Fluxo de compra e pagamento

1. O cliente consulta filmes e sessões disponíveis.
2. Seleciona os assentos desejados, que são validados e reservados transacionalmente no Aurora.
3. Adiciona produtos complementares ao carrinho, armazenado no DynamoDB.
4. Revisa o pedido e visualiza o valor total antes de iniciar o pagamento.
5. O backend valida novamente os preços e registra os valores confirmados no pedido.
6. A Lambda de Pagamentos solicita o processamento ao provedor externo.
7. O resultado é recebido por meio de um webhook HTTPS, encaminhado pelo API Gateway à Lambda de Pagamentos.
8. Após a aprovação, o pedido é confirmado no Aurora e o evento é registrado na Outbox.
9. O evento é publicado no SNS e distribuído às filas SQS para emissão dos ingressos e envio de notificações.

A validação de preços no backend impede que valores manipulados pelo frontend sejam utilizados no pedido. Após a confirmação, os preços são preservados nos itens do pedido.

A integração com o provedor deve utilizar idempotência, timeout, retentativas controladas e Circuit Breaker. Caso um pagamento seja aprovado após a expiração da reserva, o sistema deve garantir novamente os assentos ou prever o estorno, evitando emitir ingressos sem disponibilidade confirmada.

## 5. Segurança e observabilidade

A arquitetura considera:

- **IAM:** permissões específicas por função, seguindo o princípio do menor privilégio.
- **WAF:** camada de proteção ao cloudfront.
- **Secrets Manager:** armazenamento de credenciais e segredos de integração.
- **KMS:** gerenciamento de chaves e criptografia dos dados.
- **CloudWatch:** centralização de logs, métricas, alarmes e dashboards.
- **X-Ray:** rastreamento das requisições e identificação de gargalos.
- **VPC:** isolamento do banco de dados em sub-redes privadas.

Os logs devem utilizar formato estruturado e identificadores de correlação, evitando registrar credenciais, tokens ou dados sensíveis de pagamento.

O provisionamento da infraestrutura é realizado com Terraform, enquanto o GitHub Actions é utilizado para automatizar etapas de compilação, testes e implantação.

## 6. Considerações finais e trade-offs

As principais escolhas buscam equilibrar consistência, escalabilidade e complexidade operacional:

- **Aurora + DynamoDB:** atende a diferentes padrões de armazenamento, mas exige fronteiras claras de responsabilidade entre os bancos.
- **Redis:** reduz consultas repetitivas, mas exige controle de expiração e atualização do cache.
- **Lambda:** permite execução sob demanda, mas exige atenção a cold starts e gerenciamento de conexões.
- **SNS + SQS:** desacopla processos e absorve picos, mas exige tratamento de mensagens duplicadas e falhas.
- **Transactional Outbox:** aumenta a confiabilidade da publicação de eventos, adicionando um processo de monitoramento e processamento de registros pendentes.

A proposta utiliza serviços gerenciados da AWS para construir uma solução modular e escalável, mantendo as operações críticas de reserva e compra sob controle transacional e delegando atividades complementares ao processamento assíncrono.