-- Esquema do banco. Executado na inicializacao pela classe ConexaoBanco.
-- IF NOT EXISTS garante que os dados antigos nao sejam apagados ao reiniciar.

CREATE TABLE IF NOT EXISTS medicao (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_sensor VARCHAR(20)      NOT NULL,
    tipo_sensor   VARCHAR(30)      NOT NULL,
    valor         DOUBLE PRECISION NOT NULL,
    unidade       VARCHAR(10)      NOT NULL,
    data_hora     TIMESTAMP        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_medicao_sensor ON medicao (codigo_sensor, data_hora);

CREATE TABLE IF NOT EXISTS alerta (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_sensor    VARCHAR(20)      NOT NULL,
    tipo_sensor      VARCHAR(30)      NOT NULL,
    nivel            VARCHAR(10)      NOT NULL,
    valor_previsto   DOUBLE PRECISION NOT NULL,
    limite_estourado DOUBLE PRECISION NOT NULL,
    estrategia       VARCHAR(60)      NOT NULL,
    justificativa    VARCHAR(1000)    NOT NULL,
    data_hora        TIMESTAMP        NOT NULL
);
