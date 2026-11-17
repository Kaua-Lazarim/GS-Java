package br.com.fiap.territorio.dominio;

import br.com.fiap.territorio.excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ponto vulnerável do território (uma ponte, uma encosta) onde ficam
 * instalados os sensores.
 */
public class PontoMonitoramento extends ComponenteTerritorial {

    private final String descricaoRisco;
    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(int id, String nome, String regiao, String descricaoRisco) {
        super(id, nome, regiao);
        if (descricaoRisco == null || descricaoRisco.isBlank()) {
            throw new DadosInvalidosException("Descreva o risco do ponto de monitoramento \"" + nome + "\".");
        }
        this.descricaoRisco = descricaoRisco.trim();
    }

    public void adicionarSensor(Sensor sensor) {
        if (sensor == null) {
            throw new DadosInvalidosException("Não é possível instalar um sensor nulo no ponto " + getNome() + ".");
        }
        for (Sensor instalado : sensores) {
            if (instalado.getCodigo().equalsIgnoreCase(sensor.getCodigo())) {
                throw new DadosInvalidosException(
                        "Já existe um sensor com o código " + sensor.getCodigo() + " no ponto " + getNome() + ".");
            }
        }
        sensores.add(sensor);
    }

    /**
     * Todos os sensores funcionando = operacional; só alguns = degradado;
     * nenhum (ou ponto sem sensor) = inoperante.
     */
    @Override
    public StatusComponente verificarStatus() {
        int funcionando = 0;
        for (Sensor sensor : sensores) {
            if (sensor.verificarFuncionamento()) {
                funcionando++;
            }
        }

        StatusComponente novoStatus;
        if (sensores.isEmpty() || funcionando == 0) {
            novoStatus = StatusComponente.INOPERANTE;
        } else if (funcionando == sensores.size()) {
            novoStatus = StatusComponente.OPERACIONAL;
        } else {
            novoStatus = StatusComponente.DEGRADADO;
        }
        setStatus(novoStatus);
        return novoStatus;
    }

    public String getDescricaoRisco() {
        return descricaoRisco;
    }

    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }
}
