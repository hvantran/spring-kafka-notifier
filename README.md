## Spring Kafka Notifier

### Overview

Spring Kafka Notifier is a REST API application designed to receive dynamic Kafka messages at runtime and trigger notifications to various channels based on configurable message conditions. Currently, it supports Slack notifications via incoming webhooks, with the flexibility to extend to other notification channels in the future. The application exposes REST APIs for configuration management and stores all configuration in MongoDB.

### Features

- **Dynamic Kafka Message Reception:**
   - Listens to Kafka topics and processes messages in real time.
   - Supports runtime configuration for topics and message formats stored in MongoDB.

- **Conditional Slack Notification:**
  - Define conditions (e.g., keywords, field values, patterns) to match incoming messages.
  - When a message meets the specified condition, a Slack notification is sent via incoming webhook to the configured channel.
  - Extensible design allows for future support of additional notification channels.

- **MongoDB Configuration Storage:**
  - All `NotifierConfiguration` documents containing notification rules, Kafka topic subscriptions, and incoming webhook configurations are stored in MongoDB.
  - Allows dynamic updates to configuration without application restart.

- **REST API Management:**
  - Exposes REST APIs to manage `NotifierConfiguration` documents for notification rules, Kafka topic subscriptions, and incoming webhook configurations.
  - Provides endpoints for CRUD operations on configuration data.
  - API documentation available through Swagger/OpenAPI.

### Example Use Case

1. Use the REST API to create a `NotifierConfiguration` for monitoring CPU usage from the `cpu-usage` Kafka topic.
2. Set a rule: If a message value is greater than 80, trigger a Slack notification.
3. The application will evaluate incoming messages against the rules and send notifications via incoming webhook when conditions are met.
4. Monitor and update configurations through the REST API endpoints.

### Getting Started

1. Clone the repository.
2. Configure MongoDB connection settings in `application.yml` or environment variables.
3. Run the Spring Boot application.
4. Use the REST API endpoints to set up Kafka topics and Slack incoming webhook configurations.
5. Create notification rules through the API endpoints.

### Configuration Model

The application uses a `NotifierConfiguration` model to store notification rules in MongoDB. Each configuration defines how to process messages from a specific Kafka topic and what actions to take when conditions are met.

#### NotifierConfiguration Structure

```json
{
  "notifier": "MetricThresholdNotifier",
  "topic": "cpu-usage",
  "rules": {
    "$and": [
      { "$gt": { "$value": 80 } }
    ]
  },
  "actions": [
    {
      "type": "call",
      "params": {
        "provider": "SLACK",
        "webhookURL": "https://hooks.slack.com/services/xxx/yyy/zzz",
        "message": "CPU usage high: ${value}"
      }
    }
  ]
}
```

#### Field Descriptions

- **notifier**: The name/type of the notifier (e.g., "MetricThresholdNotifier")
- **topic**: The Kafka topic to listen to for messages
- **rules**: Conditional logic using MongoDB-like query operators to evaluate message content
- **actions**: Array of actions to execute when rules match, including:
  - **type**: Action type (e.g., "call")
  - **params**: Action parameters including:
    - **provider**: Notification provider ("SLACK")
    - **webhookURL**: Slack incoming webhook URL
    - **message**: Message template with variable substitution (e.g., `${value}`)

### Prerequisites

- Java 21 or higher
- Spring Boot 3.2.0
- MongoDB instance (local or remote)
- Kafka cluster access
- Slack incoming webhook URL

### Architecture

Kafka → Spring Boot REST API → Condition Evaluator → MongoDB Config → Slack Incoming Webhook
                     ↕
              REST API Endpoints

### Contributing

Contributions are welcome! Please open issues or submit pull requests for improvements or new features.

