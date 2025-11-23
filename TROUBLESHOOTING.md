# Устранение неполадок Bitunix Scalper Bot

## Ошибка SLF4J: NoClassDefFoundError

### Проблема
```
Exception in thread "main" java.lang.NoClassDefFoundError: org/slf4j/impl/StaticLoggerBinder
```

### Решение

#### Вариант 1: Использовать исправленный скрипт запуска
```bash
start-fixed.bat
```

#### Вариант 2: Ручной запуск с правильными параметрами
```bash
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.config=classpath:logback-spring.xml"
```

#### Вариант 3: Запуск с профилем
```bash
mvn spring-boot:run -Dspring.profiles.active=default
```

### Дополнительные исправления

1. **Проверьте версию Java:**
   ```bash
   java -version
   ```
   Должна быть Java 11 или выше.

2. **Очистите кэш Maven:**
   ```bash
   mvn clean
   mvn dependency:purge-local-repository
   ```

3. **Пересоберите проект:**
   ```bash
   mvn clean compile
   mvn package
   ```

## Другие возможные ошибки

### Ошибка компиляции: cannot find symbol toList()
**Решение:** Уже исправлено в коде - используется `.collect(Collectors.toList())` вместо `.toList()`

### Ошибка подключения к API
**Решение:**
1. Проверьте правильность API ключей в `application.yml`
2. Убедитесь в наличии интернет-соединения
3. Проверьте статус API Bitunix

### Ошибка базы данных H2
**Решение:**
1. Убедитесь, что порт 8080 свободен
2. Проверьте права доступа к папке проекта
3. Очистите временные файлы H2

## Логирование

Логи сохраняются в папке `logs/`:
- `scalper-bot.log` - текущий лог
- `scalper-bot.YYYY-MM-DD.log` - архивы по дням

## Контакты

При возникновении проблем:
1. Проверьте логи в папке `logs/`
2. Убедитесь в правильности настроек
3. Проверьте версии Java и Maven
