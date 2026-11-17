package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.util.Formato;

import java.util.List;

/**
 * Compara o período atual (últimas N leituras) com o período anterior
 * (as N leituras antes dele) e projeta que a variação vai se repetir.
 * Reage mais rápido que a média móvel quando a chuva está aumentando.
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    private final int tamanhoPeriodo;

    public PrevisaoComparacao(int tamanhoPeriodo, int minimoLeituras) {
        // precisa de dois períodos completos para ter o que comparar
        super("Comparação com período anterior", Math.max(tamanhoPeriodo * 2, minimoLeituras));
        if (tamanhoPeriodo < 1) {
            throw new DadosInvalidosException(
                    "O período de comparação deve ter pelo menos 1 leitura (recebido: " + tamanhoPeriodo + ").");
        }
        this.tamanhoPeriodo = tamanhoPeriodo;
    }

    @Override
    public Previsao prever(List<Medicao> historico) {
        Previsao base = super.prever(historico);
        if (!base.isConfiavel()) {
            return base;
        }

        int total = historico.size();
        List<Medicao> periodoAtual = historico.subList(total - tamanhoPeriodo, total);
        List<Medicao> periodoAnterior = historico.subList(total - 2 * tamanhoPeriodo, total - tamanhoPeriodo);

        double mediaAtual = calcularMedia(periodoAtual);
        double mediaAnterior = calcularMedia(periodoAnterior);
        double variacao = mediaAtual - mediaAnterior;

        // a grandeza nunca é negativa, então a projeção para em zero
        double previsto = Math.max(0, mediaAtual + variacao);

        String explicacao = "Média do período atual (últimas " + tamanhoPeriodo + " leituras) = "
                + Formato.numero(mediaAtual) + "; período anterior = " + Formato.numero(mediaAnterior) + ". "
                + descreverTendencia(variacao, mediaAnterior)
                + " Se a tendência continuar, o próximo período fica em torno de " + Formato.numero(previsto) + ".";
        return Previsao.confiavel(getNome(), previsto, explicacao, total);
    }

    private String descreverTendencia(double variacao, double mediaAnterior) {
        if (variacao == 0) {
            return "Sem variação entre os períodos.";
        }
        String direcao = variacao > 0 ? "Alta" : "Queda";
        String texto = direcao + " de " + Formato.numero(Math.abs(variacao));
        if (mediaAnterior > 0) {
            double percentual = Math.abs(variacao) / mediaAnterior * 100;
            texto += " (" + Formato.numero(percentual) + "%)";
        }
        return texto + " em relação ao período anterior.";
    }

    @Override
    public String descrever() {
        return "Compara a média das últimas " + tamanhoPeriodo + " leituras com a das " + tamanhoPeriodo
                + " anteriores e projeta a mesma variação.";
    }

    public int getTamanhoPeriodo() {
        return tamanhoPeriodo;
    }
}
