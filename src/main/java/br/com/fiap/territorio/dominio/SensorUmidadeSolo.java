package br.com.fiap.territorio.dominio;

import java.util.Random;

/**
 * Mede a saturação do solo em porcentagem. Solo encharcado não absorve mais
 * chuva: a água escorre para a baixada e a encosta fica instável.
 */
public class SensorUmidadeSolo extends SensorSimulado {

    public static final String TIPO = "UMIDADE_SOLO";

    public SensorUmidadeSolo(String codigo) {
        this(codigo, new Random());
    }

    public SensorUmidadeSolo(String codigo, Random random) {
        super(codigo, 62.0, 6.0, random);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    @Override
    public String getUnidade() {
        return "%";
    }

    @Override
    public double getValorMinimo() {
        return 0.0;
    }

    @Override
    public double getValorMaximo() {
        return 100.0;
    }
}
