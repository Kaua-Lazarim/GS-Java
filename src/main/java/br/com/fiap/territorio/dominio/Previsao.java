package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.util.Formato;

/**
 * Resultado de uma estratégia de previsão. Quando não há leituras suficientes
 * a previsão vem marcada como não confiável e sem valor: o sistema prefere
 * ficar calado a chutar.
 */
public class Previsao {

    private final String estrategia;
    private final double valorPrevisto;
    private final boolean confiavel;
    private final String explicacao;
    private final int leiturasDisponiveis;

    private Previsao(String estrategia, double valorPrevisto, boolean confiavel, String explicacao,
                     int leiturasDisponiveis) {
        this.estrategia = estrategia;
        this.valorPrevisto = valorPrevisto;
        this.confiavel = confiavel;
        this.explicacao = explicacao;
        this.leiturasDisponiveis = leiturasDisponiveis;
    }

    public static Previsao confiavel(String estrategia, double valorPrevisto, String explicacao,
                                     int leiturasDisponiveis) {
        return new Previsao(estrategia, valorPrevisto, true, explicacao, leiturasDisponiveis);
    }

    public static Previsao semDadosSuficientes(String estrategia, int leiturasDisponiveis, int minimoLeituras) {
        String explicacao = "Há apenas " + leiturasDisponiveis + " leitura(s) no histórico e a estratégia \""
                + estrategia + "\" precisa de pelo menos " + minimoLeituras + ". Sem previsão por enquanto.";
        return new Previsao(estrategia, Double.NaN, false, explicacao, leiturasDisponiveis);
    }

    public String getEstrategia() {
        return estrategia;
    }

    /** Valor previsto. Só faz sentido quando isConfiavel() é true (senão é NaN). */
    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public boolean isConfiavel() {
        return confiavel;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public int getLeiturasDisponiveis() {
        return leiturasDisponiveis;
    }

    @Override
    public String toString() {
        if (!confiavel) {
            return estrategia + ": sem previsão";
        }
        return estrategia + ": " + Formato.numero(valorPrevisto);
    }
}
