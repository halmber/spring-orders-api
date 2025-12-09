# Spring Orders API

A RESTful API for managing customers and orders with advanced features including pagination, filtering, bulk import, and
report generation to files.

## Features

- ✅ **CRUD Operations** for Customers and Orders
- ✅ **Pagination & Sorting** with custom validation
- ✅ **Advanced Filtering** for orders
- ✅ **Bulk Import** from JSON files using streaming parser for large files
- ✅ **Report Generation** in CSV and XLSX formats using memory-efficient streaming
- ✅ **Custom Pageable Validation** with whitelist/blacklist support
- ✅ **Bean Validation** for all DTOs
- ✅ **Proper HTTP Status Codes** and error handling

## Tech Stack

- **Java 21+**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **PostgreSQL** and **H2** for testing
- **MapStruct** for DTO mapping
- **Apache POI** for Excel generation
- **Jackson** for JSON processing
- **Lombok** for boilerplate reduction

## Custom Pageable Validation

This API implements a custom `@PageableConstraints` annotation for secure and controlled pagination and sorting.

### PageableConstraints Annotation

The annotation provides three validation modes:

1. **Whitelist Mode** - Only specified fields can be used for sorting
2. **Blacklist Mode** - All fields except specified ones can be used for sorting
3. **No Sorting Mode** - If both whitelist and blacklist are empty, sorting is completely disabled

#### Implementation

```java

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PageableConstraints {
    String[] whitelist() default {};

    String[] blacklist() default {};
}
```

#### Usage Examples

**Whitelist Mode:**

```java

@GetMapping("/api/customers")
public CustomerListResponseDto getPageableList(
        @PageableConstraints(whitelist = {"firstName", "lastName", "city"})
        @PageableDefault(size = 5) Pageable pageable) {
    return customerService.listCustomers(pageable);
}
```

**Blacklist Mode:**

```java

@GetMapping("/api/orders")
public OrderListResponseDto getPageableList(
        @PageableConstraints(blacklist = {"password", "secretField"})
        @PageableDefault(size = 5) Pageable pageable) {
    return orderService.listOrders(pageable);
}
```

**No Sorting Allowed:**

```java

@GetMapping("/api/sensitive")
public ResponseDto getSensitiveData(
        @PageableConstraints() // Both empty = no sorting allowed
        @PageableDefault(size = 10) Pageable pageable) {
    return service.list(pageable);
}
```

### Validation Rules

The `PageableConstraintResolver` validates:

- **Page**: Must be >= 0
- **Size**: Must be >= 0
- **Sort fields**: Must comply with whitelist/blacklist rules

Invalid requests throw `InvalidRequestParameterException` with descriptive error messages.

---

## API Endpoints

### Customer Endpoints

#### 1. Get Paginated Customers List

```http
GET /api/customers?page=0&size=10&sort=firstName,asc&sort=city,desc
```

**Query Parameters:**

- `page` (optional, default: 0) - Page number
- `size` (optional, default: 5) - Page size
- `sort` (optional) - Sorting field and direction, can retrieve multiple sorting fields

**Allowed Sort Fields:** `firstName`, `lastName`, `city`

**Response:**

```json
{
  "customers": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "phone": "+380501234567",
      "city": "Kyiv"
    }
  ],
  "totalPages": 5
}
```

---

#### 2. Get Customer by ID

```http
GET /api/customers/{id}
```

**Path Parameters:**

- `id` (UUID) - Customer ID

**Response:**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+380501234567",
  "city": "Kyiv"
}
```

**Error Responses:**

- `404 Not Found` - Customer doesn't exist

---

#### 3. Create Customer

```http
POST /api/customers
Content-Type: application/json
```

**Request Body:**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+380501234567",
  "city": "Kyiv"
}
```

**Validation Rules:**

- `firstName`: required, 3-100 characters
- `lastName`: required, 3-100 characters
- `email`: required, valid email format, must be unique
- `phone`: required, 3-50 characters
- `city`: required, 3-255 characters

**Response:** `201 Created`

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+380501234567",
  "city": "Kyiv"
}
```

**Error Responses:**

- `400 Bad Request` - Validation errors or email already exists

---

#### 4. Update Customer

```http
PUT /api/customers/{id}
Content-Type: application/json
```

**Path Parameters:**

- `id` (UUID) - Customer ID

**Request Body:**

```json
{
  "firstName": "John Updated",
  "lastName": "Doe",
  "phone": "+380501234999",
  "city": "Lviv"
}
```

**Note:** Email cannot be updated through this endpoint.

**Response:** `200 OK`

**Error Responses:**

- `404 Not Found` - Customer doesn't exist
- `400 Bad Request` - Validation errors

---

#### 5. Delete Customer

```http
DELETE /api/customers/{id}
```

**Path Parameters:**

- `id` (UUID) - Customer ID

**Response:** `200 OK`

```json
{
  "status": 200,
  "message": "Customer with id '123e4567-e89b-12d3-a456-426614174000' was deleted."
}
```

**Note:** This will cascade delete all associated orders.

**Error Responses:**

- `404 Not Found` - Customer doesn't exist

---

### Order Endpoints

#### 1. Get Paginated Orders List

```http
GET /api/orders?page=0&size=10&sort=amount,desc
```

**Query Parameters:**

- `page` (optional, default: 0) - Page number
- `size` (optional, default: 5) - Page size
- `sort` (optional) - Sorting field and direction

**Allowed Sort Fields:** `status`, `paymentMethod`, `amount`

**Response:**

```json
{
  "orders": [
    {
      "id": "456e7890-e89b-12d3-a456-426614174000",
      "amount": 100.50,
      "status": "NEW",
      "paymentMethod": "CARD",
      "createdAt": "2025-12-08T10:30:00Z",
      "customer": {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "firstName": "John",
        "lastName": "Doe",
        "email": "john.doe@example.com",
        "phone": "+380501234567",
        "city": "Kyiv"
      }
    }
  ],
  "totalPages": 10
}
```

---

#### 2. Get Order by ID

```http
GET /api/orders/{id}
```

**Path Parameters:**

- `id` (UUID) - Order ID

**Response:**

```json
{
  "id": "456e7890-e89b-12d3-a456-426614174000",
  "amount": 100.50,
  "status": "NEW",
  "paymentMethod": "CARD",
  "createdAt": "2025-12-08T10:30:00Z",
  "customer": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+380501234567",
    "city": "Kyiv"
  }
}
```

**Error Responses:**

- `404 Not Found` - Order doesn't exist

---

#### 3. Create Order

```http
POST /api/orders
Content-Type: application/json
```

**Request Body:**

```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 100.50,
  "status": "NEW",
  "paymentMethod": "CARD"
}
```

**Validation Rules:**

- `customerId`: required, valid UUID format, customer must exist
- `amount`: required, must be positive
- `status`: required, valid values: `NEW`, `PROCESSING`, `DONE`, `CANCELED`
- `paymentMethod`: required, valid values: `CARD`, `CASH`, `PAYPAL`, `GOOGLE_PAY`, `APPLE_PAY`

**Response:** `201 Created`

**Error Responses:**

- `404 Not Found` - Customer doesn't exist
- `400 Bad Request` - Validation errors or invalid enum values

---

#### 4. Update Order

```http
PUT /api/orders/{id}
Content-Type: application/json
```

**Path Parameters:**

- `id` (UUID) - Order ID

**Request Body:**

```json
{
  "amount": 150.75,
  "status": "PROCESSING",
  "paymentMethod": "PAYPAL"
}
```

**Note:** Customer assignment cannot be changed through this endpoint.

**Response:** `200 OK`

**Error Responses:**

- `404 Not Found` - Order doesn't exist
- `400 Bad Request` - Validation errors

---

#### 5. Get Filtered Orders List

```http
POST /api/orders/_list
Content-Type: application/json
```

**Request Body:**

```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "NEW",
  "paymentMethod": "CARD",
  "page": 0,
  "size": 10
}
```

**Important Notes:**

- **Pagination parameters (`page` and `size`) are passed in the request body**, not as query parameters
- All filter parameters are optional
- Returns orders in **short format** (reduced customer information)

**Request Body Parameters:**

- `customerId` (optional) - Filter by specific customer (valid UUID)
- `status` (optional) - Filter by order status
- `paymentMethod` (optional) - Filter by payment method
- `page` (optional, default: 0, min: 0) - Page number
- `size` (optional, default: 5, min: 1, max: 100) - Page size

**Response:**

```json
{
  "orders": [
    {
      "id": "456e7890-e89b-12d3-a456-426614174000",
      "amount": 100.50,
      "status": "NEW",
      "paymentMethod": "CARD",
      "createdAt": "2025-12-08T10:30:00Z",
      "customer": {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "fullName": "John Doe",
        "email": "john.doe@example.com"
      }
    }
  ],
  "totalPages": 3
}
```

**Error Responses:**

- `404 Not Found` - Specified customer doesn't exist
- `400 Bad Request` - Validation errors

---

#### 6. Generate Report

```http
POST /api/orders/_report
Content-Type: application/json
```

**Request Body:**

```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "DONE",
  "paymentMethod": "CARD",
  "fileType": "xlsx"
}
```

**Request Body Parameters:**

- `customerId` (optional) - Filter by specific customer (valid UUID)
- `status` (optional) - Filter by order status
- `paymentMethod` (optional) - Filter by payment method
- `fileType` (optional, default: "csv") - Output format: `csv` or `xlsx`

**Response:** Binary file download

**Response Headers:**

```
Content-Type: text/csv
  or
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

Content-Disposition: attachment; filename="orders_report_20251208_103000.csv"
Cache-Control: no-cache, no-store, must-revalidate
```

**Report Columns:**

1. Order ID
2. Customer ID
3. Customer Name
4. Email
5. Amount
6. Status
7. Payment Method
8. Created At

**Features:**

- Memory-efficient streaming (handles large datasets)
- CSV format: proper escaping for commas, quotes, newlines
- XLSX format: styled headers, bordered cells, numeric formatting

---

#### 7. Import Orders from JSON

```http
POST /api/orders/upload
Content-Type: multipart/form-data
```

You can use prepared file that contains orders records with `customerId`'s. Liquibase creates some customers
with certain id's. So after running the application you can use for `POST /api/orders/upload` json from
`src/main/resources/jsonFiles/sampleOrdersImport.json`.

**Request:**

- Form field: `file` (JSON file)

**File Requirements:**

- Format: JSON (.json extension)
- Maximum size: 10MB
- Root element: array

**Expected JSON Format:**

```json
[
  {
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 100.50,
    "status": "NEW",
    "paymentMethod": "CARD"
  },
  {
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 250.00,
    "status": "PROCESSING",
    "paymentMethod": "PAYPAL"
  }
]
```

**Validation per Order:**

- `customerId`: required, valid UUID, customer must exist
- `amount`: required, must be positive
- `status`: required, valid enum
- `paymentMethod`: required, valid enum

**Response:** `200 OK`

```json
{
  "totalRecords": 100,
  "successfulImports": 95,
  "failedImports": 5,
  "errors": [
    {
      "lineNumber": 23,
      "reason": "Customer not found",
      "details": "No customer with ID: 999e4567-e89b-12d3-a456-426614174000"
    },
    {
      "lineNumber": 45,
      "reason": "Invalid amount",
      "details": "Amount must be positive, got: -10.00"
    }
  ]
}
```

**Features:**

- Streaming JSON parser (memory-efficient)
- Batch processing (50 records per batch)
- Detailed error reporting with line numbers
- Partial success (valid records are saved even if some fail)

**Error Responses:**

- `400 Bad Request` - File validation errors (empty, wrong format, too large)

---

#### 8. Delete Order

```http
DELETE /api/orders/{id}
```

**Path Parameters:**

- `id` (UUID) - Order ID

**Response:** `200 OK`

```json
{
  "status": 200,
  "message": "Order with id '456e7890-e89b-12d3-a456-426614174000' was deleted."
}
```

**Error Responses:**

- `404 Not Found` - Order doesn't exist

---

## Error Responses

All endpoints return consistent error responses:

### Validation Error (400 Bad Request)

```json
{
  "timestamp": "2025-12-08T10:30:00Z",
  "status": 400,
  "message": "Validation failed"
}
```

### Not Found Error (404)

```json
{
  "timestamp": "2025-12-08T10:30:00Z",
  "status": 404,
  "message": "Customer with id '123e4567-e89b-12d3-a456-426614174000' not found"
}
```

### Invalid Pageable Parameter (400)

```json
{
  "timestamp": "2025-12-08T10:30:00Z",
  "status": 400,
  "message": "Sorting by field 'city' is not allowed. Allowed fields: [status, paymentMethod, amount]"
}
```

---

## Database Schema

### Customer Entity

```sql
CREATE TABLE customers
(
    id         UUID PRIMARY KEY,
    first_name VARCHAR(100)        NOT NULL,
    last_name  VARCHAR(100)        NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    phone      VARCHAR(50),
    city       VARCHAR(255),
    created_at TIMESTAMP           NOT NULL,
    updated_at TIMESTAMP           NOT NULL
);
```

### Order Entity

```sql
CREATE TABLE orders
(
    id             UUID PRIMARY KEY,
    customer_id    UUID           NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    amount         DECIMAL(10, 2) NOT NULL,
    status         VARCHAR(50)    NOT NULL,
    payment_method VARCHAR(50),
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);
```

---

## Running the Application

### Prerequisites

- Java 21 or higher
- PostgreSQL (H2 for testing)
- Maven
- Docker and Docker Compose (recommended) or PostgreSQL installed locally

### Option 1: Running with Docker Compose (Recommended)

This project uses **Liquibase** for database migrations and seed data, which is why `spring.jpa.hibernate.ddl-auto` is
set to `none`. The database schema is managed entirely by Liquibase, not Hibernate.

**1. Start the Database Container**

```bash
docker-compose up -d
```

This will:

- Start PostgreSQL on port `5433` (host) → `5432` (container)
- Create the database with credentials from `docker-compose.yml`

**2. Verify Container is Running**

```bash
docker-compose ps
```

**3. Run the Application**

```bash
mvn spring-boot:run
```

Liquibase will automatically:

- Create database schema
- Insert seed data
- Apply any pending migrations

### Option 2: Running with Local PostgreSQL

If you prefer to use a local PostgreSQL installation:

**1. Create Database**

```sql
CREATE
DATABASE orders;
```

**2. Update Configuration**

Modify `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/orders
```

**Note:**

- Change port from `5433` (Docker) to `5432` (local PostgreSQL)
- Liquibase will handle all migrations and seed data.

**3. Run the Application**

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Integration Tests Only

```bash
mvn test -Dtest=*IntegrationTest
```

### Test Coverage

The project includes comprehensive integration tests for:

- All controller endpoints
- CSV report generation
- XLSX report generation
- Pagination and filtering
- Validation scenarios
- Error handling

---

## API Documentation

Once the application is running, you can access:

- **Swagger UI**: `http://localhost:8080/swagger-ui`

---

## Performance Considerations

### Memory Efficiency

- **Report Generation** uses streaming queries (Hibernate) and streaming writers (POI SXSSFWorkbook) to handle millions
  of records without OutOfMemoryError
- **File uploading** uses Jackson streaming parser to process large JSON files (up to 10MB like in configuration, you
  can change that value) without loading entire file
  into memory
- **Batch Processing** import processes records in batches of 50 for optimal database performance

### Database Optimization

- Proper indexes on foreign keys and frequently queried fields
- `@EntityGraph` for efficient eager loading and avoiding N+1 queries
- Read-only transactions for query operations
- Query hints for streaming (fetch size, read-only)

---

## License

This project is licensed under the MIT License.

---

## Author

**Halmber**

---