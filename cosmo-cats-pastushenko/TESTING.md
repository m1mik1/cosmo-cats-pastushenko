# Лабораторна робота № 2 — тести та CI

Пастушенко Ростислав, ІС-44, КПІ. (Зроблено через ШІ, для кращого розуміння та читання)

## Запуск

Потрібен **JDK 21**. Команди виконуються в каталозі з `build.gradle`.

```bash
./gradlew clean build
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

В IntelliJ IDEA слід обрати JDK 21 для Project SDK і Gradle JVM,
а в налаштуваннях Gradle — **Run tests using: Gradle**. Після змін
`build.gradle` потрібно виконати Reload All Gradle Projects.

Окремі етапи:

| Команда | Призначення |
| --- | --- |
| `./gradlew test` | Unit-тести та MVC-тести; класи `*IT` виключено |
| `./gradlew integrationTest` | Сценарії `*IT` з реальним HTTP-сервером |
| `./gradlew jacocoTestReport` | Обидва набори тестів та об'єднаний звіт |
| `./gradlew check` | Обидва набори тестів і перевірка порогів покриття |
| `./gradlew clean build` | Чиста повна збірка, усі перевірки та JAR |

## Що перевіряється

| Тестовий клас | Основні сценарії |
| --- | --- |
| `ProductServiceTest` | Усі п'ять публічних методів сервісу: пагінація, CRUD, відсутній товар або категорія, некоректний id, заборона upsert, відсутність запису після відхилення даних |
| `ProductControllerTest` | GET/POST/PUT/DELETE, 200/201/204, Location, пагінація, валідація полів для POST і PUT, строгий JSON, переповнення чисел, 400/404/405/406/415/500, формат ProblemDetail |
| `DomainRulesTest` | Точні грошові суми, межі полів, незмінність колекцій, сума замовлення, великі значення offset |
| `InMemoryProductRepositoryTest` | Стабільне сортування, унікальні id, відсутність повторного використання id, повне оновлення, видалення, відсутність upsert |
| `ProductMapperTest` | Перенесення полів, точність ціни, заборона відповіді без id та відсутніх обов'язкових чисел |
| `ProductApiIT` | Повний CRUD через справжній HTTP-сервер з `/cosmo`; відхилені записи не змінюють стан сховища |

Сервіс перевіряється із замоканими інтерфейсами репозиторіїв, без Spring.
MVC-тести використовують справжні Jackson, Bean Validation, mapper та обробник
помилок, а сервіс замінено через `@MockitoBean`.
Інтеграційні тести запускають Spring Boot на випадковому порту та використовують
Java HttpClient, справжній сервіс і репозиторії. Створені товари видаляються
у `finally`, тому сценарії не залежать від порядку запуску.

Регресійні сценарії для виправлень ЛР № 1: `12.500` приймається без втрати
точності, `12.345` відхиляється; помилки PUT містять назви полів DTO;
Location зберігає context path і не містить query string.

## Покриття

Підключені `gradle/test.gradle` та `gradle/jacoco.gradle`.
JaCoCo об'єднує результати `test` і `integrationTest`. Класи застосунку
не виключаються зі звіту.

Пороги, які перевіряє збірка:

- кожен клас пакета `application.service`: щонайменше **80% рядків і 80% гілок**;
- весь застосунок: щонайменше **80% рядків**.

`check` явно залежить від `jacocoTestCoverageVerification`, тому низьке покриття
завершує `build` з помилкою. Вимога курсу до сервісів — мінімум 60%,
рекомендовано 80%; у цьому проєкті встановлено поріг 80%.

Звіти після збірки:

- `build/reports/tests/test/index.html`;
- `build/reports/tests/integrationTest/index.html`;
- `build/reports/coverage/index.html`;
- `build/reports/coverage/coverage.xml` та `coverage.csv`.

Локальна перевірка підготовленої версії, 23.10.2026, Java 21 / Gradle 8.10.2:

| Перевірка | Результат |
| --- | --- |
| `test` | 144 тести, 0 помилок, 0 пропущених |
| `integrationTest` | 2 тести, 0 помилок, 0 пропущених |
| `ProductService`, рядки | 30/30 — 100% |
| `ProductService`, гілки | 12/12 — 100% |
| Увесь застосунок, рядки | 294/304 — 96,71% |
| Увесь застосунок, гілки | 101/116 — 87,07% |
| `build`, включно з перевіркою покриття | BUILD SUCCESSFUL |
| Workflow, actionlint 1.7.12 | Без помилок |

Це локальні результати. Статус GitHub Actions потрібно перевірити після push.

## CI

Workflow `.github/workflows/pull_request.yml` має бути в **корені Git-репозиторію**.
Gradle-проєкт вкладений у каталог `cosmo-cats-pastushenko`, тому workflow задає
цей `working-directory`. На push і pull request він встановлює Java 21,
виконує `./gradlew --no-daemon clean build` і зберігає повні звіти як artifact
`test-and-coverage-reports`, зокрема після невдалого тесту.

Приклад викладача адаптовано до Java 21, вкладеної структури репозиторію,
актуальних версій actions та явного запуску перевірок.
Mockito підключається Java-агентом під час старту тестової JVM: це усуває
залежність від dynamic self-attachment, який може бути обмежений на Java 21.

WireMock — додаткове завдання курсу. У поточному застосунку немає вихідних
HTTP-клієнтів, тому він не використовується. Наявні інтеграційні тести
перевіряють власний сервер і не потребують доступу до зовнішніх сервісів.

Джерела:

- [Умови курсу](https://grzik-production.notion.site/Commerce-Development-with-Java-or-Kotlin-ourse-V3-3d0bf3d0970a801fa559d5de13d8aad8)
- [Приклад викладача](https://github.com/Ekatereana/Java-WEB-KPI-Course/pull/1)
- [Mockito: instrumentation для Java 21+](https://javadoc.io/static/org.mockito/mockito-core/5.20.0/org.mockito/org/mockito/Mockito.html#0.3)
- [Gradle: JaCoCo plugin](https://docs.gradle.org/8.10.2/userguide/jacoco_plugin.html)
