package br.com.fiap.territorio.servico;

import br.com.fiap.territorio.dominio.Alerta;
import br.com.fiap.territorio.dominio.Previsao;

import java.util.Optional;

/**
 * O que o serviço devolve para o menu depois de rodar uma estratégia em um
 * sensor: a previsão, o que ela significa diante dos limiares e o alerta
 * (se algum foi emitido).
 */
public class ResultadoPrevisao {

    private final String codigoSensor;
    private final String unidade;
    private final Previsao previsao;
    private final String significado;
    private final Alerta alerta;

    public ResultadoPrevisao(String codigoSensor, String unidade, Previsao previsao, String significado,
                             Alerta alerta) {
        this.codigoSensor = codigoSensor;
        this.unidade = unidade;
        this.previsao = previsao;
        this.significado = significado;
        this.alerta = alerta;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public String getUnidade() {
        return unidade;
    }

    public Previsao getPrevisao() {
        return previsao;
    }

    public String getSignificado() {
        return significado;
    }

    /** Vazio quando a previsão não estourou nenhum limiar (ou não era confiável). */
    public Optional<Alerta> getAlerta() {
        return Optional.ofNullable(alerta);
    }
}
