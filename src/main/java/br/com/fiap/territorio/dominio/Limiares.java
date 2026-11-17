package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.util.Optional;

/**
 * Os três limites de um tipo de sensor. Sempre em ordem crescente:
 * atenção < alerta < crítico. É imutável; para mudar, cria-se outro.
 */
public class Limiares {

    private final double atencao;
    private final double alerta;
    private final double critico;

    public Limiares(double atencao, double alerta, double critico) {
        if (Double.isNaN(atencao) || Double.isNaN(alerta) || Double.isNaN(critico)) {
            throw new DadosInvalidosException("Os limiares precisam ser números válidos.");
        }
        if (atencao <= 0) {
            throw new DadosInvalidosException(
                    "O limiar de atenção deve ser maior que zero (recebido: " + atencao + ").");
        }
        if (atencao >= alerta || alerta >= critico) {
            throw new DadosInvalidosException(
                    "Limiares fora de ordem: é preciso atenção < alerta < crítico (recebido: "
                            + atencao + ", " + alerta + ", " + critico + ").");
        }
        this.atencao = atencao;
        this.alerta = alerta;
        this.critico = critico;
    }

    /**
     * Diz em qual nível o valor se encaixa. Se estiver abaixo do limiar de
     * atenção devolve vazio, ou seja, não há alerta a emitir.
     */
    public Optional<NivelAlerta> classificar(double valor) {
        if (valor >= critico) {
            return Optional.of(NivelAlerta.CRITICO);
        }
        if (valor >= alerta) {
            return Optional.of(NivelAlerta.ALERTA);
        }
        if (valor >= atencao) {
            return Optional.of(NivelAlerta.ATENCAO);
        }
        return Optional.empty();
    }

    /** Valor do limite correspondente a um nível. */
    public double getLimite(NivelAlerta nivel) {
        if (nivel == null) {
            throw new DadosInvalidosException("Informe o nível de alerta para consultar o limite.");
        }
        return switch (nivel) {
            case ATENCAO -> atencao;
            case ALERTA -> alerta;
            case CRITICO -> critico;
        };
    }

    public double getAtencao() {
        return atencao;
    }

    public double getAlerta() {
        return alerta;
    }

    public double getCritico() {
        return critico;
    }
}
