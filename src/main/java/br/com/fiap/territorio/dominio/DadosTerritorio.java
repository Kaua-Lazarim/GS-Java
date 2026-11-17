package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

/**
 * Dados gerais do território monitorado. Nada aqui identifica pessoas:
 * só região, totais estimados e os limiares.
 *
 * Os atributos são privados e toda alteração passa por validação. Mexer nos
 * limiares muda quando o sistema alerta, então exige o código de acesso.
 */
public class DadosTerritorio {

    private String nome;
    private String regiao;
    private int populacaoEstimada;
    private int domiciliosEmAreaDeRisco;
    private final String codigoAcesso;
    private final CatalogoLimiares catalogoLimiares;

    public DadosTerritorio(String nome, String regiao, int populacaoEstimada, int domiciliosEmAreaDeRisco,
                           String codigoAcesso, CatalogoLimiares catalogoLimiares) {
        if (codigoAcesso == null || codigoAcesso.isBlank()) {
            throw new DadosInvalidosException("O território precisa de um código de acesso para proteger os limiares.");
        }
        if (catalogoLimiares == null) {
            throw new DadosInvalidosException("O território precisa saber onde os limiares ficam guardados.");
        }
        setNome(nome);
        setRegiao(regiao);
        setPopulacaoEstimada(populacaoEstimada);
        setDomiciliosEmAreaDeRisco(domiciliosEmAreaDeRisco);
        this.codigoAcesso = codigoAcesso;
        this.catalogoLimiares = catalogoLimiares;
    }

    /**
     * Altera os limiares de um tipo de sensor. Só passa se o código de acesso
     * estiver certo, o tipo existir e os valores estiverem em ordem.
     */
    public void alterarLimiares(String codigoInformado, String tipoSensor, double atencao, double alerta,
                                double critico) {
        validarCodigoAcesso(codigoInformado);
        // se o tipo não existir, a busca já lança DadosInvalidosException
        catalogoLimiares.buscarLimiares(tipoSensor);
        Limiares novos = new Limiares(atencao, alerta, critico);
        catalogoLimiares.definirLimiares(tipoSensor, novos);
    }

    public Limiares consultarLimiares(String tipoSensor) {
        return catalogoLimiares.buscarLimiares(tipoSensor);
    }

    private void validarCodigoAcesso(String codigoInformado) {
        if (codigoInformado == null || !codigoAcesso.equals(codigoInformado.trim())) {
            throw new DadosInvalidosException("Código de acesso incorreto: os limiares não foram alterados.");
        }
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("O nome do território é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        if (regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("A região do território é obrigatória.");
        }
        this.regiao = regiao.trim();
    }

    public int getPopulacaoEstimada() {
        return populacaoEstimada;
    }

    public void setPopulacaoEstimada(int populacaoEstimada) {
        if (populacaoEstimada < 0) {
            throw new DadosInvalidosException(
                    "A população estimada não pode ser negativa (recebido: " + populacaoEstimada + ").");
        }
        this.populacaoEstimada = populacaoEstimada;
    }

    public int getDomiciliosEmAreaDeRisco() {
        return domiciliosEmAreaDeRisco;
    }

    public void setDomiciliosEmAreaDeRisco(int domiciliosEmAreaDeRisco) {
        if (domiciliosEmAreaDeRisco < 0) {
            throw new DadosInvalidosException(
                    "O número de domicílios em área de risco não pode ser negativo (recebido: "
                            + domiciliosEmAreaDeRisco + ").");
        }
        this.domiciliosEmAreaDeRisco = domiciliosEmAreaDeRisco;
    }
}
