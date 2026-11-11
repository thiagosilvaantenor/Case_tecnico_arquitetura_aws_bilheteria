# Amostra de Código — Sistema Gerenciador de TG

Conforme solicitado um código na minha linguagem de domínio, desvinculada do contexto do sistema de bilheteria. Escolhi pegar um trecho que fiz do meu Trabalho de Graduação, um sistema de gerenciamento da disciplina de Trabalhos de Graduação, embora fiz o trabalho em grupo esse trecho foi 100% de minha autoria.

Trata-se de um "corte vertical" (do Controller até a Infraestrutura de Banco de Dados), desenvolvido em **Java 25** com **Spring Boot 3** e **MySQL**. O sistema completo gerencia o ciclo de vida acadêmico de alunos e bancas de avaliação.

O caso de uso selecionado é o **Atribuir Notas da Banca**, responsável por receber as notas dos avaliadores, validar o estado da banca e calcular a média final.

## 🎯 Motivação da Escolha

Escolhi este recorte específico pois ele reflete diretamente minhas práticas diárias de engenharia de software e intenções de design, alinhando-se com a maturidade exigida para o desenho da arquitetura AWS apresentada:

* **Clean Architecture e DDD:** O domínio (`Banca`) é isolado de detalhes de infraestrutura e frameworks. Regras de negócio, como validação de status e cálculo de notas, residem na própria entidade (Modelo de Domínio Rico), e não em serviços anêmicos.
* **Isolamento de Persistência:** Utilizo classes distintas para a persistência (`BancaModelo` via Spring Data JPA) e para o domínio puro, conectadas por um *Mapper* explícito. Isso garante que o banco de dados relacional não dite o design do domínio.
* **Design Orientado a Eventos:** O caso de uso finaliza disparando um evento de domínio (`BancaAvaliadaEvento`), desacoplando rotinas secundárias (como a geração de certificados em PDF e envio de e-mails). É a mesma mentalidade de sistemas distribuídos aplicada no desenho da AWS com o uso de mensageria (SNS/SQS).
* **Uso de Java Moderno:** Utilização de `records` para DTOs (garantindo imutabilidade dos dados de tráfego) e respostas REST padronizadas no *Controller*.

## 📂 Estrutura do Recorte

Para facilitar a leitura, o código enviado está organizado respeitando a separação física das camadas da Arquitetura Limpa:

* **`/aplicacao/casodeuso`**: Contém o Caso de Uso principal (`AtribuirNotasBancaCaso`), que orquestra o fluxo, aciona o domínio e publica os eventos.
* **`/dominio`**: O coração da regra de negócio. Contém a entidade `Banca`, o objeto de valor `StatusBanca` e a interface do repositório (Inversão de Dependência). Não possui dependências do Spring.
* **`/infraestrutura`**: A borda do sistema. Contém o `BancaControlador` (REST API) e a camada de persistência com anotações do Hibernate (`BancaModelo`), o `BancaMapeador` e a implementação real do repositório (`BancaRepositorioImpl`).