package br.com.fiap.territorio.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação de números e datas usada nas mensagens do sistema,
 * para ficar igual em qualquer máquina (vírgula decimal, dd/MM/yyyy).
 */
public final class Formato {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Formato() {
    }

    public static String numero(double valor) {
        return String.format(BRASIL, "%.2f", valor);
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora.format(DATA_HORA);
    }
}
