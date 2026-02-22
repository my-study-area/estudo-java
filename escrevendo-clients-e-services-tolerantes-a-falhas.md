# Escrevendo Clients e Services Tolerantes a Falhas com Rafael Ponte | 💻 Zup Open Talks 🚀
- Data: 01/09/2025
- Fonte: [Youtube](https://www.youtube.com/watch?v=TMmN9cR_IsM&ab_channel=ZupInnovation%3ASuaaliadatechdoagoraaofuturo)

11:34 O que é um sistema tolerante a falhas?

Indicação de livro: Designing data-intensive applications: Martin Kleppmann

8 faláscias da computação distribuída:
- 1: a rede é confiável
- 2: latência zero
- 3: banda infinita
- 4: a rede é segura
- 5: topologia não muda
- 6: existe somente um administrador
- 7: o custo de transporte de dados é zero
- 8: a rede é homogênea. Somente tipos de dispositivos e conexõs iguais

15:31 Desenhado sistemas distribuídos

- Chamadas locais: ocorrem na mesma aplicação. O seu sistema se comunicando com alguma api interna, dentro do próprio sistema, sem a necessidade de entrar na Internet.
- Chamadas remotas: sua aplicação fazendo chamadas para sistemas remotos. Sistemas por problemas da rede, não consegue responder e etc.

> Martin Fowler: First law of distributed Object Design: Dont'n distribute your objects.

Exemplo de código não preparado para tratar falhas, implementação ingênua:
```java
@restController
public class CalculadoraDeFretesController {

    @GetMapping(path = "/fretes/calcula")
    public ResponseEntity<Frete> calcula(@RequestParam String cep) {
        // Consulta frete em outro microsserviço
        String url = "https://ms.fast-fretes.com/calcula-frete";
        
        ZupHttpClient<Frete> client = new ZupHttpClient<>();
        Frete frete = client.get(url)
                            .withParameter("cep", cep)
                            .execute();
        
        return ResponseEntity.ok(frete);
    }
}
```

O primeiro problema pode ser a falta de um timeout e aplicação fica esperando uma resposta por um tempo indeterminado.

```java
ZupClientConfig config = ZupClientConfig.custom()
    .withRequestTimeout(Duration.of(5, SECONDS))
    .build();

ZupHttpClient<Frete> client = new ZupHttpClient<>(config);
Frete frete = client.get(url)
    .withParameter("cep", cep)
    .execute();
```

33:20 Transient failures (falhas intermitentes). Em caso de problema de rede a aplicação retorna um erro ao usuário que realiza uma nova tentativa. Não podemos jogar esse problema para o usuário, o sistema deve realizar um retentativa (retry). Não podemos esquecer de definir o número de tentativas ao realizar o retry, assim evitamos uma enchurrada de novas requisições ao servidor.
```java
ZupClientConfig config = ZupClientConfig.custom()
    .withRequestTimeout(Duration.of(5, SECONDS))
    .withRetryPolicy(RetryPolicy.custom()
        .retryOn(HttpStatus5xxException.class)
        .withMaxAttempts(3)
        .build())
    .build();

ZupHttpClient<Frete> client = new ZupHttpClient<>(config);
Frete frete = client.get(url)
    .withParameter("cep", cep)
    .execute();
```

36:28 clientes são egoístas. Não importa se o sistema realiza o retry, o cliente pode realizar novas requisições se sentir que algo está demorando mais que o normal.
- Retry com backoff: é adicionar um retry com delay.
- Retry exponential backoff: é adicionar um retry com um intervalo maior entre as requisições em cada nova requisição. A primeira pode aguardar 100ms, a segunda 200ms, a terceira 400ms e assim por diante. Com isso conseguimos dar um folêgo para o servidor se recuperar, porém ...
```java
ZupClientConfig config = ZupClientConfig.custom()
    .withRequestTimeout(Duration.of(5, SECONDS))
    .withRetryPolicy(RetryPolicy.custom()
        .retryOn(HttpStatus5xxException.class)
        .withMaxAttempts(3)
        .withExponentialBackoff(Duration.of(100, MILLIS))
        .build());

ZupHttpClient<Frete> client = new ZupHttpClient<>(config);
client.get(url)
    .withParameter("cep", cep)
    .execute();
```

39:57 sincronia entre clients: ocorre quando um sistema reinicia ou volta após um tempo após se recuperar de uma instabilidade momentânea. O sistema acaba recebendo uma enxurada de requisições que podem sobrecarregar o sistema.

**retry backoff and jitter** (randômico): é a soma ou subtração em um valor de fator, por exemplo 0.25. O calculo ficará algo como `delay +- random(0, delay * 0.25)`. Podendo gerar um valor, algo como:
- 100ms -> 121ms
- 200ms -> 203ms
- 400ms -> 393ms
- 800ms -> 788ms

Não só evitamos a sincronização dos cliente que poderiam derrubar o servidor, como também distruímos melhor os requests entre os intervalos de espera.

45:22 Para entender melhor como tudo isso funciona, em relação em cada tipo de falha (particionamento de rede):
- delay de 100ms | o pico e a diminuição acontecem do 8 a 80.
- exponential backoff | o pico diminui gradualmente
- exponential backoff and jitter | diminui o pico em frequência bem menor

Efeito de falhas parciais (falhas curtas e falhas com um pouco): mostra um gráfico comparando o pico em cada tipo de técnica.


Pontos de atenção:
- sempre defina **timeouts**. Olhe o histórico, tempo médio ou o p99.
- não faça **retry** (por default). Pode causar uma tempestade de requisições
- se fizer retry, faça **backoff** (exponencial)
- sempre use **jitter**


