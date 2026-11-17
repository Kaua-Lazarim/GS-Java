package br.com.fiap.territorio.excecao;

/**
 * Embrulha os erros de banco (SQLException) e de gravação de arquivo
 * (IOException) para que o resto do sistema não dependa de JDBC.
 * Quando existe uma causa original, ela é sempre guardada.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
