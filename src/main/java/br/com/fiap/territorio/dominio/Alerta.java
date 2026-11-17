package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.time.LocalDateTime;

/**
 * Alerta emitido a partir de uma previsão. Guarda tudo que explica o porquê:
 * qual sensor, qual valor foi previsto, por qual estratégia e qual limite estourou.
 */
public class Alerta {

    private final Long id;
    private final String codigoSensor;
    private final String tipoSensor;
    private final NivelAlerta nivel;
    private final double valorPrevisto;
    private final double limiteEstourado;
    private final String estrategia;
    private final String justificativa;
    private final LocalDateTime dataHora;

    /** Alerta novo, ainda sem id do banco. */
    public Alerta(String codigoSensor, String tipoSensor, NivelAlerta nivel, double valorPrevisto,
                  double limiteEstourado, String estrategia, String justificativa, LocalDateTime dataHora) {
        this(null, codigoSensor, tipoSensor, nivel, valorPrevisto, limiteEstourado, estrategia, justificativa,
                dataHora);
    }

    /** Alerta já gravado, reconstruído a partir do banco. */
    public Alerta(Long id, String codigoSensor, String tipoSensor, NivelAlerta nivel, double valorPrevisto,
                  double limiteEstourado, String estrategia, String justificativa, LocalDateTime dataHora) {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("O alerta precisa informar o sensor que o originou.");
        }
        if (tipoSensor == null || tipoSensor.isBlank()) {
            throw new DadosInvalidosException("O alerta do sensor " + codigoSensor + " está sem o tipo do sensor.");
        }
        if (nivel == null) {
            throw new DadosInvalidosException("O alerta do sensor " + codigoSensor + " está sem nível.");
        }
        if (Double.isNaN(valorPrevisto) || valorPrevisto < limiteEstourado) {
            throw new DadosInvalidosException(
                    "Alerta incoerente para o sensor " + codigoSensor + ": o valor previsto (" + valorPrevisto
                            + ") não atinge o limite informado (" + limiteEstourado + ").");
        }
        if (estrategia == null || estrategia.isBlank()) {
            throw new DadosInvalidosException("O alerta precisa informar a estratégia de previsão usada.");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw new DadosInvalidosException("Todo alerta precisa de uma justificativa.");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("O alerta do sensor " + codigoSensor + " está sem data e hora.");
        }
        this.id = id;
        this.codigoSensor = codigoSensor.trim();
        this.tipoSensor = tipoSensor.trim();
        this.nivel = nivel;
        this.valorPrevisto = valorPrevisto;
        this.limiteEstourado = limiteEstourado;
        this.estrategia = estrategia.trim();
        this.justificativa = justificativa.trim();
        this.dataHora = dataHora;
    }

    /** Devolve uma cópia deste alerta com o id gerado pelo banco. */
    public Alerta comId(long idGerado) {
        return new Alerta(idGerado, codigoSensor, tipoSensor, nivel, valorPrevisto, limiteEstourado, estrategia,
                justificativa, dataHora);
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

    public NivelAlerta getNivel() {
        return nivel;
    }

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public double getLimiteEstourado() {
        return limiteEstourado;
    }

    public String getEstrategia() {
        return estrategia;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
