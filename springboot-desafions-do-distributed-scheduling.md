# Spring Boot: Os desafios do Distributed Scheduling | 🏢 Office Hours 🕑
- Data do estudo: 29/08/2025
- Fonte: [Youtube](https://www.youtube.com/watch?v=I_kEO_HPfBU&t=81s&ab_channel=ZupInnovation%3ASuaaliadatechdoagoraaofuturo)
- Apresentado por: Rafael Pontes

Código inicial:
```java
package com.example.jobs;

import com.example.clients.CardsClient;
import com.example.models.Card;
import com.example.models.Proposal;
import com.example.models.enums.Status;
import com.example.repositories.CardRepository;
import com.example.repositories.ProposalRepository;
import com.example.responses.CardDataResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AttachCardsToProposalsJob {

    private final CardsClient cardsClient;
    private final CardRepository cardRepository;
    private final ProposalRepository proposalRepository;

    public AttachCardsToProposalsJob(CardsClient cardsClient,
                                     CardRepository cardRepository,
                                     ProposalRepository proposalRepository) {
        this.cardsClient = cardsClient;
        this.cardRepository = cardRepository;
        this.proposalRepository = proposalRepository;
    }

    @Transactional
    @Scheduled(fixedDelay = 60_000)
    public void attach() {
        List<Proposal> proposals = proposalRepository
                .findAllByStatusOrderByCreatedAtAsc(Status.ELIGIBLE);

        for (Proposal proposal : proposals) {
            try {
                CardDataResponse cardData = cardsClient
                    .findCardByProposalId(proposal.getId());
                Card newCard = new Card(cardData);
                cardRepository.save(newCard);
                proposal.attachCard(newCard);
                proposalRepository.save(proposal);
            } catch (Exception e) {
                // Log de erro ou tratamento específico
                System.err.println("Erro ao processar proposta ID " 
                + proposal.getId() + ": " + e.getMessage());
            }
        }
    }
}
```

```java
package com.example.repositories;

import com.example.models.Proposal;
import com.example.models.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, UUID> {
    List<Proposal> findAllByStatusOrderByCreatedAtAsc(ProposalStatus status); //v1
    List<Proposal> findATop50ByStatusOrderByCreatedAtAsc(ProposalStatus status); //v2
}
```

16:58 e se sua aplicação parar por uma hora e tiver milhares de registros no banco de dados e sua consulta está buscando todos os registros  `findAllByStatusOrderByCreatedAtAsc`? A memória da aplicação irá subir em proporção da quantidade de registro do banco de dados.

Primeira otimização, busca top 50 no lugar de busca com todos os registros do banco de dados:
```java
//top50
@Transactional
@Scheduled(fixedDelay = 60_000)
public void execute() {
    while (true) {
        List<Proposal> proposals = proposalRepository
            .findTop50ByStatusOrderByCreatedAtAsc(Status.ELIGIBLE);

        if (proposals.isEmpty()) {
            break;
        }

        // processa cada proposta
        proposals.forEach(proposal -> {
            // lógica de processamento aqui
        });
    }
}

```


21:25 mesmo após a otimização, o uso de memória continua subindo. Estamo usando o @Transactional do Spring Data que tem o Hibernate com JPA. No ciclo de vida da execução do Job, o EntityManager mantém uma cópia, em cache (cache de primeiro nível), e só é eliminado pelo Garbage Collector no final da execução do Job porque está anotado como @Transactional (long running transacional).

24:00: **long running transactional** fala sobre o @Transactional, onde o EntityManager faz o cache. No loop de 50 itens acaba juntando diversos caches e somente limpa no final das execuções do método anotado com @Transactional.

27:58 **shoting-running transaction**

26:30 com a remoção do @Transactional, cada execução é finalizada, mas ficamos sem o rollback em caso de erro, onde existe diversas operações processadas. Solução é **controle transacional programático**.

```java
//TransactionTemplate

@Component
public class AttachCardsToProposalsJob {

    private TransactionTemplate transactionManager;

    @Scheduled(fixedDelay = 60_000)
    public void execute() {

        while (true) {
            transactionManager.execute(transaction -> {

                // executa código em escopo transacional

            });
        }
    }
}
```



32:24 synchronized, bloqueia thread, mas somente dentro de um processamento local, numa única JVM.
```java
//synchronized
@Component
public class AttachCardsToProposalsJob {

    @Scheduled(...)
    public synchronized void execute() {
        // executa lógica aqui
    }
}
```

35:00 lock distribuido (zookeeper, redis, mongo),mas já existe esta infraestrutura toda para o projeto? Caso não, pode ser possível utilizar um banco de dados como postgres. Vamos utilizar o **lock pessismista**.
```java
package com.example.repositories;

import com.example.models.Proposal;
import com.example.models.enums.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Proposal> findTop50ByStatusOrderByCreatedAtAsc(ProposalStatus status);
}
```

```sql
select p.*
  from proposal p
  where p.status = 'ELIGIBLE'
order by p.created_at asc
limit 50
for update
```

40:00 processamento paralelo, a solução seria uma fila e um consumidor. Rabit MQ ou Kafka? Já tenho essa infraestrutura? A equipe já domina a tecnologia? Podemos usar um banco de dados como uma fila.
```java
@QueryHints({
    @QueryHint(
        name = "javax.persistence.lock.timeout",
        value = LockOptions.SKIP_LOCKED
    )
})
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<Proposal> findTop50ByStatusOrderByCreatedAtAsc(ProposalStatus status);
```

```sql
select p.*
  from proposal p
  where p.status = 'ELIGIBLE'
order by p.created_at asc
limit 50
for update skip locked
```

43:37 se impactar o startup da aplicação? Ao iniciar o spring, é inicializado o agendamento para execução que pode impactar a execução da aplicação. Podemos configurar um delay para iniciar o agendamento, somente quando a aplicação já estiver executando com sucesso.

```java
@Scheduled(
    fixedDelay = 60_000,
    initialDelay = 120_000
)
public void execute() {
    // executa lógica aqui
}
```

45:11 a aplicação continua demorada com problemas no throughtput, sem considerar outros pontos de gargalos e assumirmos que o problema está no banco de dados, podemos definir batch size. No loop dos registros do banco (top 50), realizamos diversas chamadas ao banco de dados, pode ser este o problema, o gargalo. Ou talvez essa operação no banco de dados nem seja o problema, o problema real é a latência, o tempo de resposta para ir e voltar ao banco de dados. Para diminuir esta latência pode-se trabalhar com batch size de 50 propostas (top 50).


```
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

Ao configurar o o batch size, agora será realizado 2 "roundtrips", uma para o insert e outra para o update.
```sql
INSERT INTO card(cardNumber) VALUES (?)
Params:
('5513-8141-9261-0001',
 '5513-8141-9261-0002',
 '5513-8141-9261-0003',
 '5513-8141-9261-0004')

UPDATE proposal SET status = ? WHERE id = ?;
Params:
('ELIGIBLE_WITH_ATTACHED_CARD', 2021),
('ELIGIBLE_WITH_ATTACHED_CARD', 2022),
('ELIGIBLE_WITH_ATTACHED_CARD', 2023),
('ELIGIBLE_WITH_ATTACHED_CARD', 2024)
```

48:40 e se ocorrer erro de integração com os serviços? 

- Numa chamada de api fazemos uma espera infinita? **timeout**
- Erros intermitentes? **retry**
- posso enviar mais de uma vez uma requisição? **idempotência**

Gerado por IA:

Estamos falando de sistemas distribuídos. Isso significa que, a todo momento, nosso código está se comunicando pela rede — seja para acessar o banco de dados, seja para interagir com sistemas externos, como o sistema da matriz bancária, por exemplo.

Essas chamadas HTTP, RPC ou qualquer outro protocolo de comunicação estão operando sobre a rede, e por isso estão suscetíveis às famosas oito falácias da computação distribuída. Elas voltam para atormentar os desenvolvedores sempre que trabalhamos com sistemas distribuídos. Vivemos e morremos por essas falácias.

Ao escrever uma simples linha de código que faz uma chamada remota, precisamos pensar: e se essa chamada falhar? O comando vai esperar indefinidamente? É necessário configurar um timeout. Mas qual deve ser esse timeout? Como determinar o tempo ideal?

Se ocorrer um erro — como uma falha intermitente de rede — o que fazer? Parar? Continuar? Tentar novamente? Usar um mecanismo de retry? Com que frequência devemos fazer essas tentativas? Quantas vezes devemos tentar reconectar?

E se decidirmos reenviar a requisição após um erro, será que a máquina do outro lado já recebeu a primeira tentativa? Se mandarmos de novo, corremos o risco de duplicar a operação, causar inconsistência ou até quebrar o sistema.

Ao trabalhar com sistemas distribuídos, estamos sujeitos a todos esses tipos de falhas. Não basta apenas saber que é preciso configurar um timeout ou implementar um retry. É preciso entender como fazer isso corretamente — e isso pode ser bem mais desafiador do que parece.

O código por si só não resolve tudo. O desenvolvedor precisa pensar diferente. Ele deve se perguntar: quantos “e se” existem nessa funcionalidade? Quanto mais você entende os fundamentos de computação paralela, concorrência e sistemas distribuídos, mais você enxerga os trade-offs, os problemas e os desafios envolvidos na implementação de uma funcionalidade aparentemente simples.
