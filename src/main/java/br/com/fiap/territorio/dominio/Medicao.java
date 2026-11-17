package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.time.LocalDateTime;

/**
 * Uma leitura de sensor já validada. É imutável: depois de criada não muda,
 * porque histórico não se edita.
 */
public class Medicao {

    private final Long id;
    private final String codigoSensor;
    private final String tipoSensor;
    private final double valor;
    private final String unidade;
    private final LocalDateTime dataHora;

    /** Medição nova, que ainda não foi para o banco (sem id). */
    public Medicao(String codigoSensor, String tipoSensor, double valor, String unidade, LocalDateTime dataHora) {
        this(null, codigoSensor, tipoSensor, valor, unidade, dataHora);
    }

    /** Medição já gravada, reconstruída a partir do banco. */
    public Medicao(Long id, String codigoSensor, String tipoSensor, double valor, String unidade,
                   LocalDateTime dataHora) {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("A medição precisa do código do sensor que a gerou.");
        }
        if (tipoSensor == null || tipoSensor.isBlank()) {
            throw new DadosInvalidosException("A medição do sensor " + codigoSensor + " está sem o tipo do sensor.");
        }
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new DadosInvalidosException(
                    "A medição do sensor " + codigoSensor + " não é um número válido.");
        }
        if (valor < 0) {
            throw new DadosInvalidosException(
                    "Medição negativa (" + valor + ") no sensor " + codigoSensor
                            + ": nenhuma grandeza monitorada aceita valor abaixo de zero.");
        }
        if (unidade == null || unidade.isBlank()) {
            throw new DadosInvalidosException("A medição do sensor " + codigoSensor + " está sem unidade de medida.");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("A medição do sensor " + codigoSensor + " está sem data e hora.");
        }
        this.id = id;
        this.codigoSensor = codigoSensor.trim();
        this.tipoSensor = tipoSensor.trim();
        this.valor = valor;
        this.unidade = unidade.trim();
        this.dataHora = dataHora;
    }

    /** Devolve uma cópia desta medição com o id gerado pelo banco. */
    public Medicao comId(long idGerado) {
        return new Medicao(idGerado, codigoSensor, tipoSensor, valor, unidade, dataHora);
    }

    public Long getId() {
        return id;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public String getTipoSensor() {
        return tipoSensor;
    }

    public double getValor() {
        return valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
