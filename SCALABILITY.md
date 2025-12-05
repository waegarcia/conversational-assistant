# Estrategia de Escalabilidad

## Contexto

Este documento analiza cómo el microservicio de Conversational Assistant puede escalar desde el estado actual (cientos de usuarios) hasta decenas de miles o millones de usuarios concurrentes, identificando cuellos de botella, estrategias de mitigación y trade-offs.

---

## Estado Actual y Capacidad

### Infraestructura Base
- **Aplicación**: Spring Boot monolito stateless
- **Base de datos**: PostgreSQL single instance
- **API Externa**: OpenWeather API (límite gratuito: 1000 llamadas/día)
- **Recursos por instancia**: 2GB RAM, 2 CPU cores

### Capacidad Estimada (Single Instance)

| Métrica | Valor Estimado | Observación |
|---------|----------------|-------------|
| Throughput | 1000 req/s | Sin llamadas a API externa |
| Throughput (con API) | 100-200 req/s | Limitado por latencia de OpenWeather |
| Latencia p50 | 50ms | Intenciones sin API externa |
| Latencia p50 (WEATHER) | 800ms | Dominado por API externa |
| Conexiones DB | 10 concurrentes | HikariCP default |
| Usuarios concurrentes | 500-1000 | Considerando conversaciones activas |

### Cuellos de Botella Identificados

**1. OpenWeather API (CRÍTICO)**
- Latencia: 300-1000ms por llamada
- Rate limit: 1000 llamadas/día (plan gratuito)
- Sin caché: cada consulta golpea la API
- **Impacto**: Bloquea threads, reduce throughput

**2. Base de Datos (ALTO)**
- Single point of failure
- Conexiones limitadas (10 por defecto)
- Queries sin optimización de índices
- Write-heavy workload (cada mensaje = 2 INSERTs)

**3. CPU y Memoria (MEDIO)**
- Detección de intenciones con regex (bajo costo)
- Serialización JSON (Hibernate + Jackson)
- Garbage collection con alta carga

**4. Network I/O (BAJO)**
- Requests HTTP relativamente pequeños
- Responses <5KB típicamente

---

## Estrategia de Escalabilidad

### Fase 1: Optimización Single Instance (0-5,000 usuarios)

**Objetivo**: Maximizar capacidad sin cambios de arquitectura.

#### 1.1 Implementar Caché de OpenWeather API

**Problema**: Cada consulta golpea la API externa.

**Solución**: Redis como caché distribuido.
```java
@Cacheable(value = "weather", key = "#city")
public WeatherResponse getCurrentWeather(String city) {
    // Llamada a API solo si no está en caché
}
```

**Configuración sugerida**:
- TTL: 30 minutos (datos meteorológicos cambian lentamente)
- Eviction: LRU (Least Recently Used)
- Tamaño: 10,000 ciudades en memoria

**Impacto esperado**:
- Reducción de llamadas a API: 90%
- Latencia p50 de WEATHER_QUERY: 800ms → 50ms
- Throughput: 200 req/s → 1000 req/s
- Costo: Redis managed (AWS ElastiCache): $50/mes

#### 1.2 Optimizar Base de Datos

**A. Índices adicionales**
```sql
-- Búsqueda por sessionId (muy frecuente)
CREATE INDEX idx_conversations_session_id ON conversations(session_id);

-- Búsqueda de mensajes por conversación
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);

-- Conversaciones activas por usuario
CREATE INDEX idx_conversations_user_status ON conversations(user_id, status);
```

**B. Connection pooling tuning**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # Aumentar de 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**C. Batch inserts para mensajes**

Agrupar INSERTs de mensajes cuando sea posible.

**Impacto esperado**:
- Reducción de latencia de queries: 30%
- Mayor capacidad de conexiones concurrentes

#### 1.3 Configurar JVM para Alta Carga
```bash
java -jar app.jar \
  -Xms2g -Xmx2g \           # Heap fijo evita resize
  -XX:+UseG1GC \             # G1 para baja latencia
  -XX:MaxGCPauseMillis=200 \ # Pausas GC <200ms
  -XX:+UseStringDeduplication # Optimizar strings
```

**Impacto esperado**:
- Reducción de pausas GC: 50%
- Mejor utilización de memoria

---

### Fase 2: Escalamiento Horizontal (5,000-50,000 usuarios)

**Objetivo**: Múltiples instancias de la aplicación detrás de load balancer.

#### 2.1 Arquitectura Multi-Instancia
```
                    [Load Balancer - ALB]
                            |
        +-----------+-------+-------+-----------+
        |           |               |           |
    [Instance 1] [Instance 2] [Instance 3] [Instance 4]
        |           |               |           |
        +-----------+-------+-------+-----------+
                            |
                    [PostgreSQL Primary]
                            |
                    [Redis Cache]
```

**Configuración**:
- 4 instancias iniciales
- Auto-scaling: CPU >70% → add instance
- Health checks: /actuator/health cada 30s
- Session affinity: NO necesaria (stateless)

**Impacto esperado**:
- Throughput lineal: 4x instances = 4000 req/s
- Alta disponibilidad: tolerancia a fallos de instancias

#### 2.2 Read Replicas para PostgreSQL

**Problema**: Escrituras y lecturas compiten por recursos.

**Solución**: Read replicas para queries de solo lectura.
```java
@Transactional(readOnly = true)
@ReadOnlyConnection // Custom annotation
public ConversationHistoryResponse getConversationHistory(String sessionId) {
    // Esta query va a la replica
}
```

**Configuración**:
- 1 primary (escrituras)
- 2 read replicas (lecturas)
- Replicación asíncrona (lag <1s aceptable)

**Impacto esperado**:
- Capacidad de lectura: 3x
- Primary solo maneja escrituras (mayor throughput)

#### 2.3 Circuit Breaker para OpenWeather API

**Problema**: Si OpenWeather cae, todas las instancias fallan.

**Solución**: Resilience4j circuit breaker.
```java
@CircuitBreaker(name = "weatherApi", fallbackMethod = "fallbackWeather")
public WeatherResponse getCurrentWeather(String city) {
    // Llamada a API
}

private WeatherResponse fallbackWeather(String city, Exception e) {
    // Retornar respuesta genérica o datos cacheados antiguos
}
```

**Configuración**:
- Threshold: 50% de fallos
- Open state: 60 segundos
- Half-open: intentar 3 requests

**Impacto esperado**:
- Mayor resiliencia ante fallos externos
- Evita cascada de errores

---

### Fase 3: Arquitectura Distribuida (50,000-500,000 usuarios)

**Objetivo**: Desacoplar componentes, introducir asincronía.

#### 3.1 Event-Driven con Message Queue

**Problema**: Procesamiento síncrono limita escalabilidad.

**Solución**: RabbitMQ o AWS SQS para procesamiento asíncrono.
```
[API Gateway]
     |
     v
[Message Producer] ---> [Queue: conversations.process]
                              |
                              v
                    [Worker Pool (10+ workers)]
                              |
                              v
                        [PostgreSQL]
                        [Redis Cache]
```

**Flujo asíncrono**:
1. Cliente POST mensaje → retorna 202 Accepted inmediatamente
2. Mensaje va a cola
3. Workers procesan en background
4. Cliente hace polling o recibe webhook

**Impacto esperado**:
- Throughput: 10,000+ req/s (limitado por cola)
- Latencia percibida: <50ms (respuesta inmediata)
- Tolerancia a picos: cola absorbe spikes

#### 3.2 Separar Servicios por Responsabilidad

**Microservicios propuestos**:

1. **Conversation Service** (actual)
    - Gestión de conversaciones
    - Persistencia

2. **Intent Detection Service** (nuevo)
    - Detección de intenciones
    - Puede escalar independientemente
    - Posible migración a ML/NLP

3. **Weather Service** (nuevo)
    - Encapsula integración con OpenWeather
    - Gestión de caché
    - Circuit breaker

4. **Notification Service** (nuevo)
    - Envío de respuestas vía webhook/SSE
    - Desacopla notificación de procesamiento

**Comunicación**:
- Síncrona: REST para requests críticos
- Asíncrona: Message queue para procesamiento

**Impacto esperado**:
- Escalamiento independiente por servicio
- Mayor resiliencia (fallo de uno no afecta a otros)
- Deployment independiente

#### 3.3 Base de Datos Distribuida

**Opción A: Particionamiento (Sharding) por Usuario**
```
Shard 1: userIds A-M  →  PostgreSQL Instance 1
Shard 2: userIds N-Z  →  PostgreSQL Instance 2
```

**Pros**: Escalamiento lineal
**Contras**: Complejidad operacional, queries cross-shard difíciles

**Opción B: CQRS (Command Query Responsibility Segregation)**

- **Write Model**: PostgreSQL para escrituras
- **Read Model**: Elasticsearch para búsquedas y analytics

**Pros**: Lecturas ultra-rápidas, queries complejas
**Contras**: Consistencia eventual, mayor complejidad

**Recomendación**: Opción A si queries son simples, Opción B si analytics es crítico.

---

### Fase 4: Escala Global (500,000+ usuarios)

**Objetivo**: Multi-región, CDN, optimización de latencia global.

#### 4.1 Despliegue Multi-Región
```
[US-EAST]                    [EU-WEST]                    [ASIA-PACIFIC]
   API                          API                           API
   DB Primary                   DB Replica                    DB Replica
   Redis                        Redis                         Redis
```

**Estrategia de replicación**:
- Escrituras: siempre a región primary (US-EAST)
- Lecturas: región más cercana al usuario
- Replicación de DB: asíncrona cross-region

**Routing**:
- DNS-based: Route 53 latency-based routing
- Usuarios de Europa → EU-WEST
- Usuarios de Asia → ASIA-PACIFIC

**Impacto esperado**:
- Latencia global: <100ms para 95% de usuarios
- Alta disponibilidad: tolerancia a fallo de región completa

#### 4.2 CDN para Contenido Estático

**Assets estáticos**:
- Swagger UI
- Documentación
- Imágenes de respuestas (si aplica)

**CDN**: CloudFront o Cloudflare

**Impacto esperado**:
- Reducción de carga en aplicación
- Latencia de assets: <50ms globalmente

#### 4.3 API Gateway Centralizado

**Funciones**:
- Rate limiting por usuario/IP
- Autenticación JWT
- Logging y tracing distribuido
- Throttling

**Herramientas**: AWS API Gateway, Kong, Apigee

**Impacto esperado**:
- Control centralizado de tráfico
- Protección contra abuso

---

## Estrategia de Caché Multi-Nivel

### Nivel 1: Application Cache (Caffeine)
- **Uso**: Intenciones detectadas frecuentemente
- **TTL**: 5 minutos
- **Tamaño**: 1000 entradas
- **Beneficio**: Evita regex repetido

### Nivel 2: Distributed Cache (Redis)
- **Uso**: Respuestas de OpenWeather API
- **TTL**: 30 minutos
- **Tamaño**: 10,000 ciudades
- **Beneficio**: Compartido entre instancias

### Nivel 3: CDN Cache
- **Uso**: Assets estáticos
- **TTL**: 24 horas
- **Beneficio**: Reduce latencia global

---

## Métricas para Monitorear Escalabilidad

### Métricas de Aplicación
- **Throughput**: Requests/segundo actual vs. capacity
- **Latencia**: p50, p95, p99 por endpoint
- **Error rate**: Tasa de errores 4xx/5xx
- **Thread pool**: Threads activos vs. disponibles

### Métricas de Base de Datos
- **Connection pool**: Conexiones activas vs. máximo
- **Query latency**: Tiempo de queries lentas (>100ms)
- **Replication lag**: Delay entre primary y replicas
- **Disk I/O**: IOPS y throughput

### Métricas de Redis
- **Hit rate**: Cache hits / (hits + misses)
- **Evictions**: Entradas expulsadas por falta de memoria
- **Memory usage**: Porcentaje de memoria usada

### Métricas de API Externa
- **Success rate**: Tasa de éxito de OpenWeather API
- **Latencia**: p95 de llamadas externas
- **Rate limit**: Llamadas restantes en ventana

---

## Costos Estimados por Fase

### Fase 1: Single Instance Optimizada
| Item | Costo Mensual |
|------|---------------|
| EC2 t3.medium | $30 |
| RDS PostgreSQL (db.t3.micro) | $15 |
| Redis ElastiCache (cache.t3.micro) | $15 |
| **Total** | **$60/mes** |

**Capacidad**: 5,000 usuarios concurrentes

---

### Fase 2: Escalamiento Horizontal
| Item | Costo Mensual |
|------|---------------|
| ALB | $20 |
| EC2 x4 (t3.medium) | $120 |
| RDS Primary (db.t3.small) | $30 |
| RDS Replicas x2 (db.t3.small) | $60 |
| Redis (cache.t3.small) | $30 |
| **Total** | **$260/mes** |

**Capacidad**: 50,000 usuarios concurrentes

---

### Fase 3: Arquitectura Distribuida
| Item | Costo Mensual |
|------|---------------|
| ECS Fargate (10 tasks) | $150 |
| RDS Aurora Serverless | $100 |
| SQS | $10 |
| Redis Cluster | $100 |
| Elasticsearch Service | $80 |
| **Total** | **$440/mes** |

**Capacidad**: 500,000 usuarios concurrentes

---

### Fase 4: Escala Global
| Item | Costo Mensual |
|------|---------------|
| ECS Multi-Región (3 regiones) | $450 |
| Aurora Global Database | $300 |
| CloudFront CDN | $50 |
| Route 53 | $5 |
| Redis Global Datastore | $300 |
| API Gateway | $35 |
| **Total** | **$1,140/mes** |

**Capacidad**: Millones de usuarios concurrentes

---

## Trade-offs y Decisiones

### Consistencia vs. Disponibilidad (CAP Theorem)

**Decisión actual**: Preferir Consistencia (ACID en PostgreSQL)

**Para alta escala**: Relajar a Consistencia Eventual
- Lecturas de replicas pueden estar levemente desactualizadas (<1s)
- Aceptable para historial de conversaciones
- NO aceptable para datos críticos (balances, transacciones)

---

### Sincronía vs. Asincronía

**Actual**: Todo síncrono
- **Pro**: Respuesta inmediata al usuario
- **Contra**: Throughput limitado

**Futuro (Fase 3+)**: Procesamiento asíncrono
- **Pro**: Alto throughput, absorbe picos
- **Contra**: Usuario debe hacer polling o esperar webhook

**Decisión**: Híbrido
- Intenciones simples (GREETING, HELP): síncronas
- Intenciones costosas (WEATHER_QUERY): asíncronas si carga >70%

---

### Monolito vs. Microservicios

**Actual**: Monolito modular
- **Pro**: Simplicidad operacional, desarrollo rápido
- **Contra**: Escalamiento global, deployment acoplado

**Futuro**: Microservicios (Fase 3+)
- **Pro**: Escalamiento independiente, stack heterogéneo
- **Contra**: Complejidad operacional (service mesh, distributed tracing)

**Decisión**: Monolito hasta 50,000 usuarios, luego migración gradual.

---

## Plan de Implementación Gradual

### Mes 1-2: Preparación
- Implementar caché de OpenWeather (Redis)
- Optimizar índices de base de datos
- Métricas de escalabilidad en dashboard

### Mes 3-4: Escalamiento Horizontal
- Configurar ALB + Auto Scaling
- Desplegar 4 instancias iniciales
- Configurar read replicas

### Mes 5-6: Asincronía
- Implementar message queue (SQS)
- Refactorizar WEATHER_QUERY a asíncrono
- Webhook/SSE para notificaciones

### Mes 7-12: Microservicios
- Separar Intent Detection Service
- Separar Weather Service
- Service mesh (Istio o AWS App Mesh)

### Año 2: Escala Global
- Despliegue multi-región
- Aurora Global Database
- CDN y API Gateway global

---

## Conclusión

El microservicio actual está diseñado para escalar mediante:
1. Arquitectura stateless (permite horizontal scaling)
2. Base de datos relacional robusta (PostgreSQL)
3. Métricas completas para detectar cuellos de botella
4. Separación en capas (facilita refactoring a microservicios)

**Cuellos de botella críticos**:
- OpenWeather API (mitigable con caché)
- PostgreSQL single instance (mitigable con replicas)

**Capacidad estimada por fase**:
- Actual: 1,000 usuarios concurrentes
- Fase 1: 5,000 usuarios ($60/mes)
- Fase 2: 50,000 usuarios ($260/mes)
- Fase 3: 500,000 usuarios ($440/mes)
- Fase 4: Millones de usuarios ($1,140/mes)

**Recomendación**: Implementar caché (Fase 1) como primera optimización. El ROI es inmediato y el costo mínimo.
