package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.util.List;

/**
 * Base das estratégias de previsão. O serviço chama prever() sem saber qual
 * filha está por trás (polimorfismo).
 *
 * O prever() daqui faz o que é comum a todas: valida o histórico, decide se
 * há leituras suficientes e devolve a previsão mais simples possível (repetir
 * a última leitura). As filhas chamam super.prever() e refinam o resultado.
 */
public abstract class EstrategiaPrevisao {

    private final String nome;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String nome, int minimoLeituras) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("A estratégia de previsão precisa de um nome.");
        }
        if (minimoLeituras < 1) {
            throw new DadosInvalidosException(
                    "O mínimo de leituras da estratégia \"" + nome + "\" deve ser pelo menos 1 (recebido: "
                            + minimoLeituras + ").");
        }
        this.nome = nome;
        this.minimoLeituras = minimoLeituras;
    }

    /**
     * @param historico medições de um único sensor, da mais antiga para a mais recente
     */
    public Previsao prever(List<Medicao> historico) {
        if (historico == null) {
            throw new DadosInvalidosException(
                    "O histórico de medições não pode ser nulo na estratégia \"" + nome + "\".");
        }
        if (historico.size() < minimoLeituras) {
            return Previsao.semDadosSuficientes(nome, historico.size(), minimoLeituras);
        }
        double ultimaLeitura = historico.get(historico.size() - 1).getValor();
        return Previsao.confiavel(nome, ultimaLeitura, "Repete a última leitura registrada.", historico.size());
    }

    /** Explica em uma frase como a estratégia calcula a previsão. */
    public abstract String descrever();

    /** Média simples dos valores de uma lista de medições. */
    protected double calcularMedia(List<Medicao> medicoes) {
        double soma = 0;
        for (Medicao medicao : medicoes) {
            soma += medicao.getValor();
        }
        return soma / medicoes.size();
    }

    public String getNome() {
        return nome;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }
}
