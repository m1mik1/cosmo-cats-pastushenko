# Cosmo Cats Intergalactic Marketplace

Навчальний проєкт з курсу Commerce Development with Java or Kotlin.

Студент: Пастушенко Ростислав. Група: ІС-44, КПІ.

## Стек

- Java 21
- Spring Boot 3.5.16
- Gradle 8.10.2, Groovy DSL
- Spring Web і Jakarta Bean Validation

## Поточний стан

Початкова конфігурація проєкту для лабораторної роботи № 1. CRUD товарів,
доменна модель, OpenAPI-контракт і обробка помилок будуть додані окремими
змінами у гілці `lab1-draft`.

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

Застосунок запускається на порту `8080`. У початковій конфігурації HTTP-ендпоїнти
ще не реалізовано, тому відповідь `404` на `/` очікувана. Успішний запуск
підтверджує повідомлення `Started CosmoCatsApplication` у консолі.

Зібрати виконуваний JAR:

```bash
./gradlew clean build
```

Тести та перевірка покриття додаються у лабораторній роботі № 2.
