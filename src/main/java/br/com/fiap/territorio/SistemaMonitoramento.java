package br.com.fiap.territorio;

import br.com.fiap.territorio.dominio.Alerta;
import br.com.fiap.territorio.dominio.DadosTerritorio;
import br.com.fiap.territorio.dominio.EstrategiaPrevisao;
import br.com.fiap.territorio.dominio.Limiares;
import br.com.fiap.territorio.dominio.Medicao;
import br.com.fiap.territorio.dominio.PontoMonitoramento;
import br.com.fiap.territorio.dominio.Previsao;
import br.com.fiap.territorio.dominio.Sensor;
import br.com.fiap.territorio.dominio.SensorNivelCorrego;
import br.com.fiap.territorio.dominio.SensorPluviometro;
import br.com.fiap.territorio.dominio.SensorSimulado;
import br.com.fiap.territorio.dominio.SensorUmidadeSolo;
import br.com.fiap.territorio.dominio.StatusComponente;
import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.excecao.FonteIndisponivelException;
import br.com.fiap.territorio.excecao.PersistenciaException;
import br.com.fiap.territorio.persistencia.AlertaDAO;
import br.com.fiap.territorio.persistencia.ConexaoBanco;
import br.com.fiap.territorio.persistencia.ExportadorCsv;
import br.com.fiap.territorio.persistencia.MedicaoDAO;
import br.com.fiap.territorio.servico.ConfiguracaoSistema;
import br.com.fiap.territorio.servico.FabricaPrevisao;
import br.com.fiap.territorio.servico.ResultadoPrevisao;
import br.com.fiap.territorio.servico.ServicoMonitoramento;
import br.com.fiap.territorio.util.Formato;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menu de console do sistema. Aqui só tem entrada e saída de dados: as regras
 * ficam no ServicoMonitoramento e no domínio. É também aqui que as exceções
 * são capturadas e viram mensagem para o usuário.
 */
public class SistemaMonitoramento {

    private static final int MAXIMO_RODADAS = 50;
    private static final int MAXIMO_LINHAS_HISTORICO = 30;
    private static final Path PASTA_EXPORTACAO = Path.of("exportacoes");
    private static final DateTimeFormatter CARIMBO_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Scanner scanner;
    private final ConfiguracaoSistema configuracao;
    private final ServicoMonitoramento servico;
    private final DadosTerritorio territorio;
    private final List<PontoMonitoramento> pontos = new ArrayList<>();
    private final Map<String, SensorSimulado> sensoresSimulados = new LinkedHashMap<>();
    private final ExportadorCsv exportadorCsv = new ExportadorCsv();

    public SistemaMonitoramento(Scanner scanner) {
        this.scanner = scanner;
        this.configuracao = ConfiguracaoSistema.getInstancia();

        ConexaoBanco conexaoBanco = new ConexaoBanco(
                configuracao.getUrlBanco(), configuracao.getUsuarioBanco(), configuracao.getSenhaBanco());
        conexaoBanco.criarTabelas();

        List<EstrategiaPrevisao> estrategias = FabricaPrevisao.criarAtivas(configuracao);
        this.servico = new ServicoMonitoramento(
                new MedicaoDAO(conexaoBanco), new AlertaDAO(conexaoBanco), configuracao, estrategias,
                configuracao.getLeiturasConsultadas());

        this.territorio = new DadosTerritorio(
                "Vila Esperança", "Zona Leste", 12500, 480, configuracao.getCodigoAcesso(), configuracao);
        montarPontos();
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            SistemaMonitoramento sistema = new SistemaMonitoramento(scanner);
            sistema.executar();
        } catch (PersistenciaException e) {
            System.out.println("Não foi possível iniciar o sistema por um problema no banco de dados.");
            System.out.println("Detalhe: " + e.getMessage());
        } catch (DadosInvalidosException e) {
            System.out.println("Não foi possível iniciar o sistema: a configuração está inválida.");
            System.out.println("Detalhe: " + e.getMessage());
        }
    }

    /** Território fictício: dois pontos vulneráveis e quatro sensores. */
    private void montarPontos() {
        PontoMonitoramento ponte = new PontoMonitoramento(1, "Ponte da Rua das Palmeiras",
                "Baixada do Córrego Azul",
                "Parte mais baixa do bairro; a rua alaga quando o córrego transborda.");
        instalar(ponte, new SensorPluviometro("PLU-01"));
        instalar(ponte, new SensorNivelCorrego("NIV-01"));

        PontoMonitoramento encosta = new PontoMonitoramento(2, "Escadaria do Morro Alto",
                "Encosta Norte",
                "Encosta ocupada; com o solo encharcado a água desce toda para a baixada.");
        instalar(encosta, new SensorPluviometro("PLU-02"));
        instalar(encosta, new SensorUmidadeSolo("UMI-01"));

        pontos.add(ponte);
        pontos.add(encosta);
    }

    private void instalar(PontoMonitoramento ponto, SensorSimulado sensor) {
        ponto.adicionarSensor(sensor);
        sensoresSimulados.put(sensor.getCodigo(), sensor);
    }

    // ------------------------------------------------------------------
    // Loop principal
    // ------------------------------------------------------------------

    public void executar() {
        System.out.println();
        System.out.println("Bem-vindo ao Territory Predictive Insights.");
        System.out.println("Monitoramento de chuva e risco de alagamento - " + territorio.getNome() + ".");

        boolean executando = true;
        while (executando) {
            try {
                exibirMenuPrincipal();
                int opcao = lerInteiro("Escolha uma opção: ");
                switch (opcao) {
                    case 1 -> verificarSensores();
                    case 2 -> rodarPrevisoes();
                    case 3 -> verHistorico();
                    case 4 -> gerenciarTerritorio();
                    case 5 -> exibirStatusCompleto();
                    case 0 -> {
                        executando = false;
                        System.out.println("Sistema encerrado. As medições e os alertas continuam salvos no banco.");
                    }
                    default -> System.out.println("Opção inválida. Digite um número de 0 a 5.");
                }
            } catch (DadosInvalidosException e) {
                System.out.println("Dados inválidos: " + e.getMessage());
            } catch (PersistenciaException e) {
                System.out.println("Problema no banco de dados ou em arquivo: " + e.getMessage());
            } catch (NoSuchElementException e) {
                System.out.println("A entrada de dados foi encerrada. Finalizando o sistema.");
                executando = false;
            }
        }
    }

    private void exibirMenuPrincipal() {
        System.out.println();
        System.out.println("================ MENU PRINCIPAL ================");
        System.out.println("1 - Verificar sensores (ler e gravar medições)");
        System.out.println("2 - Rodar previsões e emitir alertas");
        System.out.println("3 - Ver histórico (medições e alertas)");
        System.out.println("4 - Gerenciar dados do território");
        System.out.println("5 - Status completo");
        System.out.println("0 - Encerrar o programa");
        System.out.println("================================================");
    }

    // ------------------------------------------------------------------
    // 1 - Verificar sensores
    // ------------------------------------------------------------------

    private void verificarSensores() {
        int rodadas = lerInteiroNaFaixa("Quantas rodadas de leitura? (1 a " + MAXIMO_RODADAS + "): ",
                1, MAXIMO_RODADAS);
        int gravadas = 0;
        int semLeitura = 0;
        int descartadas = 0;

        for (int rodada = 1; rodada <= rodadas; rodada++) {
            System.out.println();
            System.out.println("--- Rodada " + rodada + " de " + rodadas + " ---");
            for (Sensor sensor : todosOsSensores()) {
                try {
                    Medicao medicao = servico.registrarLeitura(sensor);
                    gravadas++;
                    System.out.println("  [GRAVADA]     " + sensor.getCodigo() + ": "
                            + Formato.numero(medicao.getValor()) + " " + medicao.getUnidade()
                            + " (id " + medicao.getId() + ")");
                } catch (FonteIndisponivelException e) {
                    semLeitura++;
                    System.out.println("  [SEM LEITURA] " + e.getMessage());
                } catch (DadosInvalidosException e) {
                    descartadas++;
                    System.out.println("  [DESCARTADA]  " + e.getMessage());
                }
            }
        }

        System.out.println();
        System.out.println("Situação dos pontos de monitoramento:");
        for (PontoMonitoramento ponto : pontos) {
            StatusComponente status = ponto.verificarStatus();
            System.out.println("  " + ponto.getNome() + ": " + status.getRotulo());
        }
        System.out.println();
        System.out.println("Resumo: " + gravadas + " medição(ões) gravada(s), " + semLeitura
                + " sem leitura e " + descartadas + " descartada(s) por valor inválido.");
    }

    // ------------------------------------------------------------------
    // 2 - Previsões e alertas
    // ------------------------------------------------------------------

    private void rodarPrevisoes() {
        int alertasEmitidos = 0;

        for (PontoMonitoramento ponto : pontos) {
            System.out.println();
            System.out.println(">> " + ponto.getNome() + " (" + ponto.getRegiao() + ")");
            for (Sensor sensor : ponto.getSensores()) {
                System.out.println("  Sensor " + sensor.getCodigo() + " (" + sensor.getTipo() + ")");
                List<ResultadoPrevisao> resultados = servico.rodarPrevisoes(sensor);
                for (ResultadoPrevisao resultado : resultados) {
                    exibirResultado(resultado);
                    if (resultado.getAlerta().isPresent()) {
                        alertasEmitidos++;
                    }
                }
            }
        }

        System.out.println();
        if (alertasEmitidos == 0) {
            System.out.println("Nenhum alerta emitido nesta rodada de previsões.");
        } else {
            System.out.println(alertasEmitidos + " alerta(s) emitido(s) e gravado(s) no banco.");
            System.out.println("Lembrete: o alerta é um apoio. Quem decide o que fazer é a equipe responsável,");
            System.out.println("que conhece o local e pode conferir a situação antes de agir.");
        }
    }

    private void exibirResultado(ResultadoPrevisao resultado) {
        Previsao previsao = resultado.getPrevisao();
        if (!previsao.isConfiavel()) {
            System.out.println("    - " + previsao.getEstrategia() + ": sem previsão.");
            System.out.println("        " + previsao.getExplicacao());
            return;
        }

        System.out.println("    - " + previsao.getEstrategia() + ": " + Formato.numero(previsao.getValorPrevisto())
                + " " + resultado.getUnidade());
        System.out.println("        Como chegou: " + previsao.getExplicacao());
        System.out.println("        O que significa: " + resultado.getSignificado());

        Optional<Alerta> alerta = resultado.getAlerta();
        if (alerta.isPresent()) {
            Alerta emitido = alerta.get();
            System.out.println("        >>> " + emitido.getNivel().getRotulo() + " emitido (id " + emitido.getId()
                    + "). Sugestão: " + emitido.getNivel().getRecomendacao());
        }
    }

    // ------------------------------------------------------------------
    // 3 - Histórico
    // ------------------------------------------------------------------

    private void verHistorico() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println();
            System.out.println("------------- HISTÓRICO -------------");
            System.out.println("1 - Medições por sensor");
            System.out.println("2 - Alertas já emitidos");
            System.out.println("0 - Voltar");
            try {
                int opcao = lerInteiro("Escolha uma opção: ");
                switch (opcao) {
                    case 1 -> listarMedicoesPorSensor();
                    case 2 -> listarAlertas();
                    case 0 -> voltar = true;
                    default -> System.out.println("Opção inválida. Digite 0, 1 ou 2.");
                }
            } catch (DadosInvalidosException e) {
                System.out.println("Dados inválidos: " + e.getMessage());
            }
        }
    }

    private void listarMedicoesPorSensor() {
        Sensor sensor = escolherSensor();
        List<Medicao> historico = servico.listarHistoricoPorSensor(sensor.getCodigo());

        if (historico.isEmpty()) {
            System.out.println("Ainda não há medições gravadas para o sensor " + sensor.getCodigo()
                    + ". Use a opção 1 do menu principal para ler os sensores.");
            return;
        }

        System.out.println();
        System.out.println("Histórico do sensor " + sensor.getCodigo() + " (" + sensor.getTipo() + "): "
                + historico.size() + " medição(ões) no banco.");
        int inicio = Math.max(0, historico.size() - MAXIMO_LINHAS_HISTORICO);
        if (inicio > 0) {
            System.out.println("Mostrando as " + MAXIMO_LINHAS_HISTORICO
                    + " mais recentes (o CSV exporta todas).");
        }
        for (Medicao medicao : historico.subList(inicio, historico.size())) {
            System.out.println("  #" + medicao.getId() + " | " + Formato.dataHora(medicao.getDataHora()) + " | "
                    + Formato.numero(medicao.getValor()) + " " + medicao.getUnidade());
        }

        if (lerSimOuNao("Exportar este histórico para CSV? (s/n): ")) {
            String nomeArquivo = "historico_" + sensor.getCodigo() + "_"
                    + LocalDateTime.now().format(CARIMBO_ARQUIVO) + ".csv";
            Path arquivo = exportadorCsv.exportar(historico, PASTA_EXPORTACAO.resolve(nomeArquivo));
            System.out.println("Arquivo gerado: " + arquivo);
        }
    }

    private void listarAlertas() {
        List<Alerta> alertas = servico.listarAlertas();
        if (alertas.isEmpty()) {
            System.out.println("Nenhum alerta foi emitido até agora.");
            return;
        }

        System.out.println();
        System.out.println(alertas.size() + " alerta(s) emitido(s), do mais recente para o mais antigo:");
        for (Alerta alerta : alertas) {
            System.out.println("  #" + alerta.getId() + " | " + Formato.dataHora(alerta.getDataHora()) + " | "
                    + alerta.getNivel().getRotulo());
            System.out.println("      " + alerta.getJustificativa());
        }
    }

    // ------------------------------------------------------------------
    // 4 - Dados do território
    // ------------------------------------------------------------------

    private void gerenciarTerritorio() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println();
            System.out.println("-------- DADOS DO TERRITÓRIO --------");
            System.out.println("1 - Ver dados e limiares");
            System.out.println("2 - Atualizar população estimada");
            System.out.println("3 - Atualizar domicílios em área de risco");
            System.out.println("4 - Alterar limiares de alerta (exige código de acesso)");
            System.out.println("5 - Desativar/reativar sensor (manutenção)");
            System.out.println("0 - Voltar");
            try {
                int opcao = lerInteiro("Escolha uma opção: ");
                switch (opcao) {
                    case 1 -> exibirDadosTerritorio();
                    case 2 -> atualizarPopulacao();
                    case 3 -> atualizarDomicilios();
                    case 4 -> alterarLimiares();
                    case 5 -> alternarSensor();
                    case 0 -> voltar = true;
                    default -> System.out.println("Opção inválida. Digite um número de 0 a 5.");
                }
            } catch (DadosInvalidosException e) {
                System.out.println("Dados inválidos: " + e.getMessage());
            }
        }
    }

    private void exibirDadosTerritorio() {
        System.out.println();
        System.out.println("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + ")");
        System.out.println("População estimada: " + territorio.getPopulacaoEstimada() + " pessoas");
        System.out.println("Domicílios em área de risco: " + territorio.getDomiciliosEmAreaDeRisco());
        exibirLimiares();
    }

    private void exibirLimiares() {
        System.out.println("Limiares vigentes (atenção | alerta | crítico):");
        for (String tipo : configuracao.getTiposDeSensor()) {
            Limiares limiares = territorio.consultarLimiares(tipo);
            System.out.println("  " + tipo + ": " + Formato.numero(limiares.getAtencao()) + " | "
                    + Formato.numero(limiares.getAlerta()) + " | " + Formato.numero(limiares.getCritico()));
        }
    }

    private void atualizarPopulacao() {
        int populacao = lerInteiro("Nova população estimada: ");
        territorio.setPopulacaoEstimada(populacao);
        System.out.println("População estimada atualizada para " + populacao + ".");
    }

    private void atualizarDomicilios() {
        int domicilios = lerInteiro("Novo número de domicílios em área de risco: ");
        territorio.setDomiciliosEmAreaDeRisco(domicilios);
        System.out.println("Domicílios em área de risco atualizados para " + domicilios + ".");
    }

    private void alterarLimiares() {
        List<String> tipos = new ArrayList<>(configuracao.getTiposDeSensor());
        System.out.println("Tipos de sensor:");
        for (int i = 0; i < tipos.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + tipos.get(i));
        }
        int escolha = lerInteiroNaFaixa("Escolha o tipo: ", 1, tipos.size());
        String tipo = tipos.get(escolha - 1);

        Limiares atuais = territorio.consultarLimiares(tipo);
        System.out.println("Limiares atuais de " + tipo + ": " + Formato.numero(atuais.getAtencao()) + " | "
                + Formato.numero(atuais.getAlerta()) + " | " + Formato.numero(atuais.getCritico()));

        String codigo = lerTexto("Código de acesso: ");
        double atencao = lerDecimal("Novo limiar de atenção: ");
        double alerta = lerDecimal("Novo limiar de alerta: ");
        double critico = lerDecimal("Novo limiar crítico: ");

        territorio.alterarLimiares(codigo, tipo, atencao, alerta, critico);
        System.out.println("Limiares de " + tipo + " alterados. Já valem para as próximas previsões.");
    }

    private void alternarSensor() {
        List<SensorSimulado> sensores = new ArrayList<>(sensoresSimulados.values());
        System.out.println("Sensores:");
        for (int i = 0; i < sensores.size(); i++) {
            SensorSimulado sensor = sensores.get(i);
            System.out.println("  " + (i + 1) + " - " + sensor.getCodigo() + " ("
                    + descreverFuncionamento(sensor) + ")");
        }
        int escolha = lerInteiroNaFaixa("Escolha o sensor: ", 1, sensores.size());
        SensorSimulado sensor = sensores.get(escolha - 1);

        if (sensor.verificarFuncionamento()) {
            sensor.desativar();
            System.out.println("Sensor " + sensor.getCodigo() + " desativado para manutenção.");
        } else {
            sensor.reativar();
            System.out.println("Sensor " + sensor.getCodigo() + " reativado.");
        }
        for (PontoMonitoramento ponto : pontos) {
            System.out.println("  " + ponto.getNome() + ": " + ponto.verificarStatus().getRotulo());
        }
    }

    // ------------------------------------------------------------------
    // 5 - Status completo
    // ------------------------------------------------------------------

    private void exibirStatusCompleto() {
        System.out.println();
        System.out.println("=============== STATUS COMPLETO ===============");
        exibirDadosTerritorio();

        System.out.println();
        System.out.println("Pontos de monitoramento:");
        for (PontoMonitoramento ponto : pontos) {
            ponto.verificarStatus();
            System.out.println("  " + ponto);
            System.out.println("    Risco: " + ponto.getDescricaoRisco());
            for (Sensor sensor : ponto.getSensores()) {
                System.out.println("    " + descreverSensor(sensor));
            }
        }

        System.out.println();
        System.out.println("Estratégias de previsão ativas:");
        for (EstrategiaPrevisao estrategia : servico.getEstrategias()) {
            System.out.println("  " + estrategia.getNome() + ": " + estrategia.descrever()
                    + " Mínimo de " + estrategia.getMinimoLeituras() + " leituras.");
        }

        System.out.println();
        int totalAlertas = servico.contarAlertas();
        System.out.println("Alertas emitidos até agora: " + totalAlertas);
        if (totalAlertas > 0) {
            Alerta ultimo = servico.listarAlertas().get(0);
            System.out.println("  Mais recente: " + ultimo.getNivel().getRotulo() + " em "
                    + Formato.dataHora(ultimo.getDataHora()) + " (sensor " + ultimo.getCodigoSensor() + ")");
        }
        System.out.println("================================================");
    }

    private String descreverSensor(Sensor sensor) {
        int total = servico.contarMedicoes(sensor.getCodigo());
        String texto = sensor.getCodigo() + " [" + sensor.getTipo() + "] " + descreverFuncionamento(sensor)
                + " | " + total + " medição(ões)";
        Optional<Medicao> ultima = servico.buscarUltimaMedicao(sensor.getCodigo());
        if (ultima.isPresent()) {
            Medicao medicao = ultima.get();
            texto += " | última: " + Formato.numero(medicao.getValor()) + " " + medicao.getUnidade() + " em "
                    + Formato.dataHora(medicao.getDataHora());
        }
        return texto;
    }

    private String descreverFuncionamento(Sensor sensor) {
        return sensor.verificarFuncionamento() ? "funcionando" : "fora de operação";
    }

    // ------------------------------------------------------------------
    // Apoio: escolha de sensor e leitura do teclado
    // ------------------------------------------------------------------

    private List<Sensor> todosOsSensores() {
        List<Sensor> sensores = new ArrayList<>();
        for (PontoMonitoramento ponto : pontos) {
            sensores.addAll(ponto.getSensores());
        }
        return sensores;
    }

    private Sensor escolherSensor() {
        List<Sensor> sensores = todosOsSensores();
        System.out.println("Sensores:");
        for (int i = 0; i < sensores.size(); i++) {
            Sensor sensor = sensores.get(i);
            System.out.println("  " + (i + 1) + " - " + sensor.getCodigo() + " (" + sensor.getTipo() + ")");
        }
        int escolha = lerInteiroNaFaixa("Escolha o sensor: ", 1, sensores.size());
        return sensores.get(escolha - 1);
    }

    private String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return scanner.nextLine().trim();
    }

    private int lerInteiro(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" não é um número inteiro. Tente de novo.");
            }
        }
    }

    private int lerInteiroNaFaixa(String mensagem, int minimo, int maximo) {
        while (true) {
            int valor = lerInteiro(mensagem);
            if (valor >= minimo && valor <= maximo) {
                return valor;
            }
            System.out.println("Digite um número entre " + minimo + " e " + maximo + ".");
        }
    }

    private double lerDecimal(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem);
            try {
                return Double.parseDouble(texto.replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" não é um número. Use algo como 35 ou 35,5.");
            }
        }
    }

    private boolean lerSimOuNao(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem).toLowerCase();
            if (texto.equals("s") || texto.equals("sim")) {
                return true;
            }
            if (texto.equals("n") || texto.equals("nao") || texto.equals("não")) {
                return false;
            }
            System.out.println("Responda com s ou n.");
        }
    }
}
