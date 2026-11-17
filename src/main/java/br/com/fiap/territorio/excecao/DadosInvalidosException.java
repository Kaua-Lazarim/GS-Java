package br.com.fiap.territorio.excecao;

/**
 * Lançada quando um dado não passa na validação: medição fora da faixa,
 * população negativa, limiar fora de ordem, código de acesso errado etc.
 *
 * É unchecked (RuntimeException) porque as validações ficam em construtores
 * e setters, e não queremos "throws" em todas as assinaturas do domínio.
 * Quem trata é o menu, que mostra a mensagem para o usuário.
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
