# Лабораторна 3 — вимоги та здача

Пастушенко Ростислав, ІС-44.

| Вимога | Реалізація |
| --- | --- |
| 3NF: товари, категорії, замовлення | categories, products, orders, order_items; пояснення в DATABASE.md |
| Liquibase та версіонування | db/changelog/db.changelog-master.xml, 3 changeSet |
| ID sequences та індекси | 001-create-schema.sql, 002-add-constraints-and-indexes.sql |
| UNIQUE для поля | categories.name |
| Унікальність товару в категорії | UNIQUE(category_id, name) |
| Spring Data JPA CRUD | SpringData*Repository, Jpa*Repository, сервіси та контролери |
| Сервісні транзакції | @Transactional для змін, readOnly для читання |
| PostgreSQL у Testcontainers | PostgresTestConfiguration, @ServiceConnection |
| Інтеграційний CRUD з частковим контекстом | DatabaseCrudIT, @DataJpaTest |
| Rollback та обмеження | DatabaseCrudIT: помилки запису, UNIQUE/FK/CHECK |
| Покриття ≥80% | gradle/jacoco.gradle, перевірка у clean build |
| Git і CI | гілка lab3-database, PR № 5, GitHub Actions |

Опціональні Natural ID та звіт популярних товарів через projections не
реалізовані. Складене UNIQUE для товару в категорії реалізоване; оцінку
додаткових пунктів визначає викладач.

## Перед здачею

1. Запустити з Docker: ./gradlew clean build.
2. Запушити зміни й дочекатися зеленого CI останнього комміту.
3. Переглянути звіти тестів та coverage.
4. Додати коментарі до коду в PR: пояснити транзакції, історичні значення,
   sequences і те, як використовувався AI та перевірявся результат.
5. Оновити опис PR відповідно до остаточної реалізації.
6. Після успішної перевірки перевести Draft PR у Ready for review.
7. У таблиці здачі Lab 3 додати посилання на PR і поставити Ready for review.

Посилання для здачі: https://github.com/m1mik1/cosmo-cats-pastushenko/pull/5

До завершення перевірки PR не потрібно мерджити або видаляти попередні гілки.
Одна лише зелена збірка не заміняє пояснювальні коментарі та статус у таблиці.

[Завдання лабораторної 3](https://grzik-production.notion.site/c2fbf3d0970a8389b3920140cd78361a).
