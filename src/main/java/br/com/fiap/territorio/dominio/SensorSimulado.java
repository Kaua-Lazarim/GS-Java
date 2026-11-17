package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.excecao.FonteIndisponivelException;

import java.util.Random;

/**
 * Base dos sensores simulados. A leitura é um "passeio aleatório": cada valor
 * novo parte do anterior e varia um pouco para cima ou para baixo, o que fica
 * mais parecido com chuva de verdade do que sortear um número solto.
 *
 * Também simula os dois problemas que um sensor real dá:
 * - não responder (FonteIndisponivelException);
 * - responder um valor absurdo (leitura espúria, que o serviço descarta).
 */
public abstract class SensorSimulado implements Sensor {

    /** Chance de o sensor não responder em uma leitura. */
    private static final double CHANCE_FALHA = 0.08;
    /** Chance de o sensor devolver um valor absurdo. */
    private static final double CHANCE_LEITURA_ESPURIA = 0.04;
    /** Valor devolvido quando a leitura sai espúria (fora de qualquer faixa). */
    private static final double VALOR_ESPURIO = -1.0;

    private final String codigo;
    private final double passoMaximo;
    private final Random random;
    private double valorAtual;
    private boolean ativo = true;

    protected SensorSimulado(String codigo, double valorInicial, double passoMaximo, Random random) {
        if (codigo == null || codigo.isBlank()) {
            throw new DadosInvalidosException("O código do sensor é obrigatório.");
        }
        if (passoMaximo <= 0) {
            throw new DadosInvalidosException("O passo de variação do sensor " + codigo + " deve ser positivo.");
        }
        if (random == null) {
            throw new DadosInvalidosException("O gerador de números do sensor " + codigo + " não pode ser nulo.");
        }
        this.codigo = codigo.trim().toUpperCase();
        this.valorAtual = valorInicial;
        this.passoMaximo = passoMaximo;
        this.random = random;
    }

    @Override
    public double lerValor() throws FonteIndisponivelException {
        if (!ativo) {
            throw new FonteIndisponivelException(
                    "O sensor " + codigo + " está desativado para manutenção.");
        }

        double sorteio = random.nextDouble();
        if (sorteio < CHANCE_FALHA) {
            throw new FonteIndisponivelException(
                    "O sensor " + codigo + " não respondeu à leitura (falha de comunicação).");
        }
        if (sorteio < CHANCE_FALHA + CHANCE_LEITURA_ESPURIA) {
            return VALOR_ESPURIO;
        }

        double variacao = (random.nextDouble() * 2 - 1) * passoMaximo;
        valorAtual = limitarNaFaixa(valorAtual + variacao);
        return arredondar(valorAtual);
    }

    @Override
    public boolean verificarFuncionamento() {
        return ativo;
    }

    /** Tira o sensor de operação (manutenção). */
    public void desativar() {
        this.ativo = false;
    }

    /** Devolve o sensor para operação. */
    public void reativar() {
        this.ativo = true;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    private double limitarNaFaixa(double valor) {
        return Math.max(getValorMinimo(), Math.min(getValorMaximo(), valor));
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    @Override
    public String toString() {
        return codigo + " [" + getTipo() + ", " + getUnidade() + "]";
    }
}
