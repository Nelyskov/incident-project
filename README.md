<a id="readme-top"></a>

<!-- README HEADER -->
<div align="center"> 
  <a href="https://github.com/Nelyskov/incident-project">
    <img src="images/rofrn.jpg" alt="Logo" width="80" height="80">
  </a>
  
  <h3 align="center">Incident Management Project</h3>
  <b>IMP</b> - система микросервисов для управления инцидентами.<br/>
  Позволяет создавать, обновлять и отслеживать инциденты с автоматической эскалацией HIGH Priority через цепочку сервисов.
</div>

---

### Built With

* Java 21
* Spring Boot 4.0.2
* Gradle
* Apache Kafka + Schema Registry
* Apache Avro
* PostgreSQL
* Docker + Docker Compose
* Prometheus + Grafana
* Loki + Promtail
* Spring Cloud OpenFeign

---

### Features

* Создание, обновление и управление инцидентами через REST API
* Автоматическая проверка HIGH priority и отправка уведомлений
* Определение ответственных групп по устранению инцидентов
* Асинхронное взаимодействие сервисов через Apache Kafka (Event-Driven Architecture)
* Отправка email уведомлений через Google SMTP
* Проверка доступности сервисов через Feign клиент
* Агрегация логов через Promtail + Loki
* Визуализация метрик и логов через Prometheus + Grafana

---

### Architecture

Система микросервисов построена по принципу Event-Driven Architecture - слабая связанность и высокая отказоустойчивость. Сервисы общаются через Kafka топики асинхронно. Единственная синхронная точка входа - REST API через `incedent-producer-service`

<div align="center"> 
    <img src="images/incident_management_uml_v2.svg" alt="Architecture schema" width="600" height="1100">
</div>

---

### Prerequisites

Перед запуском убедитесь, что установлено:

* [Java 21](https://adoptium.net/)
* [Docker Desktop](https://www.docker.com/products/docker-desktop/)
* [Git](https://git-scm.com/)
* Инструмент для тестирования API - например [Postman](https://www.postman.com/) или curl

---

### Getting Started

_Инструкция как запустить проект._

**1. Клонировать репозиторий**

```sh
git clone https://github.com/Nelyskov/incident-project.git
cd incident-project
```

**2. Собрать JAR файлы всех сервисов**

```sh
./gradlew :incedent-service:bootJar
./gradlew :incedent-producer-service:bootJar
./gradlew :incedent-processor:bootJar
./gradlew :alert-service:bootJar
./gradlew :ping-service:bootJar
```

**3. Запустить инфраструктуру** (PostgreSQL, Kafka, Prometheus, Promtail, Loki, Grafana)

```sh
cd infra
docker compose up -d
```

Дождитесь пока все контейнеры перейдут в статус `healthy`. Это можно проверить командой:

```sh
docker ps
```

**4. Поочерёдно запустить сервисы**

> Флаг `--build` гарантирует что Docker использует свежий образ из только что собранного JAR.
> Сервисы запускать только после того как инфраструктура полностью готова.

```sh
cd incedent-service && docker compose up -d --build && cd ..
cd incedent-producer-service && docker compose up -d --build && cd ..
cd incedent-processor && docker compose up -d --build && cd ..
cd alert-service && docker compose up -d --build && cd ..
cd ping-service && docker compose up -d --build && cd ..
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

### API Reference

Основные endpoints (примеры в виде curl).

**1. Создание инцидента**

Request:
```sh
curl -X POST 'http://localhost:8082/api/incident-producer-service/create' \
  -H 'Content-Type: application/json' \
  -d '{
    "service": "Payment Service",
    "info": "This service has timeout 30 seconds and response 500 HTTP code",
    "priority": "HIGH"
  }'
```

Response:
```json
{
  "service": "Payment Service",
  "time": 1775746147873,
  "message": "Инцидент успешно создан",
  "incidentId": 37,
  "priority": "HIGH",
  "info": "This service has timeout 30 seconds and response 500 HTTP code",
  "status": "CREATED"
}
```

---

**2. Обновление инцидента**

Request:
```sh
curl -X PUT 'http://localhost:8082/api/incident-producer-service/update' \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 37,
    "status": "PROCESSING"
  }'
```

Response:
```json
{
  "incidentId": 37,
  "service": "Payment Service",
  "status": "PROCESSING",
  "priority": "HIGH",
  "updatedAt": 1775746200000
}
```

---

**3. Поиск инцидента по ID**

Request:
```sh
curl 'http://localhost:8082/api/incident-producer-service/37'
```

Response:
```json
{
  "service": "Payment Service",
  "id": 37,
  "priority": "HIGH",
  "info": "This service has timeout 30 seconds and response 500 HTTP code",
  "status": "CREATED",
  "timestamp": 1775746147873000
}
```

---

**4. Получить все инциденты**

Request:
```sh
curl 'http://localhost:8082/api/incident-producer-service'
```

Response:
```json
{
  "incidents": [
    {
      "id": 37,
      "service": "Payment Service",
      "priority": "HIGH",
      "status": "CREATED",
      "timestamp": 1775746147873000
    }
  ]
}
```

---

**5. Проверка статусов сервисов**

Request:
```sh
curl 'http://localhost:9091/ping'
```

Response:
```json
{
  "incident-producer-service": "OK",
  "alert-service": "OK",
  "incident-service": "OK",
  "incident-processor-service": "OK"
}
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

---

### Project Structure

| Модуль | Порт | Описание |
| --- | --- | --- |
| common | — | Библиотека-зависимость, не запускается как сервис. Содержит Avro схемы для Kafka событий |
| incedent-service | 8079 | Слушает Kafka топики, сохраняет инциденты в PostgreSQL, отправляет ответы |
| incedent-producer-service | 8082 | Принимает REST запросы, отправляет события в Kafka, возвращает ответ клиенту |
| incedent-processor | 8086 | Обрабатывает HIGH priority инциденты, определяет ответственную группу, создаёт Alert |
| alert-service | 8085 | Слушает alert топик, отправляет email ответственной команде через SMTP |
| ping-service | 9091 | Проверяет доступность всех сервисов через Feign клиенты |
| infra | — | Инфраструктура: Kafka, PostgreSQL, Prometheus, Promtail, Loki, Grafana |

---

### Monitoring

| Сервис | URL | Описание |
| --- | --- | --- |
| Prometheus | http://localhost:9090 | Метрики всех сервисов |
| Grafana | http://localhost:3000 | Дашборды (login: admin / admin) |
| Loki | http://localhost:3100 | Хранилище логов |

<p align="right">(<a href="#readme-top">back to top</a>)</p>
# guesser_app
