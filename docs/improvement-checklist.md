# SalesPrevision — Checklist de Melhorias Arquitecturais

## Fase 1 — Produção-Ready (Crítico)

### Infraestrutura e Deploy
- [ ] Substituir URLs hardcoded (`localhost:8081`, etc.) por variáveis de ambiente
- [ ] Adicionar API Gateway (Spring Cloud Gateway) como ponto de entrada único
- [ ] Implementar service discovery (Eureka ou DNS nativo de Kubernetes)
- [ ] Substituir `ddl-auto=update` por Flyway ou Liquibase para migrations controladas
- [ ] Migrar storage de ficheiros do sistema de ficheiros local para storage externo (MinIO / S3 / Azure Blob)

### Segurança
- [ ] Configurar Spring Security com autenticação JWT em todos os serviços
- [ ] Definir roles e autorização por endpoint (ex: ADMIN para criar templates, USER para ingestão)
- [ ] Adicionar HTTPS / TLS na comunicação entre serviços

### Processamento
- [ ] Tornar a ingestão de ficheiros assíncrona (`@Async` ou mensageria) — responder com `202 Accepted` imediatamente
- [ ] Substituir `String.split()` no CSV parser por Apache Commons CSV ou OpenCSV (suporte a campos entre aspas, delimitadores escapados, newlines em campos)

---

## Fase 2 — Resiliência e Observabilidade

### Resiliência Inter-Serviços
- [ ] Adicionar Resilience4j aos clientes REST (circuit breaker, retry com backoff exponencial, timeout)
- [ ] Definir fallback para quando template-service está indisponível (ex: cache local de templates)
- [ ] Adicionar retry com backoff no `ForecastItemWriter` para falhas na API de previsão externa

### Scheduler e Batch
- [ ] Persistir `lastRunTimes` do `BatchScheduler` na base de dados (actualmente em memória — perdido no restart)
- [ ] Tornar o `chunkSize` (actualmente hardcoded a 100) configurável via `application.properties`
- [ ] Tornar o tamanho de página do `ForecastItemReader` (actualmente 200) configurável
- [ ] Implementar mecanismo de replay de schedules que falharam durante downtime

### Observabilidade
- [ ] Adicionar Spring Boot Actuator (health, info, metrics) em todos os serviços
- [ ] Integrar OpenTelemetry ou Spring Sleuth para tracing distribuído entre serviços
- [ ] Adicionar correlation ID nas chamadas inter-serviços para rastreabilidade end-to-end
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
- [ ] Introduzir broker de mensagens (Kafka ou RabbitMQ) para desacoplar ingestão e processamento
- [ ] Publicar evento `IngestionCompleted` quando um job termina — permitir ao batch-service reagir em vez de polling periódico
- [ ] Avaliar event sourcing para auditoria completa de transformações aplicadas

### Suporte a Formatos
- [ ] Implementar `JsonIngestionProcessor`
- [ ] Implementar `XlsxIngestionProcessor`
- [ ] Implementar `TxtIngestionProcessor`

### Developer Experience
- [ ] Adicionar `docker-compose.yml` para arrancar todos os serviços localmente com um comando
- [ ] Configurar perfis Spring (`dev`, `prod`) com configurações diferenciadas
- [ ] Adicionar testes de integração inter-serviços (ex: Testcontainers + WireMock)
- [ ] Publicar contratos de API em OpenAPI/Swagger com `springdoc-openapi`

---

## Legenda de Prioridade

| Símbolo | Significado |
|---|---|
| Fase 1 | Bloqueador para produção |
| Fase 2 | Necessário para operação fiável |
| Fase 3 | Importante para crescimento |
| Fase 4 | Investimento para longo prazo |
