+<a id="readme-top"></a>

<!-- README HEADER -->
<div align="center"> 
  <a href="https://github.com/Nelyskov/incident-projec">
    <img src="images/rofrn.jpg" alt="Logo" width="80" height="80">
  </a>
  
  <h3 align="center">Incident Managment Project</h3>
  <b>IMP</b> - система микросервисов для управления инцидентами. <br/>
  Позволяет создавать, обновлять и отслеживать инциденты с дальнейшей эскалацией HIGH Priority через цепочку сервисов.
  
</div>


### Built With

* Java 21
* Spring Boot 4.0.2
* Gradle
* Docker
* Avro Schemas

### Features 
* Создание, обновление и управление инцидентами
* Автоматическая проверка HIGH priority и отправка уведомление
* Определение ответственных групп по устранению инцидентов
* Распределенная система общения через Apache Kafka
* Отправка email по Google SMTP
* Проверка статусов сервисов
* Агрегация логов Promtail + Loki
* Визуализация метрик Prometheus + Grafana

### Architecture 
Система микросервисов построена по прицнипе Event-Driven Architecture - слабая связанность и высокая отказоустойчивость
<div align="center"> 
    <img src="images/incident_management_uml_v2.svg" alt="Architecture schema" width="600" height="1100">
</div>

### Getting Started 
_Пошаговая инструкция, как запустить и пользоваться проектом._

1. Необходимо установить любую IDE для редактирования и просмотра кода, Docker для поднятия сборки, и любой инструмент для тестирования API (Например, Postman)
2. Клонировать репозиторий
   ```sh
   git clone https://github.com/Nelyskov/incident-project.git
   ```
3. Открыть корневую папку с проектом
   ```sh
   cd incident-project
   ```
4. Собрать билды сервисов
   ```js
   ./gradlew :incedent-service:bootJar
   ```
   ```js
   ./gradlew :incedent-producer-service:bootJar
   ```
   ```js
   ./gradlew :incedent-processor:bootJar
   ```
   ```js
   ./gradlew :alert-service:bootJar
   ```
   ```js
   ./gradlew :ping-service:bootJar
   ```
5. Открыть папку с необходимой инфраструктурой (PostgresSQL, Kafka, Prometheus, Promtail, Loki, Grafana) и поднять контейнеры в Docker
   ```sh
   cd infra
   docker compose up -d
   ```
6. Открыть папку с необходимой инфраструктурой (PostgresSQL, Kafka, Prometheus, Promtail, Loki, Grafana) и поднять контейнеры в Docker
   ```sh
   cd infra
   docker compose up -d
   ```
7. Поочередно поднимать сервисы (Alert Service поднимать позже PostgreSQL). --build используется для того, чтобы по каким-либо причинам Docker использовал свежий образ
   ```sh
   cd incident-project\incedent-service
   docker compose up -d --build
   ```
   ```sh
   cd ..\incedent-producer-service
   docker compose up -d --build
   ```
   ```sh
   cd ..\incedent-processor
   docker compose up -d --build
   ```
   ```sh
   cd ..\alert-service
   docker compose up -d --build
   ```
   ```sh
   cd ..\ping-service
   docker compose up -d --build
   ```
<p align="right">(<a href="#readme-top">back to top</a>)</p>

### API Reference
Основные ednpoints (примеры запроса будут в виде CURL)
1. Создание инцидента<br/>
   1.1 Request
   ```sh
   postman request POST 'http://localhost:8082/api/incident-producer-service/create' \
      --header 'Content-Type: application/json' \
      --body '{
            "service":"Payment Service",
            "info":"This service has timeout 30 seconds and response 500 HTTP code",
            "priority":"HIGH"
      }'
    ```

    1.2 Response
   ```sh
    {
        "service": "Payment Service",
        "time": 1775746147873,
        "message": "Инцидент успешно создан",
        "incidentId": 37,
        "priority": "HIGH",
        "info": "This service has timeout 30 seconds and response 500 HTTP code",
        "status": "CREATED"
    }

2. Поиск инцидента в БД <br/>
   2.1 Request
   ```sh
      postman request 'http://localhost:8082/api/incident-producer-service/37'
    ```

    2.2 Response
   ```sh
    {
      "service": "Payment Service",
      "id": 37,
      "priority": "HIGH",
      "info": "This service has timeout 30 seconds and response 500 HTTP code",
      "status": "CREATED",
      "timestamp": 1775746147873000
    }
3. Проверка статусов сервисов <br/>
   3.1 Request
   ```sh
   postman request 'http://localhost:9091/ping'
   ```
   
   3.2 Response
   ```sh
   {
      "incident-producer-service": "OK",
      "alert-service": "OK",
      "incident-service": "OK",
      "incident-processor-service": "OK"
   }

### Project Structure
Модуль | Порт | Описание
--- | --- | ---
Common | - | Является зависимостью, а не сервисов. Модуль с Avro schemas
incedent-service | 8079 | Слушает капка Kafka топики, работает с бд, возвращает ответ
incedent-producer-service | 8082 | Принимает REST запросы от пользователей и отправляет ответы
incedent-processor | 8086 | Обрабатывает HIGH priority инциденты, определяет ответственную группу, создает Alert
alert-service | 8085 | Слушает alert топики, отправляет email ответственным по SMTP
ping-service | 9091 | Мониторит сервисы через feign клиент
infra | - | Инфраструктура: Kafka, Promtail, Loki, Prometheus, Grafana, PostgreSQL
