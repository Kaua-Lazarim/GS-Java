package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

/**
 * Qualquer coisa do território que a gente monitora (hoje, os pontos de
 * monitoramento). Guarda o que é comum e deixa cada filho dizer como
 * calcula o próprio status.
 */
public abstract class ComponenteTerritorial {

    private final int id;
    private String nome;
    private String regiao;
    private StatusComponente status;

    protected ComponenteTerritorial(int id, String nome, String regiao) {
        if (id <= 0) {
            throw new DadosInvalidosException("O id do componente deve ser maior que zero (recebido: " + id + ").");
        }
        this.id = id;
        setNome(nome);
        setRegiao(regiao);
        this.status = StatusComponente.OPERACIONAL;
    }

    /**
     * Recalcula o status do componente e devolve o resultado.
     * Cada tipo de componente tem a sua regra.
     */
    public abstract StatusComponente verificarStatus();

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("O nome do componente territorial é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        if (regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("A região do componente territorial é obrigatória.");
        }
        this.regiao = regiao.trim();
    }

    public StatusComponente getStatus() {
        return status;
    }

    protected void setStatus(StatusComponente status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "#" + id + " " + nome + " (" + regiao + ") - " + status.getRotulo();
    }
}
