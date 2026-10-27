# Тестування лабораторної 3

Пастушенко Ростислав, ІС-44. Блоки 1–3.

## Запуск

Потрібні Java 21 та Docker Engine для інтеграційних тестів.
З папки з build.gradle:

```bash
./gradlew test
./gradlew integrationTest
./gradlew clean build
```

`test` запускає 183 unit/MockMvc-тести без Docker. `integrationTest` запускає
2 HTTP-сценарії ProductApiIT і 18 сценаріїв DatabaseCrudIT. Повна збірка
додатково формує звіт JaCoCo та перевіряє пороги покриття.

Testcontainers створює окремі тимчасові PostgreSQL 17. Контейнери керуються
Spring через @ServiceConnection і закриваються разом із тестовим контекстом.
Навчальні дані Compose не використовуються. За відсутності Docker повна
збірка завершується помилкою: інтеграційні тести не пропускаються автоматично.

## Що перевіряється

| Група | Перевірки |
| --- | --- |
| Unit/MockMvc | Сервіси, доменні правила, мапери, JPA-адаптери з Mockito, позитивні та негативні HTTP-запити |
| ProductApiIT | Повний HTTP CRUD товару, пагінація, Location з context path, відхилення записів |
| DatabaseCrudIT | Частковий Spring-контекст, CRUD Product/Category/Order, обмеження БД, історія ціни, реальні транзакції |

DatabaseCrudIT використовує @DataJpaTest та явний імпорт лише потрібних
сервісів, адаптерів і контейнерної конфігурації. HTTP-сервер і контролери не
піднімаються. @AutoConfigureTestDatabase(replace=NONE) зберігає підключення до
PostgreSQL; H2 не використовується.

Тестова @Transactional має propagation=NOT_SUPPORTED. Тому кожен виклик
сервісу повинен сам відкрити транзакцію й завершити її commit або rollback.
Наступне читання перевіряє дані з БД після завершення попередньої транзакції,
а не об'єкт у тому самому persistence context.

18 сценаріїв DatabaseCrudIT:

- виконані міграції, ID sequences та відсутність контролерів у контексті;
- CRUD окремо для Category, Product та Order;
- унікальність категорії й назви товару в межах категорії;
- повний rollback конфліктного оновлення товару;
- заборона видалення категорії/товару, які мають залежності;
- історичні ціна/назва й незмінний createdAt замовлення;
- rollback заголовка та всіх позицій при помилці другої позиції;
- відновлення початкових позицій при помилці заміни замовлення;
- відсутні посилання й PUT без неявного INSERT;
- стабільна пагінація та великі offsets;
- ID не використовується повторно після видалення;
- три параметризовані перевірки CHECK-обмежень напряму в БД;
- повторне виконання Liquibase без дублювання даних.

Для перевірки rollback тест тимчасово додає CHECK, який відхиляє quantity=13.
Це створює помилку вже під час запису в БД, після початку збереження замовлення.
Обмеження існує тільки в тестовому контейнері та видаляється у finally.
Воно не входить у Liquibase-міграції і не є бізнес-правилом застосунку.
Перед кожним сценарієм дані контейнера очищуються; історія Liquibase залишається.

## Покриття та CI

Поріг збережено: ≥80% рядків загалом, ≥80% рядків і гілок кожного сервісу.
Gradle check залежить від тестів і перевірки JaCoCo. GitHub Actions виконує
clean build. Немає виключення production-класів з покриття або зниження порогів.

- HTML: build/reports/coverage/index.html
- XML: build/reports/coverage/coverage.xml
- Unit: build/reports/tests/test/index.html
- Integration: build/reports/tests/integrationTest/index.html
- CI-артефакт: test-and-coverage-reports

Орієнтир повної збірки: 183 unit/MockMvc + 20 integration = 203 тести.
Фактичний результат і покриття для здачі беруться з CI останнього комміту.
Завантажуй і розпаковуй артефакт повністю: HTML використовує сусідні файли.

Джерела: [Spring Boot test slices](https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html),
[Spring test transactions](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html).
