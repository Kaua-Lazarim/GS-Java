package br.com.fiap.territorio.dominio;

import java.util.Random;

/** Mede a altura da lâmina d'água do córrego em centímetros. */
public class SensorNivelCorrego extends SensorSimulado {

    public static final String TIPO = "NIVEL_CORREGO";

    public SensorNivelCorrego(String codigo) {
        this(codigo, new Random());
    }

    public SensorNivelCorrego(String codigo, Random random) {
        super(codigo, 120.0, 25.0, random);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    @Override
    public String getUnidade() {
        return "cm";
    }

    @Override
    public double getValorMinimo() {
        return 0.0;
    }

    @Override
    public double getValorMaximo() {
        return 400.0;
    }
}
