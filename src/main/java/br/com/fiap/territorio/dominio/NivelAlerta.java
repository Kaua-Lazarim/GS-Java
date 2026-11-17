package br.com.fiap.territorio.dominio;

/**
 * Níveis de alerta, do mais leve para o mais grave. A recomendação é só uma
 * sugestão para a equipe: quem decide o que fazer é a pessoa responsável.
 */
public enum NivelAlerta {

    ATENCAO("ATENÇÃO",
            "Acompanhar as próximas leituras e avisar os responsáveis pelo ponto."),
    ALERTA("ALERTA",
            "Deixar as equipes de prontidão e avisar os moradores das áreas mais baixas."),
    CRITICO("CRÍTICO",
            "Acionar a Defesa Civil e avaliar a retirada preventiva das famílias em área de risco.");

    private final String rotulo;
    private final String recomendacao;

    NivelAlerta(String rotulo, String recomendacao) {
        this.rotulo = rotulo;
        this.recomendacao = recomendacao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getRecomendacao() {
        return recomendacao;
    }
}
