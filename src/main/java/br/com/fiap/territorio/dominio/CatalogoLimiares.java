package br.com.fiap.territorio.dominio;

/**
 * Lugar de onde os limiares são lidos e onde são alterados.
 * Quem implementa é a ConfiguracaoSistema. Com a interface, o domínio não
 * precisa importar nada do pacote de serviço e os testes podem usar um mock.
 */
public interface CatalogoLimiares {

    /** Limiares vigentes para um tipo de sensor. Lança DadosInvalidosException se o tipo não existir. */
    Limiares buscarLimiares(String tipoSensor);

    /** Troca os limiares de um tipo de sensor que já existe. */
    void definirLimiares(String tipoSensor, Limiares limiares);
}
