# Cosmo Cats Intergalactic Marketplace

Навчальний проєкт з курсу Commerce Development with Java or Kotlin.

Студент: Пастушенко Ростислав. Група: ІС-44, КПІ.

## Стек

- Java 21
- Spring Boot 3.5.16
- Gradle 8.10.2, Groovy DSL
- Spring Web і Jakarta Bean Validation

## Поточний стан

У межах лабораторної роботи № 1 реалізовано:

- доменні моделі Product, Category, Cart і Order;
- DTO та ручний ProductMapper;
- зберігання товарів у пам'яті та сервісний шар;
- REST CRUD товарів і пагінацію зі стабільним сортуванням за id;
- валідацію запитів і глобальну обробку помилок у форматі RFC 9457;
- OpenAPI-контракт у `src/main/resources/api-specs/products-api.yml`.

Зміни збираються у гілці `lab1-draft` для self-review через Pull Request.

## Запуск

Потрібен JDK 21. Окремо встановлювати Gradle не потрібно: у репозиторії є
Gradle Wrapper. Під час першого запуску він завантажує Gradle та залежності.

macOS / Linux:

```bash
chmod +x gradlew
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Застосунок запускається на порту `8080`. Успішний запуск підтверджує
повідомлення `Started CosmoCatsApplication` у консолі.
Перевірити API можна запитом `GET http://localhost:8080/api/products`.
Головна сторінка `/` не використовується.

Зібрати виконуваний JAR:

```bash
./gradlew clean build
```

## API товарів

| Метод | Шлях | Успішна відповідь |
| --- | --- | --- |
| GET | `/api/products?page=0&size=20` | 200: content, page, size, totalElements, totalPages |
| GET | `/api/products/{productId}` | 200: товар |
| POST | `/api/products` | 201: створений товар і заголовок Location |
| PUT | `/api/products/{productId}` | 200: повністю оновлений товар |
| DELETE | `/api/products/{productId}` | 204: без тіла відповіді |

Приклад тіла POST або PUT:

```json
{
  "name": "Stellar Treats",
  "description": "Treats for space cats",
  "price": 15.99,
  "categoryId": 1,
  "stockQuantity": 10
}
```

Ціни наведено в USD та обробляються через BigDecimal. Мінімальна ціна — 0.01,
максимальна — 9999999999.99, крок — 0.01. Кількість товару — невід'ємне ціле
число. Назва має містити від 3 до 120 символів і не складатися лише з пробілів.
Опис необов'язковий, не більше 2000 символів.
Кратність ціни 0.01 перевіряє Bean Validation-анотація `@CentAmount`:
12.500 дозволено як 12.50, а 12.345 відхиляється з `errors[].field = price`.
Перевірка грошей у доменній моделі залишається незалежною від HTTP.

Ідентифікатор створює сервер. Тіло запиту з полем id або іншими невідомими
полями відхиляється. Числові поля потрібно передавати JSON-числами.
Параметр page починається з 0, size має бути від 1 до 100. Сторінка за межами
списку має порожній content і коректні загальні лічильники.

Помилки повертаються з Content-Type `application/problem+json` і полями
type, title, status, detail та instance. Для порушень валідації додається
масив errors із назвами полів і повідомленнями. Невалідні дані дають 400,
відсутній товар або категорія — 404, непідтримуваний Content-Type — 415.
Внутрішні помилки повертають 500 із загальним повідомленням.
Для POST і PUT масив errors містить назви полів DTO, наприклад name або price;
для параметрів URL — productId, page або size.

Застосунок також підтримує запуск із префіксом, наприклад аргументом
`--server.servlet.context-path=/cosmo`. Тоді API доступний за
`http://localhost:8080/cosmo/api/products`, а Location після POST містить
цей префікс. За звичайного запуску Location має вигляд `/api/products/{id}`.

## Початкові дані

Категорії: 1 — Space Food, 2 — Cosmic Toys, 3 — Space Accessories.
Після запуску доступні три товари: Cosmic Milk, Galaxy Yarn та Comet Collar.
Кожен перезапуск відновлює початкові дані; зміни з попереднього запуску
не зберігаються. Інтеграція з базою даних додається у лабораторній роботі № 3.

Приклади позитивних і негативних HTTP-запитів містяться в `api-requests.http`.
Після створення товару потрібно вказати його отриманий id у змінній productId.
Автоматизовані тести, CI та перевірка покриття додаються у лабораторній роботі № 2.
