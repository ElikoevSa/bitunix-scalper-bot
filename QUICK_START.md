# Быстрый запуск Bitunix Scalper Bot

## Проблема с логированием

Если возникают ошибки SLF4J, используйте минимальную конфигурацию:

### Вариант 1: Использовать минимальный pom.xml

1. **Переименуйте файлы:**
   ```bash
   ren pom.xml pom-full.xml
   ren pom-minimal.xml pom.xml
   ```

2. **Запустите:**
   ```bash
   start-simple.bat
   ```

### Вариант 2: Ручной запуск с параметрами

```bash
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dorg.slf4j.simpleLogger.defaultLogLevel=INFO"
```

### Вариант 3: Запуск без логирования

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.root=OFF"
```

## Структура проекта

```
bitunix-scalper-bot/
├── pom.xml                 # Основной файл зависимостей
├── pom-minimal.xml         # Минимальные зависимости
├── pom-full.xml           # Полные зависимости с логированием
├── start.bat              # Обычный запуск
├── start-simple.bat       # Запуск с простым логированием
├── start-fixed.bat        # Запуск с исправленным логированием
└── src/main/resources/
    ├── application.yml    # Конфигурация приложения
    ├── simplelogger.properties  # Простое логирование
    └── logback-spring.xml # Расширенное логирование
```

## Рекомендуемый порядок запуска

1. **Попробуйте:** `start-simple.bat`
2. **Если не работает:** Используйте `pom-minimal.xml`
3. **Если все еще не работает:** Запустите с отключенным логированием

## Устранение неполадок

### Ошибка: NoClassDefFoundError
**Решение:** Используйте `pom-minimal.xml`

### Ошибка: StaticLoggerBinder
**Решение:** Запустите `start-simple.bat`

### Ошибка: Port already in use
**Решение:** Измените порт в `application.yml`:
```yaml
server:
  port: 8081
```

## Контакты

При проблемах проверьте:
1. Версию Java (должна быть 11+)
2. Версию Maven (должна быть 3.6+)
3. Свободность порта 8080
