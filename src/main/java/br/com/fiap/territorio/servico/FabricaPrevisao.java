package br.com.fiap.territorio.servico;

import br.com.fiap.territorio.dominio.EstrategiaPrevisao;
import br.com.fiap.territorio.dominio.PrevisaoComparacao;
import br.com.fiap.territorio.dominio.PrevisaoMediaMovel;
import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory das estratégias de previsão. É o único lugar do sistema que dá
 * "new" em uma estratégia.
 *
 * Em vez de if/else por tipo, existe um mapa "chave -> como criar". Para
 * incluir uma estratégia nova basta registrar mais uma linha aqui e colocar
 * a chave no configuracao.properties; o serviço e o menu não mudam.
 */
public final class FabricaPrevisao {

    public static final String MEDIA_MOVEL = "MEDIA_MOVEL";
    public static final String COMPARACAO = "COMPARACAO";

    private static final Map<String, Function<ConfiguracaoSistema, EstrategiaPrevisao>> CRIADORES =
            new LinkedHashMap<>();

    static {
        CRIADORES.put(MEDIA_MOVEL, config ->
                new PrevisaoMediaMovel(config.getJanelaMediaMovel(), config.getMinimoLeituras()));
        CRIADORES.put(COMPARACAO, config ->
                new PrevisaoComparacao(config.getPeriodoComparacao(), config.getMinimoLeituras()));
    }

    private FabricaPrevisao() {
    }

    /** Cria uma estratégia a partir da chave (ex.: "MEDIA_MOVEL"). */
    public static EstrategiaPrevisao criar(String chave, ConfiguracaoSistema configuracao) {
        if (chave == null || chave.isBlank()) {
            throw new DadosInvalidosException(
                    "Informe a estratégia de previsão. Opções: " + CRIADORES.keySet() + ".");
        }
        if (configuracao == null) {
            throw new DadosInvalidosException("A fábrica de previsões precisa da configuração do sistema.");
        }
        Function<ConfiguracaoSistema, EstrategiaPrevisao> criador = CRIADORES.get(chave.trim().toUpperCase());
        if (criador == null) {
            throw new DadosInvalidosException(
                    "Estratégia de previsão desconhecida: \"" + chave + "\". Opções: " + CRIADORES.keySet() + ".");
        }
        return criador.apply(configuracao);
    }

    /** Cria todas as estratégias listadas em previsao.estrategias, na ordem do arquivo. */
    public static List<EstrategiaPrevisao> criarAtivas(ConfiguracaoSistema configuracao) {
        if (configuracao == null) {
            throw new DadosInvalidosException("A fábrica de previsões precisa da configuração do sistema.");
        }
        List<EstrategiaPrevisao> estrategias = new ArrayList<>();
        for (String chave : configuracao.getEstrategiasAtivas()) {
            estrategias.add(criar(chave, configuracao));
        }
        return estrategias;
    }

    public static Set<String> chavesDisponiveis() {
        return Set.copyOf(CRIADORES.keySet());
    }
}
