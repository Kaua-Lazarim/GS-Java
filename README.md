# Territory Predictive Insights - Chuva e risco de alagamento

Global Solution 2º/2026 - OOP (Turma de Fevereiro, 2CCR) - Tema CitizenTech

Recorte escolhido: **Clima local - chuva intensa e risco de alagamento em pontos vulneráveis.**

## Integrantes

| Nome | RM |
|------|----|
| Kauã da Silva Lazarim | 564625 |
| Guilherme Williams | 565319 |

Repositório: https://github.com/Kaua-Lazarim/GS-Java

## Quem sofre o problema e qual decisão o sistema apoia

O sistema foi pensado para a associação de moradores de um bairro fictício, a Vila Esperança, que trabalha junto com o núcleo de Defesa Civil da região. O bairro tem dois pontos que dão problema toda vez que chove forte:

- a **Ponte da Rua das Palmeiras**, na baixada do córrego, que é a primeira rua a alagar;
- a **Escadaria do Morro Alto**, uma encosta ocupada de onde a água desce para a baixada quando o solo encharca.

Quem sofre são as famílias que moram perto desses pontos (o sistema trabalha com o total estimado de domicílios em área de risco, sem nenhum dado de pessoa). A pergunta que a equipe da associação precisa responder é: **"pelo ritmo das últimas leituras, a situação vai piorar nas próximas horas? Já é hora de avisar os moradores ou acionar a Defesa Civil?"**

O sistema lê os sensores, grava tudo no banco, faz duas previsões simples em cima do histórico e emite um alerta (ATENÇÃO, ALERTA ou CRÍTICO) explicando o motivo. A decisão do que fazer continua sendo da equipe.

## Como rodar

Precisa de **JDK 17 ou mais novo** e **Maven 3.8+**. Não precisa instalar banco: o H2 vem como dependência e grava em arquivo.

```bash
# na pasta do projeto (onde está o pom.xml)
mvn clean test            # compila e roda os testes
mvn compile exec:java     # abre o menu no console
```

Pelo IntelliJ: abrir a pasta do projeto (ele reconhece o `pom.xml`) e rodar o `main` da classe `br.com.fiap.territorio.SistemaMonitoramento`.

Na primeira execução o sistema cria a pasta `dados/` com o arquivo do banco (`territorio.mv.db`). Para começar do zero é só apagar essa pasta. O script das tabelas está em `src/main/resources/schema.sql` e roda sozinho na inicialização.

### Roteiro rápido para testar

1. **Opção 1**, 8 rodadas: lê os 4 sensores 8 vezes e grava as medições. Aparecem linhas `[GRAVADA]`, e de vez em quando `[SEM LEITURA]` (sensor não respondeu) e `[DESCARTADA]` (valor inválido), porque a simulação inclui falhas de propósito.
2. **Opção 2**: roda as duas previsões por sensor e emite os alertas.
3. **Opção 3**: histórico de medições por sensor (com opção de exportar CSV) e alertas emitidos.
4. **Opção 0** e depois abrir de novo: na **opção 5** as medições e os alertas continuam lá.
5. **Opção 4**: tentar população negativa, tentar alterar limiar com código errado e depois com o código certo (`VILA2026`), e desativar um sensor para ver o ponto ficar "Degradado" e a leitura falhar.

## Menu

| Opção | O que faz |
|-------|-----------|
| 1 - Verificar sensores | Lê cada sensor, valida e grava a medição. Mostra o status dos pontos. |
| 2 - Rodar previsões | Roda as estratégias sobre o histórico do banco, compara com os limiares e emite alertas. |
| 3 - Ver histórico | Medições por sensor (com exportação CSV) e alertas já emitidos. |
| 4 - Gerenciar dados do território | Ver dados, alterar população e domicílios, alterar limiares (com código de acesso), desativar/reativar sensor. |
| 5 - Status completo | Território, pontos, sensores, limiares, estratégias e total de alertas. |
| 0 - Encerrar | Sai do programa. Os dados ficam no banco. |

## Estrutura

```
src/main/java/br/com/fiap/territorio/
├── SistemaMonitoramento.java      menu de console (main)
├── dominio/
│   ├── ComponenteTerritorial      classe abstrata (id, nome, região, status)
│   ├── PontoMonitoramento         filha concreta, tem os sensores
│   ├── Sensor                     interface
│   ├── SensorSimulado             base abstrata dos sensores simulados
│   ├── SensorPluviometro, SensorNivelCorrego, SensorUmidadeSolo
│   ├── DadosTerritorio            dados do território + código de acesso
│   ├── CatalogoLimiares, Limiares, NivelAlerta, StatusComponente
│   ├── EstrategiaPrevisao         classe abstrata
│   ├── PrevisaoMediaMovel, PrevisaoComparacao, Previsao
│   └── Medicao, Alerta
├── excecao/                       DadosInvalidosException, FonteIndisponivelException, PersistenciaException
├── persistencia/                  ConexaoBanco, MedicaoDAO, AlertaDAO, ExportadorCsv
├── servico/                       ServicoMonitoramento, FabricaPrevisao, ConfiguracaoSistema, ResultadoPrevisao
└── util/                          Formato
src/main/resources/                configuracao.properties, schema.sql
src/test/java/...                  testes JUnit 5 + Mockito
```

## Onde está cada requisito

| Requisito | Onde |
|-----------|------|
| Classe abstrata com método abstrato + filha | `ComponenteTerritorial.verificarStatus()` e `PontoMonitoramento` |
| Interface `Sensor` com 3 implementações | `SensorPluviometro`, `SensorNivelCorrego`, `SensorUmidadeSolo` |
| `DadosTerritorio` com atributos privados e código de acesso | `DadosTerritorio.alterarLimiares(...)` |
| `EstrategiaPrevisao` com 2 filhas, `prever()` sobrescrito e `super` | `PrevisaoMediaMovel` e `PrevisaoComparacao` (chamam `super(...)` no construtor e `super.prever(...)`) |
| Escolha da estratégia sem `if tipo ==` / `instanceof` | `FabricaPrevisao` (mapa de criadores) + laço polimórfico em `ServicoMonitoramento.rodarPrevisoes` |
| Exceções customizadas | pacote `excecao`; lançadas em construtores, setters e no serviço; capturadas no menu |
| Persistência JDBC | `MedicaoDAO` e `AlertaDAO` com `PreparedStatement` e try-with-resources |
| Padrão criacional | `FabricaPrevisao` (Factory) e `ConfiguracaoSistema` (Singleton) |
| Testes | 8 classes de teste, padrão AAA, `assertThrows` e Mockito |
| Bônus | exportação do histórico em CSV (opção 3 > 1) |

## Decisões da equipe

**JDBC em vez de Spring Boot.** O sistema é um menu de console com duas tabelas. Com JDBC puro dá para ver exatamente o que acontece em cada operação (abrir conexão, preparar o comando, passar os parâmetros, fechar), e o projeto sobe em menos de um segundo. Achamos que o Spring esconderia justamente a parte que queríamos mostrar.

**H2 em arquivo, não em memória.** O enunciado pede que os dados sobrevivam ao reinício. O H2 em memória apagaria tudo ao fechar, então usamos o modo arquivo (`jdbc:h2:file:./dados/territorio`). Continua sem precisar instalar nada.

**SQL fixo.** Todo SQL é uma constante de texto e os valores entram só por `?`. Não existe concatenação de valor dentro de SQL em nenhum lugar.

**Sensor simulado com "passeio aleatório".** Em vez de sortear um valor solto a cada leitura, o valor novo parte do anterior e varia um pouco. Fica mais parecido com chuva de verdade (que sobe e desce aos poucos) e faz as previsões terem sentido. Os sensores também falham de propósito: 8% das leituras não respondem (`FonteIndisponivelException`) e 4% devolvem um valor absurdo, que o serviço descarta com `DadosInvalidosException`. Assim as exceções aparecem no uso normal, não só nos testes.

**Exceção checked x unchecked.** `FonteIndisponivelException` é checked porque sensor falhar é rotina, e o compilador obriga quem lê o sensor a tratar. `DadosInvalidosException` é unchecked porque é lançada em construtores e setters do domínio inteiro; se fosse checked todo método teria `throws`. Criamos também `PersistenciaException` para embrulhar `SQLException` e `IOException`, assim o menu não precisa conhecer JDBC. Não há `catch` vazio: todo erro capturado vira mensagem na tela ou é relançado com a causa.

**Medição e alerta imutáveis.** Histórico não se edita. Depois de gravados, `Medicao` e `Alerta` só têm getters; o id do banco entra por uma cópia (`comId`).

**Interface `CatalogoLimiares`.** `DadosTerritorio` é quem confere o código de acesso, mas os limiares ficam na `ConfiguracaoSistema`. Para o domínio não depender do pacote de serviço, o território conversa com essa interface pequena. De quebra, o teste do território usa um mock dela.

### Padrões criacionais e por que ali

**Factory - `FabricaPrevisao`.** Criar uma estratégia exige saber a classe concreta e os parâmetros dela (janela, período, mínimo de leituras). Se isso ficasse no menu ou no serviço, teríamos um `if` por tipo em cada lugar e cada estratégia nova obrigaria a mexer neles. A fábrica guarda um mapa "chave -> como criar" e lê as chaves ativas do `configuracao.properties`. Para adicionar uma terceira estratégia basta criar a classe, registrar uma linha na fábrica e incluir a chave no arquivo. O serviço só recebe uma lista de `EstrategiaPrevisao` e chama `prever()`.

**Singleton - `ConfiguracaoSistema`.** Os limiares precisam ser os mesmos para todo mundo. O menu altera, o território valida e o serviço consulta: se cada um tivesse a sua cópia, um limiar alterado no menu não mudaria os alertas. Por isso existe uma instância só, com construtor privado e `getInstancia()`. Usamos o Singleton apenas para configuração; os DAOs e o serviço são objetos comuns passados por construtor, justamente para poderem ser trocados por mocks nos testes.

**Por que não o Builder.** O "relatório" do sistema é o status completo, que é só impressão de dados que já existem. Não há um objeto com muitos campos opcionais para montar passo a passo, então um Builder seria padrão usado só para constar.

## Testes

```bash
mvn test
```

| Classe | O que verifica |
|--------|----------------|
| `MedicaoTest` | criação válida, medição negativa (`assertThrows`), sensor em branco |
| `LimiaresTest` | classificação em cada nível, limiares fora de ordem |
| `PrevisaoMediaMovelTest` | cálculo da média só da janela, silêncio com poucas leituras, histórico nulo |
| `PrevisaoComparacaoTest` | projeção de alta, estabilidade, previsão nunca negativa |
| `PontoMonitoramentoTest` | status operacional/degradado/inoperante, sensor desativado lança `FonteIndisponivelException` |
| `DadosTerritorioTest` | população negativa, código de acesso errado não altera nada (Mockito) |
| `FabricaPrevisaoTest` | fábrica cria a classe certa, chave desconhecida, singleton |
| `ServicoMonitoramentoTest` | leitura e alertas com `MedicaoDAO` e `AlertaDAO` mockados (Mockito): alerta crítico, atenção, nenhum alerta, histórico insuficiente, leitura inválida não é gravada |

Nenhum teste usa banco nem teclado. Os valores esperados foram calculados à mão (estão nos comentários), então se a conta da previsão mudar o teste quebra.

## Reflexão crítica

**Quando o sistema deve se calar?** Com menos de 5 leituras de um sensor o sistema não faz previsão para ele, e a comparação entre períodos exige 6 (dois períodos de 3). Nesses casos a tela mostra "sem previsão" e explica quantas leituras faltam, em vez de mostrar um número. A média de 2 ou 3 valores muda completamente com uma leitura só, e um número na tela passa uma confiança que ele não tem. O mesmo vale para leitura inválida (é descartada, não entra no histórico) e para sensor fora do ar (o ponto aparece como "Degradado"). Preferimos que o sistema diga "não sei" do que acerte por sorte. Um ponto que ainda falha: se a opção de previsão for rodada várias vezes sem leitura nova, o mesmo alerta é emitido de novo. Num uso real isso gera cansaço de alerta, e a pessoa passa a ignorar.

**O que os dados não representam?** Um pluviômetro mede a chuva naquele ponto. A três quarteirões pode estar chovendo o dobro. O nível do córrego na ponte não mostra um bueiro entupido que alaga outra rua antes de o córrego subir. Os dados também não sabem quem mora ali: um idoso acamado ou uma creche na rua mudam totalmente a urgência, e isso não aparece em nenhuma leitura. Os limiares (20, 35 e 50 mm/h, por exemplo) são valores de referência que definimos para a simulação; num bairro real eles teriam que ser ajustados com a Defesa Civil e com o histórico de alagamentos do lugar. E as duas previsões só olham o passado recente do próprio sensor: não sabem que vem uma frente fria, e uma tempestade que começa de repente só aparece depois de algumas leituras.

**Por que o alerta apoia a decisão em vez de decidir?** Porque o sistema enxerga quatro sensores e quem está no bairro enxerga o resto. O alerta pode estar errado para os dois lados: um sensor com defeito gera alarme falso, e uma chuva muito localizada pode não gerar alarme nenhum. Retirar famílias de casa tem custo e risco, e não retirar também. Quem responde por isso é uma pessoa, não um programa. Por isso cada alerta mostra o sensor, o valor previsto, o limite que foi atingido e a conta que levou até ali, para a equipe conseguir conferir e discordar. A recomendação aparece como "sugestão", e a tela lembra que a decisão é da equipe. Se o sistema decidisse sozinho, ninguém conferiria e ninguém saberia explicar depois por que a decisão foi tomada.

## Limitações conhecidas

- Limiares alterados pelo menu valem até fechar o programa. Ao reiniciar, voltam para os valores do `configuracao.properties` (para mudar de vez, é só editar o arquivo).
- Os dados dos sensores são simulados. A data e hora de cada medição é o momento em que a leitura simulada foi feita.
- Rodar a previsão duas vezes seguidas sem novas leituras repete os alertas (comentado na reflexão).

## Divisão de tarefas

Como somos uma dupla, juntamos as três frentes sugeridas no enunciado em duas.

| Integrante | Responsável por |
|------------|-----------------|
| **Guilherme** | Domínio (`ComponenteTerritorial`, `PontoMonitoramento`, `Sensor` e os três sensores, `DadosTerritorio`, `Limiares`, `Medicao`, `Alerta`), exceções customizadas, estratégias de previsão (`EstrategiaPrevisao`, `PrevisaoMediaMovel`, `PrevisaoComparacao`), `FabricaPrevisao` (Factory), opções 2 e 4 do menu, testes de domínio e da fábrica. |
| **Kauã** | Persistência (`ConexaoBanco`, `MedicaoDAO`, `AlertaDAO`, `schema.sql`), `ConfiguracaoSistema` (Singleton), `ServicoMonitoramento` (leitura, validação e alertas), exportação CSV, opções 1, 3 e 5 do menu, `ServicoMonitoramentoTest` com Mockito, configuração do Maven. |
| **Os dois** | Escolha do recorte, definição dos limiares, revisão do código um do outro, README e reflexão crítica. |


## Divisão de tarefas

Foi utilizado IA para formatação e escrita das informações do readme e demais pontos de formatação.
