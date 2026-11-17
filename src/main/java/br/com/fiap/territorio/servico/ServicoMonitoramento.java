package br.com.fiap.territorio.servico;

import br.com.fiap.territorio.dominio.Alerta;
import br.com.fiap.territorio.dominio.CatalogoLimiares;
import br.com.fiap.territorio.dominio.EstrategiaPrevisao;
import br.com.fiap.territorio.dominio.Limiares;
import br.com.fiap.territorio.dominio.Medicao;
import br.com.fiap.territorio.dominio.NivelAlerta;
import br.com.fiap.territorio.dominio.Previsao;
import br.com.fiap.territorio.dominio.Sensor;
import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.excecao.FonteIndisponivelException;
import br.com.fiap.territorio.persistencia.AlertaDAO;
import br.com.fiap.territorio.persistencia.MedicaoDAO;
import br.com.fiap.territorio.util.Formato;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Regras de negócio do monitoramento: ler e validar medições, rodar as
 * previsões sobre o histórico do banco e emitir alertas.
 *
 * Tudo que o serviço precisa chega pelo construtor (DAOs, limiares e
 * estratégias). Por isso dá para testar com mocks, sem banco de verdade.
 */
public class ServicoMonitoramento {

    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final CatalogoLimiares catalogoLimiares;
    private final List<EstrategiaPrevisao> estrategias;
    private final int leiturasConsultadas;

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, CatalogoLimiares catalogoLimiares,
                                List<EstrategiaPrevisao> estrategias, int leiturasConsultadas) {
        if (medicaoDAO == null || alertaDAO == null || catalogoLimiares == null) {
            throw new DadosInvalidosException("O serviço de monitoramento precisa dos DAOs e dos limiares.");
        }
        if (estrategias == null || estrategias.isEmpty()) {
            throw new DadosInvalidosException("O serviço precisa de pelo menos uma estratégia de previsão.");
        }
        if (leiturasConsultadas < 1) {
            throw new DadosInvalidosException(
                    "A quantidade de leituras consultadas deve ser maior que zero (recebido: "
                            + leiturasConsultadas + ").");
        }
        this.medicaoDAO = medicaoDAO;
        this.alertaDAO = alertaDAO;
        this.catalogoLimiares = catalogoLimiares;
        this.estrategias = List.copyOf(estrategias);
        this.leiturasConsultadas = leiturasConsultadas;
    }

    /**
     * Lê o sensor, valida o valor e grava a medição no banco.
     *
     * @throws FonteIndisponivelException se o sensor estiver fora de operação ou não responder
     * @throws DadosInvalidosException    se o valor lido estiver fora da faixa do sensor (nada é gravado)
     */
    public Medicao registrarLeitura(Sensor sensor) throws FonteIndisponivelException {
        if (sensor == null) {
            throw new DadosInvalidosException("Informe o sensor a ser lido.");
        }
        if (!sensor.verificarFuncionamento()) {
            throw new FonteIndisponivelException(
                    "O sensor " + sensor.getCodigo() + " (" + sensor.getTipo()
                            + ") está fora de operação e não pode ser lido.");
        }

        double valor = sensor.lerValor();
        validarFaixa(sensor, valor);

        Medicao medicao = new Medicao(sensor.getCodigo(), sensor.getTipo(), valor, sensor.getUnidade(), agora());
        return medicaoDAO.salvar(medicao);
    }

    private void validarFaixa(Sensor sensor, double valor) {
        if (Double.isNaN(valor) || valor < sensor.getValorMinimo() || valor > sensor.getValorMaximo()) {
            throw new DadosInvalidosException(
                    "Leitura de " + Formato.numero(valor) + " " + sensor.getUnidade() + " no sensor " + sensor.getCodigo()
                            + " está fora da faixa aceita (" + Formato.numero(sensor.getValorMinimo()) + " a "
                            + Formato.numero(sensor.getValorMaximo()) + " " + sensor.getUnidade()
                            + "). Medição descartada.");
        }
    }

    /**
     * Roda todas as estratégias sobre o histórico do sensor que está no banco.
     * Se a previsão for confiável e atingir algum limiar, emite e grava um alerta.
     */
    public List<ResultadoPrevisao> rodarPrevisoes(Sensor sensor) {
        if (sensor == null) {
            throw new DadosInvalidosException("Informe o sensor para rodar as previsões.");
        }
        Limiares limiares = catalogoLimiares.buscarLimiares(sensor.getTipo());
        List<Medicao> historico = medicaoDAO.buscarUltimasPorSensor(sensor.getCodigo(), leiturasConsultadas);

        List<ResultadoPrevisao> resultados = new ArrayList<>();
        for (EstrategiaPrevisao estrategia : estrategias) {
            // polimorfismo: o serviço não sabe (nem precisa saber) qual é a estratégia
            Previsao previsao = estrategia.prever(historico);
            resultados.add(avaliar(sensor, previsao, limiares));
        }
        return resultados;
    }

    private ResultadoPrevisao avaliar(Sensor sensor, Previsao previsao, Limiares limiares) {
        String unidade = sensor.getUnidade();

        if (!previsao.isConfiavel()) {
            String significado = "Sem previsão confiável, então nenhum alerta é emitido para este sensor.";
            return new ResultadoPrevisao(sensor.getCodigo(), unidade, previsao, significado, null);
        }

        double valorPrevisto = previsao.getValorPrevisto();
        Optional<NivelAlerta> nivelAtingido = limiares.classificar(valorPrevisto);

        if (nivelAtingido.isEmpty()) {
            String significado = "Abaixo do limiar de atenção (" + Formato.numero(limiares.getAtencao()) + " "
                    + unidade + "). Situação normal, nenhum alerta emitido.";
            return new ResultadoPrevisao(sensor.getCodigo(), unidade, previsao, significado, null);
        }

        NivelAlerta nivel = nivelAtingido.get();
        double limite = limiares.getLimite(nivel);
        String justificativa = "Sensor " + sensor.getCodigo() + " (" + sensor.getTipo() + "): valor previsto de "
                + Formato.numero(valorPrevisto) + " " + unidade + " pela estratégia \"" + previsao.getEstrategia()
                + "\" atingiu o limite de " + nivel.getRotulo() + " (" + Formato.numero(limite) + " " + unidade
                + "). " + previsao.getExplicacao();

        Alerta alerta = new Alerta(sensor.getCodigo(), sensor.getTipo(), nivel, valorPrevisto, limite,
                previsao.getEstrategia(), justificativa, agora());
        Alerta gravado = alertaDAO.salvar(alerta);

        String significado = "Atingiu o limite de " + nivel.getRotulo() + " (" + Formato.numero(limite) + " "
                + unidade + ").";
        return new ResultadoPrevisao(sensor.getCodigo(), unidade, previsao, significado, gravado);
    }

    public List<Medicao> listarHistoricoPorSensor(String codigoSensor) {
        validarCodigoSensor(codigoSensor);
        return medicaoDAO.listarPorSensor(codigoSensor.trim().toUpperCase());
    }

    public Optional<Medicao> buscarUltimaMedicao(String codigoSensor) {
        validarCodigoSensor(codigoSensor);
        List<Medicao> ultimas = medicaoDAO.buscarUltimasPorSensor(codigoSensor.trim().toUpperCase(), 1);
        return ultimas.isEmpty() ? Optional.empty() : Optional.of(ultimas.get(0));
    }

    public int contarMedicoes(String codigoSensor) {
        validarCodigoSensor(codigoSensor);
        return medicaoDAO.contarPorSensor(codigoSensor.trim().toUpperCase());
    }

    public List<Alerta> listarAlertas() {
        return alertaDAO.listarTodos();
    }

    public int contarAlertas() {
        return alertaDAO.contar();
    }

    public List<EstrategiaPrevisao> getEstrategias() {
        return Collections.unmodifiableList(estrategias);
    }

    private void validarCodigoSensor(String codigoSensor) {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("Informe o código do sensor.");
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now().withNano(0);
    }
}
