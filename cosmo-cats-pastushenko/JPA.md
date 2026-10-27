# Лабораторна 3 — JPA і транзакції, блоки 2–3

Пастушенко Ростислав, ІС-44.

## Запуск

Потрібні Java 21 та запущений Docker Desktop. Команди з папки з `build.gradle`:

```bash
docker compose up -d --wait postgres
./gradlew bootRun
```

Застосунок підключається до `jdbc:postgresql://localhost:15432/cosmo_cats`,
користувач `cosmo`, пароль `cosmo_local`. Це локальні навчальні параметри.
Змінні `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` дозволяють змінити підключення.
Якщо змінив порт Compose через DB_PORT, зміни відповідно і DB_URL.

Liquibase запускається автоматично. Три міграції першого блока не змінені:
якщо вони вже виконані, дані та історія зберігаються. Hibernate має
`ddl-auto: validate`: створенням і змінами структури керує Liquibase.

## Структура коду

Controller → Service → доменний Repository → JPA adapter → Spring Data Repository → PostgreSQL.

Доменні моделі незалежні від Hibernate. Окремі JPA entities відображають
таблиці categories, products, orders, order_items. `allocationSize = 1`
відповідає `INCREMENT BY 1` у sequences.

Product має ManyToOne до Category. Order має OneToMany до OrderItem з cascade
та orphanRemoval. OrderItem посилається на Product, але зберігає історичні
назву й ціну позиції. Підсумки обчислюються з позицій, а не дублюються в БД.

Читання працює у `@Transactional(readOnly = true)`, зміни — у звичайній
сервісній транзакції. Замовлення та всі його позиції зберігаються атомарно.
Оновлення й видалення використовують PESSIMISTIC_WRITE-блокування рядка.
PUT не створює ресурс при відсутності ID. `open-in-view: false`:
перетворення entities у доменні об'єкти завершується всередині транзакції.

Час створення замовлення має точність до мікросекунд, узгоджену з PostgreSQL,
тому значення в POST-відповіді й подальшому GET збігаються.
In-memory реалізації перенесені до src/test і не є Spring-компонентами.

## API

| Колекція | GET колекції | GET /{id} | POST колекції | PUT /{id} | DELETE /{id} |
| --- | --- | --- | --- | --- | --- |
| /api/products | 200 | 200 | 201 | 200 | 204 |
| /api/categories | 200 | 200 | 201 | 200 | 204 |
| /api/orders | 200 | 200 | 201 | 200 | 204 |

Списки приймають page від 0 та size 1–100, сортуються за ID і повертають
content, page, size, totalElements, totalPages. POST повертає відносний Location.
Валідація: 400; відсутній ресурс: 404; конфлікт UNIQUE/FK або блокування: 409.
Помилки повертаються у форматі ProblemDetail без SQL-подробиць.

Категорія:

```json
{"name":"Orbital Gadgets"}
```

Замовлення:

```json
{"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}
```

Дозволено 1–100 позицій, повтор productId заборонено. Клієнт не передає ціну,
назву товару, час або ID замовлення: їх визначає сервер. Зміна товару не змінює
раніше збережене замовлення. PUT замовлення явно замінює всі позиції за поточними
цінами, зберігаючи id та createdAt. Видалення замовлення видаляє його позиції,
але не товари. Товар з історією замовлень і категорію з товарами видалити не можна.

Це CRUD замовлень. Оплата, резервування і списання складських залишків не
реалізовані; stockQuantity залишається редагованим полем каталогу.

Приклади для IntelliJ HTTP Client — lab3-requests.http. Для перевірки збереження
створи категорію, перезапусти застосунок та виконай GET за її ID.

## Перевірки

```bash
./gradlew test
./gradlew clean build
```

`test` запускає unit/MockMvc без Docker. Повна збірка додатково запускає
ProductApiIT та DatabaseCrudIT на PostgreSQL через Testcontainers. Це окремі тимчасові БД,
вони не використовують дані Compose. Docker обов'язковий для повної збірки.
Поріг JaCoCo збережено: ≥80% рядків загалом, ≥80% рядків і гілок кожного сервісу.

DatabaseCrudIT використовує @DataJpaTest, перевіряє CRUD, обмеження та rollback
для реальних сервісних транзакцій. Сценарії та запуск описані в [TESTING.md](TESTING.md).

Джерела: [Spring Data JPA transactions](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html),
[Spring Boot Testcontainers](https://docs.spring.io/spring-boot/3.5/reference/testing/testcontainers.html),
[PostgreSQL timestamp precision](https://www.postgresql.org/docs/17/datatype-datetime.html).
