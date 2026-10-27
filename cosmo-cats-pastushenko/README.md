# Cosmo Cats Intergalactic Marketplace

Пастушенко Ростислав, ІС-44, КПІ. Commerce Development with Java or Kotlin.

Поточний стан: лабораторна 3, блоки 1–3. Товари, категорії й замовлення зберігаються
у PostgreSQL. Дані залишаються після перезапуску застосунку та контейнера.

Стек: Java 21, Spring Boot 3.5.16, Gradle 8.10.2, PostgreSQL 17, Liquibase 4.33.0,
Spring Data JPA, Jakarta Validation, JUnit 5, Mockito, MockMvc, JaCoCo, Testcontainers.

## Запуск

З папки з build.gradle, з JDK 21 і запущеним Docker Desktop:

```bash
chmod +x gradlew
docker compose up -d --wait postgres
./gradlew bootRun
```

API: http://localhost:8080/api/products, /api/categories, /api/orders.
На Windows замість `./gradlew` використовуй `.\gradlew.bat`.

Liquibase застосовує міграції автоматично; Hibernate перевіряє структуру.
Початкові дані додаються один раз: 3 категорії та товари Cosmic Milk,
Galaxy Yarn, Comet Collar. Named volume PostgreSQL зберігає дані;
`docker compose down` їх не видаляє, а `down -v` видаляє.

## Перевірки та документація

```bash
./gradlew test
./gradlew clean build
```

Повна збірка потребує Docker для Testcontainers. GitHub Actions виконує clean build
та перевірку покриття ≥80%. Звіт: build/reports/coverage/index.html.

- [JPA.md](JPA.md): архітектура, API, транзакції та запуск.
- [DATABASE.md](DATABASE.md): схема, sequences, індекси й обмеження.
- [TESTING.md](TESTING.md): тести та межі перевірок.
- [LAB3_CHECKLIST.md](LAB3_CHECKLIST.md): вимоги та підготовка до здачі.
- [lab3-requests.http](lab3-requests.http): сценарій перевірки в IntelliJ.
- [OpenAPI товарів](src/main/resources/api-specs/products-api.yml).

Попередні лабораторні збережені у lab1-draft, lab1-fixes та lab2-testing.
Лабораторна 3 продовжується в lab3-database. Додано CRUD-тести з частковим
Spring-контекстом і перевірки rollback. Перед здачею потрібні зелений CI
останнього комміту та пояснювальні коментарі в PR.
