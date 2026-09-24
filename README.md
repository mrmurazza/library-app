# Library-App

A single Spring Boot service for a library catalog, its members, and their loans. Borrowing limits and loan length live in configuration. PostgreSQL stores the books, members, and loans.

## How to Run

Prerequisites: JDK 17 or newer, and PostgreSQL listening on `localhost:5432`.

`application.yml` connects to database `library_app` as user `root` with an empty password. Create that database if it does not exist.

`docker-compose.yml` starts Postgres 16 with database, user, and password `library_app`. To use that container, point `spring.datasource` at user `library_app` and password `library_app`.

```bash
./mvnw spring-boot:run
```

The Maven plugin will loads a small catalog and two members from seeder (`razza@razza.com`, `rafif@rafif.com`). Those rows are different with login accounts.

Swagger UI is at <http://localhost:8080/swagger-ui.html>.
Health is at <http://localhost:8080/actuator/health>.
Prometheus metrics are at <http://localhost:8080/actuator/prometheus>.

## Login Credentials

As stated in the task description, accounts for authentication use predefined accounts. So, we put the static users in application.yml under `app.authentication.users`. The authentication uses Basic Authentication that have to be sent in the headers in every requests.

### List of Accounts

| Username          | Password       | Role                                                                                                        |
| ----------------- | -------------- | ----------------------------------------------------------------------------------------------------------- |
| `admin@admin.com` | `admin`        | Admin. Manages books and members, and can list every loan.                                                  |
| `razza@razza.com` | `member_razza` | Member. The username must match the member email. Can list books, and can borrow or return only as herself. |
| `rafif@rafif.com` | `member_rafif` | Member, same restriction as Razza.                                                                          |

### Example of Request cURL

- Using -u flag (username:password)

```
curl -u admin@admin.com:admin http://localhost:8080/api/books
```

- Using explicit Authorization header (base64 of "admin@admin.com:admin" is "YWRtaW5AYWRtaW4uY29tOmFkbWlu")

```
curl -H "Authorization: Basic YWRtaW5AYWRtaW4uY29tOmFkbWlu" http://localhost:8080/api/books
```

- A member borrows as herself. The body is the book id:

```bash
curl -u razza@razza.com:PASSWORD -H 'Content-Type: application/json' \
  -d '{"book_id":1}' \
  http://localhost:8080/api/loans
```

## Borrowing rules

As requested from the task description, there are 2 rules that is configurable for borrowing. So we put these 2 in `src/main/resources/application.yml` as well.

```yaml
library:
  max-active-loans: 3
  loan-duration-days: 14
```

`LoanService` will reads those values. Active loan is a loan that is not yet return, thus, return time is still `null`. An overdue loan is an active loan whose due date is already past.
A member at with active loan more than `max-active-loans`, or with any overdue loan, cannot borrow. The due date is the borrow time plus `loan-duration-days`. Changing either number is a config change and need to restart the app before the changes will be applied to the running code.

## Project structure

Code under `src/main/java/com/lexhive/libraryapp` is structured by its layer.

| Package          | Role                                                                                                                                  |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `authentication` | Holds static users, and auth config that is used by Spring Security                                                                   |
| `controller`     | HTTP endpoints for books, members, and loans.                                                                                         |
| `service`        | Contains business logic, coordinating between controllers and repositories.                                                           |
| `repository`     | JPA access to PostgreSQL; can contain both prebuilt (Spring Data JPA) and custom repositories.                                        |
| `entity`         | Persistence entities for tables such as `Book`, `Member`, and `Loan`.                                                                 |
| `dto`            | Data transfer objects for request and response JSON.                                                                                  |
| `config`         | Holds record that derived from properties from application.yml and other application configuration. ( Borrowing limits, OpenAPI, etc) |
| `exception`      | Contains Error codes, the JSON error response body, and logic to handle exceptions                                                    |

A request enters a controller, the service applies the rules, and the repository reads and writes the entities.

## Schema

Flyway applies `src/main/resources/db/migration/V1__init.sql`.

```mermaid
erDiagram
    members ||--o{ loans : borrows
    books ||--o{ loans : "is borrowed as"

    books {
        bigint id PK
        varchar title
        varchar author
        varchar isbn UK
        int total_copies
        int available_copies
    }
    members {
        bigint id PK
        varchar name
        varchar email UK
    }
    loans {
        bigint id PK
        bigint book_id FK
        bigint member_id FK
        timestamptz borrowed_at
        timestamptz due_date
        timestamptz returned_at
    }
```

A member has many loans. A book has many loans. Each loan belongs to one member and one book.

### books

| Column             | Type           | Constraints                             |
| ------------------ | -------------- | --------------------------------------- |
| `id`               | `bigint`       | Primary key                             |
| `title`            | `varchar(255)` | Not null                                |
| `author`           | `varchar(255)` | Not null                                |
| `isbn`             | `varchar(32)`  | Not null, unique                        |
| `total_copies`     | `integer`      | Not null, `>= 0`                        |
| `available_copies` | `integer`      | Not null, `>= 0`, and `<= total_copies` |

### members

| Column  | Type           | Constraints      |
| ------- | -------------- | ---------------- |
| `id`    | `bigint`       | Primary key      |
| `name`  | `varchar(255)` | Not null         |
| `email` | `varchar(255)` | Not null, unique |

### loans

| Column        | Type          | Constraints                                   |
| ------------- | ------------- | --------------------------------------------- |
| `id`          | `bigint`      | Primary key                                   |
| `book_id`     | `bigint`      | Not null, references `books.id`               |
| `member_id`   | `bigint`      | Not null, references `members.id`             |
| `borrowed_at` | `timestamptz` | Not null                                      |
| `due_date`    | `timestamptz` | Not null                                      |
| `returned_at` | `timestamptz` | Null while active. When set, `>= borrowed_at` |

Indexes: `loans (member_id, returned_at)` and `loans (due_date)`.

## Tests

```bash
./mvnw test
```

These are integration tests. Each controller test will starts the application and tries to write actual rows to PostgreSQL. There is no in-memory stand-in for the database. This will require to have separate DB `library_app_test` on localhost:5432 as user `root`. Create it once with `createdb -h localhost -U root library_app_test`.

Each test deletes loans, books, and members first, then inserts the rows it needs through the API. One loan test then updates that loan's due date in the database so the next borrow is overdue. Each controller covers a rejected call, an allowed call, and that controller's own rules.

Test logins are `admin@admin.com` / `admin`, `razza@razza.com` / `razza`, and `rafif@rafif.com` / `rafif`.
