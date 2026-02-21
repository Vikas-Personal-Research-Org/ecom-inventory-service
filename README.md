# ecom-inventory-service

Stock and inventory management microservice for the e-commerce platform. Handles stock levels, reservations, releases, and tracks inventory events.

## Tech Stack

- Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0
- Spring Data JPA with H2 (in-memory)
- Eureka Client for service discovery
- SpringDoc OpenAPI for API documentation

## Port

**8082**

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/inventory` | Get all inventory records |
| GET | `/api/inventory/{productId}` | Get inventory for a product |
| POST | `/api/inventory` | Create inventory record |
| PUT | `/api/inventory/{productId}` | Update inventory stock |
| POST | `/api/inventory/reserve` | Reserve stock for an order |
| POST | `/api/inventory/release` | Release reserved stock |
| GET | `/api/inventory/low-stock?threshold=X` | Get low stock items |
| GET | `/api/inventory/events/{productId}` | Get inventory events for product |

## Build and Run

```bash
mvn clean package
java -jar target/ecom-inventory-service-0.0.1-SNAPSHOT.jar
```

## Access Points

- Swagger UI: http://localhost:8082/swagger-ui.html
- H2 Console: http://localhost:8082/h2-console
- Actuator Health: http://localhost:8082/actuator/health
