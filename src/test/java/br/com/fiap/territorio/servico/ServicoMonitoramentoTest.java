package br.com.fiap.territorio.servico;

import br.com.fiap.territorio.dominio.Alerta;
import br.com.fiap.territorio.dominio.CatalogoLimiares;
import br.com.fiap.territorio.dominio.EstrategiaPrevisao;
import br.com.fiap.territorio.dominio.Limiares;
import br.com.fiap.territorio.dominio.Medicao;
import br.com.fiap.territorio.dominio.NivelAlerta;
import br.com.fiap.territorio.dominio.PrevisaoMediaMovel;
import br.com.fiap.territorio.dominio.Sensor;
import br.com.fiap.territorio.excecao.DadosInvalidosException;
import br.com.fiap.territorio.excecao.FonteIndisponivelException;
import br.com.fiap.territorio.persistencia.AlertaDAO;
import br.com.fiap.territorio.persistencia.MedicaoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa a lógica de leitura e de alerta sem banco de dados: os DAOs, os
 * limiares e o sensor são mocks do Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    private static final String CODIGO = "PLU-01";
    private static final String TIPO = "PLUVIOMETRO";
    private static final int LEITURAS_CONSULTADAS = 20;

    @Mock
    private MedicaoDAO medicaoDAO;
    @Mock
    private AlertaDAO alertaDAO;
    @Mock
    private CatalogoLimiares catalogoLimiares;
    @Mock
    private Sensor sensor;

    private ServicoMonitoramento servico;

    @BeforeEach
    void preparar() {
        // dados fixos do sensor; lenient porque nem todo teste usa todos
        lenient().when(sensor.getCodigo()).thenReturn(CODIGO);
        lenient().when(sensor.getTipo()).thenReturn(TIPO);
        lenient().when(sensor.getUnidade()).thenReturn("mm/h");
        lenient().when(sensor.getValorMinimo()).thenReturn(0.0);
        lenient().when(sensor.getValorMaximo()).thenReturn(120.0);
        lenient().when(catalogoLimiares.buscarLimiares(TIPO)).thenReturn(new Limiares(20, 35, 50));

        List<EstrategiaPrevisao> estrategias = List.of(new PrevisaoMediaMovel(5, 5));
        servico = new ServicoMonitoramento(medicaoDAO, alertaDAO, catalogoLimiares, estrategias,
                LEITURAS_CONSULTADAS);
    }

    @Test
    void deveGravarAMedicaoQuandoALeituraEValida() throws FonteIndisponivelException {
        // Arrange
        when(sensor.verificarFuncionamento()).thenReturn(true);
        when(sensor.lerValor()).thenReturn(25.5);
        when(medicaoDAO.salvar(any(Medicao.class)))
                .thenAnswer(chamada -> chamada.getArgument(0, Medicao.class).comId(1L));
        ArgumentCaptor<Medicao> captor = ArgumentCaptor.forClass(Medicao.class);

        // Act
        Medicao gravada = servico.registrarLeitura(sensor);

        // Assert
        verify(medicaoDAO).salvar(captor.capture());
        assertEquals(CODIGO, captor.getValue().getCodigoSensor());
        assertEquals(25.5, captor.getValue().getValor(), 0.0001);
        assertEquals(1L, gravada.getId());
    }

    @Test
    void naoDeveGravarLeituraForaDaFaixaDoSensor() throws FonteIndisponivelException {
        // Arrange
        when(sensor.verificarFuncionamento()).thenReturn(true);
        when(sensor.lerValor()).thenReturn(-1.0);

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> servico.registrarLeitura(sensor));

        // Assert
        assertTrue(erro.getMessage().contains(CODIGO));
        verify(medicaoDAO, never()).salvar(any(Medicao.class));
    }

    @Test
    void deveLancarFonteIndisponivelQuandoOSensorEstaForaDeOperacao() throws FonteIndisponivelException {
        // Arrange
        when(sensor.verificarFuncionamento()).thenReturn(false);

        // Act
        assertThrows(FonteIndisponivelException.class, () -> servico.registrarLeitura(sensor));

        // Assert
        verify(sensor, never()).lerValor();
        verify(medicaoDAO, never()).salvar(any(Medicao.class));
    }

    @Test
    void deveEmitirAlertaCriticoQuandoAPrevisaoAtingeOLimiteCritico() {
        // Arrange
        when(medicaoDAO.buscarUltimasPorSensor(CODIGO, LEITURAS_CONSULTADAS))
                .thenReturn(historico(60, 60, 60, 60, 60));
        when(alertaDAO.salvar(any(Alerta.class)))
                .thenAnswer(chamada -> chamada.getArgument(0, Alerta.class).comId(7L));
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);

        // Act
        List<ResultadoPrevisao> resultados = servico.rodarPrevisoes(sensor);

        // Assert
        verify(alertaDAO).salvar(captor.capture());
        Alerta alerta = captor.getValue();
        assertEquals(NivelAlerta.CRITICO, alerta.getNivel());
        assertEquals(60.0, alerta.getValorPrevisto(), 0.0001);
        assertEquals(50.0, alerta.getLimiteEstourado(), 0.0001);
        assertTrue(alerta.getJustificativa().contains(CODIGO));
        assertTrue(resultados.get(0).getAlerta().isPresent());
    }

    @Test
    void deveEmitirApenasAtencaoQuandoAPrevisaoFicaEntreAtencaoEAlerta() {
        // Arrange: média de 22, 24, 26, 28 e 30 = 26 (atenção é 20, alerta é 35)
        when(medicaoDAO.buscarUltimasPorSensor(CODIGO, LEITURAS_CONSULTADAS))
                .thenReturn(historico(22, 24, 26, 28, 30));
        when(alertaDAO.salvar(any(Alerta.class)))
                .thenAnswer(chamada -> chamada.getArgument(0, Alerta.class).comId(8L));
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);

        // Act
        servico.rodarPrevisoes(sensor);

        // Assert
        verify(alertaDAO).salvar(captor.capture());
        assertEquals(NivelAlerta.ATENCAO, captor.getValue().getNivel());
        assertEquals(20.0, captor.getValue().getLimiteEstourado(), 0.0001);
    }

    @Test
    void naoDeveEmitirAlertaQuandoAPrevisaoFicaAbaixoDosLimiares() {
        // Arrange
        when(medicaoDAO.buscarUltimasPorSensor(CODIGO, LEITURAS_CONSULTADAS))
                .thenReturn(historico(4, 6, 5, 7, 3));

        // Act
        List<ResultadoPrevisao> resultados = servico.rodarPrevisoes(sensor);

        // Assert
        verify(alertaDAO, never()).salvar(any(Alerta.class));
        assertTrue(resultados.get(0).getPrevisao().isConfiavel());
        assertFalse(resultados.get(0).getAlerta().isPresent());
    }

    @Test
    void naoDeveAlertarQuandoOHistoricoEInsuficienteMesmoComValoresAltos() {
        // Arrange: só 3 leituras, todas acima do crítico
        when(medicaoDAO.buscarUltimasPorSensor(CODIGO, LEITURAS_CONSULTADAS))
                .thenReturn(historico(90, 95, 99));

        // Act
        List<ResultadoPrevisao> resultados = servico.rodarPrevisoes(sensor);

        // Assert
        verify(alertaDAO, never()).salvar(any(Alerta.class));
        assertFalse(resultados.get(0).getPrevisao().isConfiavel());
    }

    private static List<Medicao> historico(double... valores) {
        List<Medicao> medicoes = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 8, 0);
        for (int i = 0; i < valores.length; i++) {
            medicoes.add(new Medicao((long) (i + 1), CODIGO, TIPO, valores[i], "mm/h", inicio.plusHours(i)));
        }
        return medicoes;
    }
}
