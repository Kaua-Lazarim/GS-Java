package br.com.fiap.territorio.dominio;

import java.util.Random;

/** Mede a intensidade da chuva em milímetros por hora. */
public class SensorPluviometro extends SensorSimulado {

    public static final String TIPO = "PLUVIOMETRO";

    public SensorPluviometro(String codigo) {
        this(codigo, new Random());
    }

    public SensorPluviometro(String codigo, Random random) {
        super(codigo, 15.0, 8.0, random);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    @Override
    public String getUnidade() {
        return "mm/h";
    }

    @Override
    public double getValorMinimo() {
        return 0.0;
    }

    @Override
    public double getValorMaximo() {
        return 120.0;
    }
}
