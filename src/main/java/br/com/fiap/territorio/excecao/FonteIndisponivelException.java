package br.com.fiap.territorio.excecao;

/**
 * Lançada quando a fonte de dados falha: sensor desativado, sem resposta
 * ou gerador de leituras fora do ar.
 *
 * É checked (Exception) de propósito: falha de sensor é algo esperado no
 * dia a dia, então o compilador obriga quem lê o sensor a decidir o que fazer.
 */
public class FonteIndisponivelException extends Exception {

    public FonteIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public FonteIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
