# Observabilidad - Métricas del Sistema Conversacional

## Justificación de Métricas

Este documento explica las métricas implementadas y por qué son relevantes para un sistema conversacional en producción.

---

## Métricas Funcionales

### 1. Conversaciones Creadas (`conversations.created`)
**Tipo:** Counter  
**Descripción:** Contador total de conversaciones iniciadas.

**Por qué es relevante:**
- Permite medir el volumen de uso del sistema
- Ayuda a dimensionar la infraestructura necesaria
- Identifica picos de demanda para planificación de capacidad
- KPI clave para medir adopción del asistente virtual

**Alertas sugeridas:**
- Caída abrupta (>50% en 1 hora): posible problema en el servicio
- Pico inusual (>300% del promedio): posible ataque o campaña exitosa

---

### 2. Conversaciones Activas (`conversations.active`)
**Tipo:** Gauge  
**Descripción:** Número actual de conversaciones en curso.

**Por qué es relevante:**
- Indica la carga en tiempo real del sistema
- Permite detectar saturación antes de que afecte el rendimiento
- Útil para auto-scaling horizontal
- Ayuda a dimensionar pool de conexiones a base de datos

**Alertas sugeridas:**
- >1000 conversaciones activas: considerar escalar horizontalmente
- Crecimiento sostenido sin decrementos: posible memory leak

---

### 3. Mensajes Procesados (`messages.processed`)
**Tipo:** Counter  
**Descripción:** Total de mensajes procesados por el sistema.

**Por qué es relevante:**
- Mide la actividad real del sistema (más preciso que conversaciones)
- Permite calcular throughput (mensajes/segundo)
- Útil para facturación si el servicio es de pago
- Identifica conversaciones largas vs. cortas

**Cálculos derivados:**
- Mensajes por conversación = messages.processed / conversations.created
- Throughput = messages.processed / tiempo

---

### 4. Intenciones Detectadas (`intents.detected`)
**Tipo:** Counter con tags por tipo de intención  
**Descripción:** Contadores por cada tipo de intención (GREETING, WEATHER_QUERY, etc.).

**Por qué es relevante:**
- Identifica los casos de uso más frecuentes
- Permite priorizar mejoras en intenciones populares
- Detecta cambios en comportamiento de usuarios
- UNKNOWN alto indica necesidad de entrenar más intenciones

**Alertas sugeridas:**
- UNKNOWN >30%: el sistema no entiende a los usuarios
- Cambio drástico en distribución: posible problema en detección

---

### 5. Llamadas a API Externa (`external.api.calls`)
**Tipo:** Counter  
**Descripción:** Total de llamadas a OpenWeather API.

**Por qué es relevante:**
- Control de costos (APIs suelen cobrar por llamada)
- Identifica dependencia de servicios externos
- Permite calcular rate limiting
- Útil para cache strategy

**Alertas sugeridas:**
- >10,000 llamadas/hora: revisar estrategia de caché
- Crecimiento no lineal con conversaciones: posible bug

---

### 6. Tasa de Éxito de API Externa (`external.api.success` / `external.api.failures`)
**Tipo:** Counters  
**Descripción:** Llamadas exitosas vs. fallidas a servicios externos.

**Por qué es relevante:**
- **Métrica crítica de confiabilidad**
- Success rate <95%: degradación de experiencia de usuario
- Identifica problemas con proveedores externos
- Permite activar fallbacks o circuit breakers

**Alertas sugeridas:**
- Success rate <80%: alerta crítica
- Failures >100 en 5 minutos: posible outage del proveedor

---

## Métricas Técnicas

### 7. Tiempo de Respuesta por Intención (`response.time`)
**Tipo:** Timer con tags por intención  
**Descripción:** Latencia desde recepción del mensaje hasta respuesta.

**Por qué es relevante:**
- **SLA crítico**: usuarios esperan respuestas <2 segundos
- Identifica intenciones lentas que necesitan optimización
- WEATHER_QUERY debería ser más lenta (llamada externa)
- Detecta degradación de rendimiento

**Alertas sugeridas:**
- p95 >2 segundos: experiencia degradada
- p99 >5 segundos: alerta crítica

**Valores esperados:**
- GREETING, HELP, FAREWELL: <100ms
- WEATHER_QUERY: <1500ms (incluye llamada externa)
- UNKNOWN: <50ms

---

### 8. Errores por Tipo (`errors.occurred`)
**Tipo:** Counter con tags por tipo de error  
**Descripción:** Contadores de errores clasificados.

**Por qué es relevante:**
- Permite identificar patrones de fallo
- Prioriza correcciones según frecuencia
- Detecta regresiones después de deployments
- Alimenta dashboards de health del sistema

**Tipos de errores rastreados:**
- `weather_api_error`: Fallos en OpenWeather API
- `validation_error`: Inputs inválidos
- `database_error`: Problemas de persistencia
- `unknown_error`: Errores no categorizados

---

## Métricas Derivadas (Calculadas)

### Success Rate de API Externa
```
success_rate = (external.api.success / external.api.calls) * 100
```
**Target:** >95%

### Mensajes por Conversación
```
avg_messages = messages.processed / conversations.created
```
**Típico:** 2-5 mensajes por conversación

### Throughput del Sistema
```
throughput = messages.processed / uptime_seconds
```
**Útil para:** Dimensionar infraestructura

---

## Dashboard Recomendado

### Panel 1: Actividad del Sistema
- Conversaciones creadas (últimas 24h)
- Conversaciones activas (tiempo real)
- Mensajes procesados (últimas 24h)

### Panel 2: Intenciones
- Distribución de intenciones (pie chart)
- Tendencia de UNKNOWN (line chart)

### Panel 3: Dependencias Externas
- Success rate de OpenWeather API (gauge)
- Llamadas por hora (line chart)

### Panel 4: Performance
- p50, p95, p99 de response.time (line chart)
- Response time por intención (heatmap)

### Panel 5: Errores
- Total de errores (últimas 24h)
- Errores por tipo (stacked bar chart)

---

## Escalabilidad de Métricas

### Consideraciones para Producción

**Retención:**
- Raw metrics: 15 días
- Aggregated (1h): 90 días
- Aggregated (1d): 1 año

**Cardinalidad:**
- Evitar tags con alta cardinalidad (ej: sessionId, userId)
- Usar tags con valores limitados (ej: intent tiene solo 5 valores)

**Performance:**
- Métricas agregadas en memoria (Micrometer)
- Export cada 60 segundos a backend (Prometheus/Datadog)
- No bloquean requests del usuario

---

## Integración con Herramientas

### Prometheus
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'conversational-assistant'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Grafana
- Importar dashboard desde `/actuator/metrics`
- Alertas configuradas en Prometheus

### Datadog / New Relic
- Integración nativa con Micrometer
- APM automático para trazas distribuidas

---

## Conclusión

Las métricas implementadas cubren los aspectos críticos de un sistema conversacional:
- **Funcionales:** ¿Qué está haciendo el sistema?
- **Técnicas:** ¿Cómo lo está haciendo?
- **Negocio:** ¿Está cumpliendo su propósito?

Estas métricas permiten:
1. Detectar problemas antes de que afecten usuarios
2. Dimensionar infraestructura correctamente
3. Priorizar mejoras basadas en datos
4. Cumplir SLAs de rendimiento
5. Optimizar costos de APIs externas
