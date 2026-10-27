# Тестування

Поточний стан: лабораторна 3, блок 2.

```bash
./gradlew test
./gradlew clean build
```

Перша команда запускає unit/MockMvc без Docker. Друга додатково запускає
ProductApiIT на PostgreSQL через Testcontainers та перевіряє покриття JaCoCo.
Потрібний працюючий Docker Engine. Testcontainers створює окрему тимчасову
базу й не підключається до навчальних даних Compose.

Unit-тести перевіряють сервіси, JPA-адаптери з Mockito, доменні правила й мапери.
MockMvc перевіряє CRUD контролерів і відхилення некоректних запитів до виклику
сервісу. ProductApiIT перевіряє повний HTTP CRUD товарів із реальною БД у Docker.
Тести адаптерів із Mockito не заміняють інтеграційні тести БД.

У третьому блоці додаються окремі CRUD-тести з частковим Spring-контекстом,
перевірки rollback та обмежень для всіх сутностей на PostgreSQL/Testcontainers.

Поріг не знижено: ≥80% рядків загалом, ≥80% рядків і гілок кожного сервісу.
Gradle check залежить від тестів і перевірки JaCoCo. GitHub Actions виконує clean build.
HTML: build/reports/coverage/index.html; XML: build/reports/coverage/coverage.xml.
Звіти CI доступні в артефакті test-and-coverage-reports.
