# Исправление проблемы с логированием

## Проблема
```
LoggerFactory is not a Logback LoggerContext but Logback is on the classpath
```

## Решение

### Шаг 1: Используйте исправленный pom.xml
Файл `pom.xml` уже исправлен - исключены все зависимости Logback.

### Шаг 2: Запустите с правильным скриптом
```bash
start-no-logback.bat
```

### Шаг 3: Если не работает, используйте минимальную конфигурацию
```bash
# Переименуйте файлы
ren pom.xml pom-full.xml
ren pom-minimal.xml pom.xml

# Запустите
mvn clean compile
mvn spring-boot:run
```

## Альтернативные способы запуска

### Вариант 1: Ручной запуск
```bash
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dorg.slf4j.simpleLogger.defaultLogLevel=INFO"
```

### Вариант 2: Запуск без логирования
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.root=OFF"
```

### Вариант 3: Запуск с системными свойствами
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dorg.slf4j.simpleLogger.defaultLogLevel=INFO -Dlogging.level.root=INFO"
```

## Проверка исправления

После запуска вы должны увидеть:
```
2024-10-06 15:30:00 [main] INFO  com.bitunix.scalper.ScalperBotApplication - Starting ScalperBotApplication
2024-10-06 15:30:01 [main] INFO  com.bitunix.scalper.ScalperBotApplication - Started ScalperBotApplication in 2.5 seconds
```

## Если проблема остается

1. **Очистите кэш Maven:**
   ```bash
   mvn clean
   mvn dependency:purge-local-repository
   ```

2. **Удалите папку target:**
   ```bash
   rmdir /s target
   ```

3. **Пересоберите проект:**
   ```bash
   mvn clean compile
   ```

## Файлы для разных конфигураций

- `pom.xml` - исправленная версия (исключен Logback)
- `pom-minimal.xml` - минимальные зависимости
- `pom-full.xml` - полные зависимости (если нужно)

## Скрипты запуска

- `start-no-logback.bat` - запуск без Logback
- `start-simple.bat` - запуск с простым логированием
- `start-fixed.bat` - запуск с исправленным логированием
