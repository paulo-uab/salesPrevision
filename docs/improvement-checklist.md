# SalesPrevision — Checklist de Melhorias Arquitecturais

## Fase 1 — Produção-Ready (Crítico)

### Infraestrutura e Deploy
- [x] Substituir URLs hardcoded (`localhost:8081`, etc.) por variáveis de ambiente — perfil `docker` (`application-docker.properties`) por serviço, ativado via `SPRING_PROFILES_ACTIVE`
- [x] Adicionar API Gateway (Spring Cloud Gateway) como ponto de entrada único — `gateway-service` (:8080), 13 rotas, validação JWT via JWKS, circuit breaker por rota
- [ ] Implementar service discovery (Eureka ou DNS nativo de Kubernetes) — adiado deliberadamente por agora
- [ ] Substituir `ddl-auto=update` por Flyway ou Liquibase para migrations controladas
- [ ] Migrar storage de ficheiros do sistema de ficheiros local para storage externo (MinIO / S3 / Azure Blob) — decisão: fica local por agora
- [x] Containerizar todos os serviços — `docker-compose.yml` na raiz + `Dockerfile` próprio por serviço (build independente: só `core` + o próprio serviço)

### Segurança
- [x] Configurar Spring Security com autenticação JWT em todos os serviços — `user-service` (:8086) emite tokens RS256, publica JWKS em `/.well-known/jwks.json`
- [x] Definir roles e autorização por endpoint — `ServiceRole` (`core`): `{TEMPLATE,INGESTION,PIPELINE,BATCH}_{READ,EDIT,EXECUTE}` + `USER_{READ,EDIT}`, gating por endpoint em todos os 5 serviços backend
- [ ] Adicionar HTTPS / TLS na comunicação entre serviços

### Processamento
- [x] Tornar a ingestão de ficheiros assíncrona — processamento via Kafka (`IngestionJobEvent`, consumer + dead-letter-topic) no `ingestion-service`
- [ ] Substituir `String.split()` no CSV parser por Apache Commons CSV ou OpenCSV (suporte a campos entre aspas, delimitadores escapados, newlines em campos)

---

## Fase 2 — Resiliência e Observabilidade

### Resiliência Inter-Serviços
- [x] Adicionar Resilience4j aos clientes REST (circuit breaker, retry com backoff exponencial) — `ingestion-service`/`pipeline-service` → `template-service`, `batch-service` → `pipeline-service`/`ingestion-service`
- [x] Definir fallback para quando um serviço dependente está indisponível — `ServiceUnavailableException` (`core`) → 503, lançada pelos métodos de fallback do Resilience4j
- [ ] Adicionar retry com backoff no `ForecastItemWriter` para falhas na API de previsão externa (fica fora do âmbito do Resilience4j já aplicado — é uma URL arbitrária definida pelo utilizador, não um serviço interno)

### Scheduler e Batch
- [x] Persistir `lastRunTimes` do `BatchScheduler` na base de dados — coluna `lastRunAt` em `BatchScheduleConfig`
- [ ] Tornar o `chunkSize` (actualmente hardcoded a 100) configurável via `application.properties`
- [ ] Tornar o tamanho de página do `ForecastItemReader` (actualmente 200) configurável
- [ ] Implementar mecanismo de replay de schedules que falharam durante downtime

### Observabilidade
- [x] Adicionar Spring Boot Actuator (health, info, metrics) em todos os serviços — 5 serviços backend, `/actuator/**` público
- [ ] Integrar OpenTelemetry ou Spring Sleuth para tracing distribuído entre serviços
- [x] Adicionar correlation ID nas chamadas inter-serviços para rastreabilidade end-to-end — `CorrelationIdFilter`/`CorrelationIdAutoConfiguration` (`core`)
- [ ] Expor métricas Prometheus e configurar dashboards Grafana
- [ ] Adicionar logging estruturado (JSON) com campos consistentes (serviceId, traceId, jobId)

### Qualidade de Dados
- [ ] Registar falhas silenciosas do `TransformationEngine` em log estruturado com contador no `BatchExecution`
- [ ] Alertar quando `recordsFailed` ultrapassa threshold configurável numa execução batch

---

## Fase 3 — Escalabilidade e Manutenibilidade

### APIs
- [ ] Adicionar paginação (`Pageable`) aos endpoints de listagem sem paginação:
  - [ ] `GET v1/api/templates`
  - [ ] `GET /api/pipelines`
  - [ ] `GET /api/batch/schedules`
  - [ ] `GET /api/ingestion/jobs`
- [ ] Uniformizar versionamento de APIs — aplicar prefixo `v1/` em todos os serviços (actualmente só o template-service tem)

### Acoplamento
- [ ] Remover dependência directa do módulo `core` nos contratos de comunicação inter-serviços — cada serviço deve ter os seus próprios DTOs na fronteira
- [ ] Manter `core` apenas para excepções e enums verdadeiramente partilhados

### Performance
- [ ] Mover filtro de `lookbackDays` do cliente (`ForecastItemReader`) para query param no ingestion-service — evitar transferência de dados desnecessários entre serviços
- [ ] Avaliar índices de base de dados nas colunas usadas em filtros frequentes (`templateId`, `status`, `createdAt`)

---

## Fase 4 — Evolução Arquitectural (Futuro)

### Mensageria e Eventos
- [x] Introduzir broker de mensagens (Kafka) para desacoplar ingestão e processamento — `ingestion-service` publica/consome `IngestionJobEvent` (com dead-letter-topic); usado internamente para desacoplar o upload do parsing, não (ainda) consumido pelo batch-service
- [ ] Publicar evento `IngestionCompleted` para o batch-service reagir em vez de polling periódico — o batch-service continua a consultar `GET /api/ingestion/jobs` por REST
- [ ] Avaliar event sourcing para auditoria completa de transformações aplicadas

### Suporte a Formatos
- [ ] Implementar `JsonIngestionProcessor`
- [ ] Implementar `XlsxIngestionProcessor`
- [ ] Implementar `TxtIngestionProcessor`

### Developer Experience
- [x] Adicionar `docker-compose.yml` para arrancar todos os serviços localmente com um comando
- [x] Configurar perfis Spring (`dev`, `docker`) com configurações diferenciadas — `prod` (Postgres) ainda não existe
- [ ] Adicionar testes de integração inter-serviços (ex: Testcontainers + WireMock)
- [x] Publicar contratos de API em OpenAPI/Swagger com `springdoc-openapi` — anotações em inglês em todos os endpoints expostos

---

## Legenda de Prioridade

| Símbolo | Significado |
|---|---|
| Fase 1 | Bloqueador para produção |
| Fase 2 | Necessário para operação fiável |
| Fase 3 | Importante para crescimento |
| Fase 4 | Investimento para longo prazo |
