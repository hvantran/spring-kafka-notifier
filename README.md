# Spring Kafka Notifier

## Overview

Spring Kafka Notifier is a production-ready REST API application that dynamically processes Kafka messages and triggers intelligent notifications based on configurable rules. The system features advanced variable substitution, notification throttling to prevent spam, and flexible per-notifier configuration with sensible defaults.

## 🏗️ Architecture Diagram

## 🏗️ Architecture Overview

```
                           Spring Kafka Notifier Architecture
                           
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│                     │    │                     │    │                     │
│   📨 Kafka Topics   │───▶│  📊 Message         │───▶│  🔍 Rule           │
│                     │    │     Processor       │    │     Evaluator       │
│   (System Metrics)  │    │                     │    │                     │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
                                      │                           │
                                      │                           │
                                      ▼                           ▼
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│                     │    │                     │    │                     │
│ 💬 Slack Webhooks   │◀───│  🚦 Rate Limiter   │◀───│  📢 Notification   │
│                     │    │                     │    │     Service         │
│   (Alerts/Notifs)   │    │  (Anti-Spam)        │    │                     │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘

                                      │
                                      ▼
                           ┌─────────────────────┐
                           │                     │
                           │  💾 MongoDB         │
                           │                     │
                           │  (Configuration)    │
                           └─────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🌐 REST API Layer
├── POST /api/notifier-configurations    (Create alert rules)
├── GET  /api/notifier-configurations    (List all rules)  
├── PUT  /api/notifier-configurations/{id}  (Update rules)
└── DELETE /api/notifier-configurations/{id}  (Delete rules)

⚙️ Configuration Sources
├── MongoDB: Per-notifier settings (rules, actions, custom throttling)
└── resilience4j.yml: Default rate limiting settings
```
## ✨ Key Features

- **📨 Real-time Kafka Processing** - Monitors multiple Kafka topics simultaneously
- **🔍 Smart Rule Engine** - MongoDB-like query operators for flexible condition matching  
- **🚦 Anti-Spam Throttling** - Intelligent rate limiting prevents notification flooding
- **🌐 REST API Management** - Full CRUD operations for notification configurations
- **💾 MongoDB Storage** - Persistent configuration with runtime updates
- **🎨 Variable Substitution** - Dynamic message templates with `${field}` placeholders

## 🎯 Problem Solved

**Before**: Alert spam floods your notifications
```
CPU > 80% → Alert sent ✅ → Alert sent ✅ → Alert sent ✅ → 📱💥 SPAM!
```

**After**: Smart throttling prevents spam
```
CPU > 80% → Alert sent ✅ → Throttled 🛑 → Throttled 🛑 → ⏰ Next alert in 5min
```

## � Quick Start

### Basic Configuration Example
```json
{
  "notifier": "cpu-alert",
  "topic": "system-metrics",
  "rules": {
    "$gt": { "$field": "cpu", "$value": 80 }
  },
  "actions": [{
    "type": "call",
    "params": {
      "provider": "SLACK",
      "webhookURL": "https://hooks.slack.com/services/xxx/yyy/zzz",
      "message": "🚨 CPU Alert: ${cpu}% usage detected!"
    }
  }],
  "enabled": true,
  "throttlePeriodMinutes": 5
}
```

### Prerequisites
- Java 21+ | Maven 3.8+ | MongoDB | Apache Kafka | Slack webhook

### Run the Application
```bash
git clone <repository-url>
cd spring-kafka-notifier
mvn clean install
mvn spring-boot:run
```

### API Endpoints
```http
POST   /api/notifier-configurations     # Create alerts
GET    /api/notifier-configurations     # List alerts  
PUT    /api/notifier-configurations/{id} # Update alerts
DELETE /api/notifier-configurations/{id} # Delete alerts
```

**Swagger UI**: http://localhost:8080/spring-kafka-notifier/swagger-ui.html

## 📋 Rule Engine

### Rule Structure
**For JSON messages**: `{"$gt": {"$field": "cpu", "$value": 80}}`  
**For raw values**: `{"$gt": {"$value": 80}}`

### Supported Operators
| Operator | Description | Example |
|----------|-------------|---------|
| `$gt/$gte` | Greater than (or equal) | `{"$gt": {"$field": "cpu", "$value": 80}}` |
| `$lt/$lte` | Less than (or equal) | `{"$lt": {"$field": "memory", "$value": 90}}` |
| `$eq/$ne` | Equal / Not equal | `{"$eq": {"$field": "status", "$value": "error"}}` |
| `$in/$nin` | In / Not in array | `{"$in": {"$field": "level", "$values": ["error", "critical"]}}` |
| `$regex` | Regular expression | `{"$regex": {"$field": "message", "$value": ".*error.*"}}` |
| `$and/$or` | Logical operators | `{"$and": [{"$gt": {"$field": "cpu", "$value": 80}}, {"$eq": {"$field": "env", "$value": "prod"}}]}` |

### Message Examples
| Input | Rule | Template | Output |
|-------|------|----------|--------|
| `42` | `{"$gt": {"$value": 40}}` | `"Value: ${value}"` | `"Value: 42"` |
| `{"cpu": 85}` | `{"$gt": {"$field": "cpu", "$value": 80}}` | `"CPU: ${cpu}%"` | `"CPU: 85%"` |
| `{"server": {"cpu": 90}}` | `{"$gt": {"$field": "server.cpu", "$value": 85}}` | `"CPU: ${server.cpu}%"` | `"CPU: 90%"` |

## 🛠️ Tech Stack
**Spring Boot 3.2.0** • **Java 21** • **Apache Kafka** • **MongoDB** • **Resilience4j** • **Maven**

## 🧪 Testing
```bash
mvn test                                    # Run all tests
mvn test -Dtest=RuleEvaluationServiceTest  # Run specific tests
```

## 🎯 Use Cases

### System Monitoring
```json
{
  "notifier": "system-monitor",
  "topic": "server-metrics",
  "rules": {
    "$or": [
      {"$gt": {"$field": "cpu", "$value": 85}},
      {"$gt": {"$field": "memory", "$value": 90}}
    ]
  },
  "actions": [{
    "type": "call",
    "params": {
      "provider": "SLACK",
      "webhookURL": "https://hooks.slack.com/services/xxx/yyy/zzz",
      "message": "🚨 System Alert - CPU: ${cpu}%, Memory: ${memory}%"
    }
  }],
  "throttlePeriodMinutes": 10
}
```

### Error Tracking
```json
{
  "notifier": "error-tracker",
  "topic": "application-logs",
  "rules": {
    "$and": [
      {"$in": {"$field": "level", "$values": ["ERROR", "CRITICAL"]}},
      {"$eq": {"$field": "environment", "$value": "production"}}
    ]
  },
  "actions": [{
    "type": "call", 
    "params": {
      "provider": "SLACK",
      "webhookURL": "https://hooks.slack.com/services/xxx/yyy/zzz",
      "message": "❌ ${level} in ${environment}: ${message}"
    }
  }],
  "throttlePeriodMinutes": 2
}
```

## 🔧 Configuration Model Reference

### NotifierConfiguration Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `notifier` | String | ✅ | Unique notifier identifier |
| `topic` | String | ✅ | Kafka topic to monitor |
| `rules` | Object | ✅ | Condition rules (MongoDB-like syntax) |
| `actions` | Array | ✅ | Actions to execute when rules match |
| `enabled` | Boolean | ❌ | Enable/disable notifier (default: true) |
| `description` | String | ❌ | Human-readable description |
| `throttlePeriodMinutes` | Long | ❌ | Custom throttling period (null = use default) |
| `throttlePermitsPerPeriod` | Integer | ❌ | Custom permit count (null = use default) |

### Action Configuration

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | ✅ | Action type ("call") |
| `params.provider` | String | ✅ | Notification provider ("SLACK") |
| `params.webhookURL` | String | ✅ | Slack webhook URL |
| `params.message` | String | ✅ | Message template with `${field}` placeholders |

## � Production Deployment

### Docker (Recommended)
```dockerfile
FROM openjdk:21-jre-slim
COPY target/spring-kafka-notifier-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables
```bash
# Database
MONGODB_URI=mongodb://mongodb:27017/notifier_db

# Kafka
KAFKA_SERVERS=kafka:9092
KAFKA_GROUP_ID=notifier-production

# Application
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
```

### Health Check
```bash
curl http://localhost:8080/spring-kafka-notifier/actuator/health
```

## 🤝 Contributing
1. Fork repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push branch: `git push origin feature/amazing-feature`
5. Open Pull Request

## � License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

