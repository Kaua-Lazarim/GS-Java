package br.com.fiap.territorio.dominio;

public enum StatusComponente {

    OPERACIONAL("Operacional"),
    DEGRADADO("Degradado"),
    INOPERANTE("Inoperante");

    private final String rotulo;

    StatusComponente(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
