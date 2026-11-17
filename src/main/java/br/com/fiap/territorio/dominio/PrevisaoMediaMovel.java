package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.util.Formato;

import java.util.List;

/**
 * Prevê a próxima leitura como a média das últimas N (a "janela").
 * Suaviza picos isolados: uma leitura alta sozinha não dispara alerta.
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    private final int janela;

    public PrevisaoMediaMovel(int janela, int minimoLeituras) {
        super("Média móvel", Math.max(janela, minimoLeituras));
        if (janela < 2) {
            throw new DadosInvalidosException(
                    "A janela da média móvel deve ter pelo menos 2 leituras (recebido: " + janela + ").");
        }
        this.janela = janela;
    }

    @Override
    public Previsao prever(List<Medicao> historico) {
        Previsao base = super.prever(historico);
        if (!base.isConfiavel()) {
            return base;
        }

        List<Medicao> ultimas = historico.subList(historico.size() - janela, historico.size());
        double media = calcularMedia(ultimas);
        double ultimaLeitura = base.getValorPrevisto();

        String posicao;
        if (ultimaLeitura > media) {
            posicao = "acima da média, sinal de subida recente";
        } else if (ultimaLeitura < media) {
            posicao = "abaixo da média, sinal de queda recente";
        } else {
            posicao = "igual à média, cenário estável";
        }

        String explicacao = "Média das últimas " + janela + " leituras = " + Formato.numero(media)
                + ". A última leitura (" + Formato.numero(ultimaLeitura) + ") está " + posicao + ".";
        return Previsao.confiavel(getNome(), media, explicacao, historico.size());
    }

    @Override
    public String descrever() {
        return "Média das últimas " + janela + " leituras do sensor.";
    }

    public int getJanela() {
        return janela;
    }
}
