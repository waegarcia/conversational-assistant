# Conversational Assistant - Microservicio de Asistente Virtual

Microservicio Java que implementa un asistente virtual conversacional con integración a servicios externos, diseñado para ser escalable, observable y fácil de mantener.

---

## Tabla de Contenidos

- [Características](#características)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [API Endpoints](#api-endpoints)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Testing](#testing)
- [Métricas y Observabilidad](#métricas-y-observabilidad)
- [Deployment](#deployment)
- [Decisiones de Diseño](#decisiones-de-diseño)
- [Roadmap](#roadmap)

---

## Características

- **Procesamiento de Conversaciones**: Maneja múltiples conversaciones simultáneas con persistencia en base de datos
- **Detección de Intenciones**: Identifica automáticamente la intención del usuario (saludo, consulta clima, ayuda, despedida)
- **Integración Externa**: Consulta API de OpenWeather para información meteorológica en tiempo real
- **Persistencia Completa**: Historial de conversaciones y mensajes almacenado en PostgreSQL/H2
- **Observabilidad**: Métricas custom con Micrometer para monitoreo en producción
- **API REST Documentada**: Swagger/OpenAPI UI disponible para exploración interactiva
- **Testing Exhaustivo**: 31 tests unitarios e integración con 100% de éxito
- **Arquitectura Hexagonal**: Separación clara de capas para mantenibilidad y escalabilidad

---

## Arquitectura

El sistema implementa una **Arquitectura Hexagonal** (Ports & Adapters) con las siguientes capas:

### Capas Principales

1. **API Layer**: Controladores REST y manejo de errores
2. **Application Layer**: Lógica de negocio y orquestación
3. **Domain Layer**: Modelo de dominio (entidades, enums, repositorios)
4. **Infrastructure Layer**: Adaptadores externos (base de datos, APIs externas)

### Diagrama de Componentes

Ver diagrama completo en [ARCHITECTURE.md](ARCHITECTURE.md)

**Flujo simplificado:**
```
Cliente HTTP → Controller → Service → Repository → PostgreSQL
                          ↓
                    WeatherApiClient → OpenWeather API
                          ↓
                    MetricsService → Micrometer
```

---

## Tecnologías

| Componente | Tecnología | Versión |
|-----------|------------|---------|
| Lenguaje | Java | 17 |
| Framework | Spring Boot | 3.4.12 |
| Build Tool | Maven | 3.9+ |
| Base de Datos (Prod) | PostgreSQL | 15+ |
| Base de Datos (Dev) | H2 | 2.3.232 |
| ORM | Hibernate/JPA | 6.6.36 |
| API Docs | Springdoc OpenAPI | 2.7.0 |
| Métricas | Micrometer | 1.14.13 |
| Testing | JUnit 5 + Mockito | 5.11.4 |

---

## Requisitos Previos

- **Java 17** o superior
- **Maven 3.9+**
- **PostgreSQL 15+** (para producción)
- **Cuenta en OpenWeather API** (gratuita): https://openweathermap.org/api

---

## Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/waegarcia/conversational-assistant.git
cd conversational-assistant
```

### 2. Configurar variables de entorno

Crea un archivo `.env` en la raíz del proyecto:
```bash
# OpenWeather API (obligatorio)
WEATHER_API_KEY=tu_api_key_aqui

# PostgreSQL (opcional, solo para profile prod)
DATABASE_URL=jdbc:postgresql://localhost:5432/assistant_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
```

**Obtener API Key de OpenWeather:**
1. Registrarse en https://openweathermap.org/api
2. Ir a "API keys" en el dashboard
3. Copiar la key generada

### 3. Compilar el proyecto
```bash
mvn clean compile
```

---

## Configuración

### Profiles de Spring Boot

El proyecto soporta 3 profiles:

#### 1. Development (default)
```bash
# Usa H2 en memoria, no requiere PostgreSQL
mvn spring-boot:run
```

**Características:**
- Base de datos: H2 en memoria
- Consola H2: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:assistant_db`
- Usuario: `sa` / Password: (vacío)
- DDL: `create-drop` (se borra al cerrar)
- SQL visible en logs

#### 2. Production
```bash
# Requiere PostgreSQL configurado
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Características:**
- Base de datos: PostgreSQL
- DDL: `update` (mantiene esquema)
- SQL no visible en logs

#### 3. Test
```bash
# Usado automáticamente por los tests
mvn test
```

**Características:**
- Base de datos: H2 en memoria (testdb)
- DDL: `create-drop`
- Aislado de otros profiles

---

## Ejecución

### Opción 1: Maven (Desarrollo)
```bash
# Configurar variable de entorno (Windows PowerShell)
$env:WEATHER_API_KEY="tu_api_key"
mvn spring-boot:run

# Configurar variable de entorno (Linux/Mac)
export WEATHER_API_KEY="tu_api_key"
mvn spring-boot:run
```

### Opción 2: IntelliJ IDEA

1. Abrir el proyecto en IntelliJ
2. Ir a `Run → Edit Configurations`
3. Agregar variables de entorno:
    - `WEATHER_API_KEY=tu_api_key`
4. Run `ConversationalAssistantApplication`

### Opción 3: JAR Ejecutable
```bash
# Compilar
mvn clean package

# Ejecutar
java -jar target/conversational-assistant-0.0.1-SNAPSHOT.jar \
  --WEATHER_API_KEY=tu_api_key
```

### Verificación

La aplicación estará disponible en:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Actuator: http://localhost:8080/actuator
- H2 Console (dev): http://localhost:8080/h2-console

---

## API Endpoints

### 1. Enviar Mensaje

**POST** `/api/conversations`

Procesa un mensaje del usuario y retorna la respuesta del asistente.

**Request:**
```json
{
  "sessionId": "uuid-opcional",
  "userId": "user123",
  "message": "Que temperatura hace en Buenos Aires"
}
```

**Response (200 OK):**
```json
{
  "sessionId": "8e4bfba5-d5e3-424f-afd9-436323d36cd9",
  "message": "El clima en Buenos Aires:\nTemperatura: 27.0°C (se siente como 28.5°C)\nCondiciones: cielo claro\nHumedad: 60%\nViento: 3.5 m/s",
  "intent": "WEATHER_QUERY",
  "timestamp": "2025-12-06T15:30:45.123456",
  "externalServiceUsed": "OpenWeather",
  "conversationActive": true
}
```

**Validaciones:**
- `userId`: obligatorio, no vacío
- `message`: obligatorio, no vacío, máximo 2000 caracteres
- `sessionId`: opcional (se genera si no se provee)

---

### 2. Obtener Historial

**GET** `/api/conversations/{sessionId}`

Retorna el historial completo de una conversación.

**Response (200 OK):**
```json
{
  "sessionId": "8e4bfba5-d5e3-424f-afd9-436323d36cd9",
  "userId": "user123",
  "status": "ACTIVE",
  "startedAt": "2025-12-06T15:30:00.000000",
  "endedAt": null,
  "messages": [
    {
      "role": "USER",
      "content": "Hola",
      "intent": null,
      "timestamp": "2025-12-06T15:30:00.000000",
      "externalServiceUsed": null
    },
    {
      "role": "ASSISTANT",
      "content": "Hola! Soy tu asistente virtual...",
      "intent": "GREETING",
      "timestamp": "2025-12-06T15:30:01.000000",
      "externalServiceUsed": null
    }
  ]
}
```

---

### 3. Finalizar Conversación

**DELETE** `/api/conversations/{sessionId}`

Marca una conversación como finalizada.

**Response (204 No Content)**

---

### 4. Métricas Personalizadas

**GET** `/api/metrics/summary`

Retorna resumen de métricas del sistema.

**Response (200 OK):**
```json
{
  "conversations": {
    "totalCreated": 150,
    "currentlyActive": 23
  },
  "messages": {
    "totalProcessed": 487
  },
  "intents": {
    "greeting": 150,
    "weather_query": 230,
    "help": 45,
    "farewell": 50,
    "unknown": 12
  },
  "externalApi": {
    "totalCalls": 230,
    "successes": 225,
    "failures": 5,
    "successRate": 97.8
  },
  "performance": {
    "weather_query": {
      "count": 230,
      "avgMs": 785.3,
      "maxMs": 1523.7
    }
  },
  "errors": {
    "weatherApiErrors": 5
  }
}
```

---

## Ejemplos de Uso

### Ejemplo 1: Saludo
```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "message": "Hola"
  }'
```

**Respuesta:**
```json
{
  "sessionId": "generado-automaticamente",
  "message": "Hola! Soy tu asistente virtual. Puedo ayudarte con informacion del clima. En que ciudad te gustaria consultar?",
  "intent": "GREETING",
  "conversationActive": true
}
```

---

### Ejemplo 2: Consulta de Clima
```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "8e4bfba5-d5e3-424f-afd9-436323d36cd9",
    "userId": "user123",
    "message": "Que temperatura hace en Madrid"
  }'
```

**Respuesta:**
```json
{
  "sessionId": "8e4bfba5-d5e3-424f-afd9-436323d36cd9",
  "message": "El clima en Madrid:\nTemperatura: 15.5°C (se siente como 14.2°C)\nCondiciones: parcialmente nublado\nHumedad: 65%\nViento: 2.3 m/s",
  "intent": "WEATHER_QUERY",
  "externalServiceUsed": "OpenWeather",
  "conversationActive": true
}
```

---

### Ejemplo 3: Ayuda
```bash
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "message": "Ayuda"
  }'
```

---

### Ejemplo 4: Obtener Historial
```bash
curl http://localhost:8080/api/conversations/8e4bfba5-d5e3-424f-afd9-436323d36cd9
```

---

### Ejemplo 5: Finalizar Conversación
```bash
curl -X DELETE http://localhost:8080/api/conversations/8e4bfba5-d5e3-424f-afd9-436323d36cd9
```

---

## Testing

### Ejecutar Tests
```bash
# Todos los tests
mvn test

# Con reporte de cobertura
mvn test jacoco:report
```

### Cobertura de Tests

- **Total de tests:** 31
- **Tasa de éxito:** 100%

**Distribución:**
- IntentProcessorServiceTest: 14 tests
- ConversationServiceTest: 9 tests
- ConversationControllerTest: 7 tests
- Application context: 1 test

**Casos cubiertos:**
- Detección de intenciones válidas
- Edge cases (null, vacío, caracteres especiales)
- Integración con API externa (mocked)
- Validaciones de input
- Manejo de errores
- Códigos HTTP correctos

---

## Métricas y Observabilidad

### Métricas Implementadas

Ver documentación completa en [METRICS.md](METRICS.md)

**Métricas funcionales:**
- Conversaciones creadas
- Conversaciones activas (gauge)
- Mensajes procesados
- Intenciones detectadas (por tipo)
- Llamadas a API externa (success/failure)

**Métricas técnicas:**
- Tiempo de respuesta por intención
- Errores por tipo

### Endpoints de Monitoreo
```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas estándar
curl http://localhost:8080/actuator/metrics

# Métrica específica
curl http://localhost:8080/actuator/metrics/conversations.created

# Métricas custom resumidas
curl http://localhost:8080/api/metrics/summary
```

### Integración con Prometheus
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'conversational-assistant'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

---

## Deployment

### Requisitos de Producción

**Infraestructura mínima:**
- 2GB RAM por instancia
- 2 CPU cores
- PostgreSQL 15+
- Load balancer (para múltiples instancias)

**Variables de entorno obligatorias:**
```bash
WEATHER_API_KEY=xxx
DATABASE_URL=jdbc:postgresql://host:5432/db
DATABASE_USERNAME=user
DATABASE_PASSWORD=pass
```

### Deployment Local con Docker (Pendiente)
```bash
# Construir imagen
docker build -t conversational-assistant .

# Ejecutar contenedor
docker run -p 8080:8080 \
  -e WEATHER_API_KEY=xxx \
  -e DATABASE_URL=jdbc:postgresql://host:5432/db \
  conversational-assistant
```

### Deployment en AWS (Pendiente)

**Arquitectura recomendada:**
- ECS Fargate para contenedores
- RDS PostgreSQL para base de datos
- Application Load Balancer
- CloudWatch para métricas

---

## Decisiones de Diseño

### Arquitectura Hexagonal

**Justificación:**
- Separación de responsabilidades
- Fácil testing con mocks
- Independencia de frameworks
- Preparado para microservicios

**Trade-off:** Mayor cantidad de capas, pero beneficio a largo plazo en mantenibilidad.

---

### Detección de Intenciones con Regex

**Justificación:**
- Suficiente para el alcance actual (5 intenciones)
- Latencia mínima (menor a 1ms)
- Sin dependencias externas pesadas
- Fácil de debuggear

**Evolución futura:** Integrar NLP (DialogFlow, Amazon Lex) cuando se requieran intenciones más complejas.

---

### Base de Datos Relacional (PostgreSQL)

**Justificación:**
- Modelo estructurado (Conversation-Message)
- ACID garantiza consistencia
- SQL más simple para queries complejas

**Alternativa descartada:** NoSQL (útil si el esquema fuera muy variable).

---

### RestTemplate vs WebClient

**Decisión:** RestTemplate (síncrono)

**Justificación:**
- Suficiente para el volumen actual
- Código más simple
- OpenWeather API responde rápido (menor a 500ms)

**Evolución futura:** Migrar a WebClient si se requiere mayor throughput.

---

## Roadmap

### Corto Plazo (1-2 semanas)
- Implementar autenticación JWT
- Agregar caché para OpenWeather API (Redis)
- Circuit breaker para resiliencia (Resilience4j)

### Mediano Plazo (1-2 meses)
- Migrar detección de intenciones a servicio NLP
- WebSocket para conversaciones en tiempo real
- Dashboard de métricas con Grafana

### Largo Plazo (3-6 meses)
- Arquitectura de microservicios distribuidos
- Event-driven con Kafka
- Deployment en Kubernetes

---

## Contribución

Este es un proyecto de challenge técnico. Para consultas o sugerencias, contactar al autor.

---

## Licencia

Proyecto privado - Todos los derechos reservados.

---

## Autor

Walter Garcia - https://github.com/waegarcia

---

## Documentación Adicional

- [Arquitectura Detallada](ARCHITECTURE.md)
- [Justificación de Métricas](METRICS.md)
- [Diagramas de Componentes y Secuencia](ARCHITECTURE.md#diagramas)
