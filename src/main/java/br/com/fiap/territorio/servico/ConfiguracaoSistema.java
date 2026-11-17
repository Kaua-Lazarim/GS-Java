package br.com.fiap.territorio.servico;

import br.com.fiap.territorio.dominio.CatalogoLimiares;
import br.com.fiap.territorio.dominio.Limiares;
import br.com.fiap.territorio.dominio.SensorNivelCorrego;
import br.com.fiap.territorio.dominio.SensorPluviometro;
import br.com.fiap.territorio.dominio.SensorUmidadeSolo;
import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Singleton com a configuração do sistema: limiares por tipo de sensor,
 * parâmetros das previsões e dados de conexão com o banco.
 *
 * Só existe uma instância porque todo mundo (menu, serviço, território)
 * precisa enxergar os MESMOS limiares. Se cada um tivesse a sua cópia, um
 * limiar alterado no menu não valeria para os alertas.
 */
public final class ConfiguracaoSistema implements CatalogoLimiares {

    private static final String ARQUIVO = "/configuracao.properties";

    private static ConfiguracaoSistema instancia;

    private final Map<String, Limiares> limiaresPorTipo = new LinkedHashMap<>();
    private List<String> estrategiasAtivas;
    private String urlBanco;
    private String usuarioBanco;
    private String senhaBanco;
    private String codigoAcesso;
    private int minimoLeituras;
    private int janelaMediaMovel;
    private int periodoComparacao;
    private int leiturasConsultadas;

    /** Construtor privado: ninguém de fora consegue dar new. */
    private ConfiguracaoSistema() {
        carregar();
    }

    public static synchronized ConfiguracaoSistema getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracaoSistema();
        }
        return instancia;
    }

    /** Volta tudo para o que está no arquivo de configuração (usado nos testes). */
    public void restaurarPadroes() {
        carregar();
    }

    private void carregar() {
        Properties propriedades = lerArquivo();

        urlBanco = propriedades.getProperty("banco.url", "jdbc:h2:file:./dados/territorio;DB_CLOSE_DELAY=-1").trim();
        usuarioBanco = propriedades.getProperty("banco.usuario", "sa").trim();
        senhaBanco = propriedades.getProperty("banco.senha", "").trim();
        codigoAcesso = propriedades.getProperty("territorio.codigoAcesso", "VILA2026").trim();

        minimoLeituras = lerInteiroPositivo(propriedades, "previsao.minimoLeituras", 5);
        janelaMediaMovel = lerInteiroPositivo(propriedades, "previsao.janelaMediaMovel", 5);
        periodoComparacao = lerInteiroPositivo(propriedades, "previsao.periodoComparacao", 3);
        leiturasConsultadas = lerInteiroPositivo(propriedades, "previsao.leiturasConsultadas", 20);

        estrategiasAtivas = new ArrayList<>();
        String estrategias = propriedades.getProperty("previsao.estrategias", "MEDIA_MOVEL,COMPARACAO");
        for (String chave : estrategias.split(",")) {
            if (!chave.isBlank()) {
                estrategiasAtivas.add(chave.trim().toUpperCase());
            }
        }
        if (estrategiasAtivas.isEmpty()) {
            throw new DadosInvalidosException(
                    "A propriedade previsao.estrategias precisa ter pelo menos uma estratégia.");
        }

        limiaresPorTipo.clear();
        limiaresPorTipo.put(SensorPluviometro.TIPO,
                lerLimiares(propriedades, SensorPluviometro.TIPO, new Limiares(20, 35, 50)));
        limiaresPorTipo.put(SensorNivelCorrego.TIPO,
                lerLimiares(propriedades, SensorNivelCorrego.TIPO, new Limiares(150, 220, 300)));
        limiaresPorTipo.put(SensorUmidadeSolo.TIPO,
                lerLimiares(propriedades, SensorUmidadeSolo.TIPO, new Limiares(70, 80, 90)));
    }

    private Properties lerArquivo() {
        Properties propriedades = new Properties();
        try (InputStream entrada = ConfiguracaoSistema.class.getResourceAsStream(ARQUIVO)) {
            if (entrada == null) {
                System.out.println("Aviso: " + ARQUIVO + " não encontrado. Usando a configuração padrão.");
                return propriedades;
            }
            propriedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Aviso: não foi possível ler " + ARQUIVO + " (" + e.getMessage()
                    + "). Usando a configuração padrão.");
        }
        return propriedades;
    }

    private int lerInteiroPositivo(Properties propriedades, String chave, int padrao) {
        String texto = propriedades.getProperty(chave);
        if (texto == null || texto.isBlank()) {
            return padrao;
        }
        try {
            int valor = Integer.parseInt(texto.trim());
            if (valor < 1) {
                throw new DadosInvalidosException(
                        "A propriedade " + chave + " deve ser um inteiro maior que zero (está: " + texto + ").");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException(
                    "A propriedade " + chave + " deve ser um número inteiro (está: " + texto + ").");
        }
    }

    private Limiares lerLimiares(Properties propriedades, String tipoSensor, Limiares padrao) {
        String chave = "limiar." + tipoSensor;
        String texto = propriedades.getProperty(chave);
        if (texto == null || texto.isBlank()) {
            return padrao;
        }
        String[] partes = texto.split(",");
        if (partes.length != 3) {
            throw new DadosInvalidosException(
                    "A propriedade " + chave + " deve ter 3 valores: atencao,alerta,critico (está: " + texto + ").");
        }
        try {
            return new Limiares(
                    Double.parseDouble(partes[0].trim()),
                    Double.parseDouble(partes[1].trim()),
                    Double.parseDouble(partes[2].trim()));
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException(
                    "A propriedade " + chave + " tem um valor que não é número (está: " + texto + ").");
        }
    }

    @Override
    public synchronized Limiares buscarLimiares(String tipoSensor) {
        Limiares limiares = tipoSensor == null ? null : limiaresPorTipo.get(tipoSensor.trim().toUpperCase());
        if (limiares == null) {
            throw new DadosInvalidosException(
                    "Não há limiares configurados para o tipo de sensor \"" + tipoSensor + "\". Tipos válidos: "
                            + limiaresPorTipo.keySet() + ".");
        }
        return limiares;
    }

    @Override
    public synchronized void definirLimiares(String tipoSensor, Limiares limiares) {
        if (limiares == null) {
            throw new DadosInvalidosException("Os novos limiares não podem ser nulos.");
        }
        // garante que o tipo existe antes de trocar
        buscarLimiares(tipoSensor);
        limiaresPorTipo.put(tipoSensor.trim().toUpperCase(), limiares);
    }

    public synchronized Set<String> getTiposDeSensor() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(limiaresPorTipo.keySet()));
    }

    public List<String> getEstrategiasAtivas() {
        return Collections.unmodifiableList(estrategiasAtivas);
    }

    public String getUrlBanco() {
        return urlBanco;
    }

    public String getUsuarioBanco() {
        return usuarioBanco;
    }

    public String getSenhaBanco() {
        return senhaBanco;
    }

    public String getCodigoAcesso() {
        return codigoAcesso;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }

    public int getJanelaMediaMovel() {
        return janelaMediaMovel;
    }

    public int getPeriodoComparacao() {
        return periodoComparacao;
    }

    public int getLeiturasConsultadas() {
        return leiturasConsultadas;
    }
}
