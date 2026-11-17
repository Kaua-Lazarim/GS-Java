package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.FonteIndisponivelException;

/**
 * Contrato de qualquer sensor do território. O serviço de monitoramento só
 * conhece esta interface, então tanto faz se o sensor é simulado ou real.
 */
public interface Sensor {

    /** Faz uma leitura. Lança FonteIndisponivelException se o sensor não responder. */
    double lerValor() throws FonteIndisponivelException;

    /** Indica se o sensor está em condições de ser lido. */
    boolean verificarFuncionamento();

    /** Tipo do sensor (ex.: PLUVIOMETRO). É a chave usada para buscar os limiares. */
    String getTipo();

    /** Código único do equipamento (ex.: PLU-01). */
    String getCodigo();

    /** Unidade de medida da leitura (ex.: mm/h). */
    String getUnidade();

    /** Menor valor fisicamente possível para este sensor. */
    double getValorMinimo();

    /** Maior valor fisicamente possível para este sensor. */
    double getValorMaximo();
}
