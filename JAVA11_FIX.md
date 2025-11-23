# Исправление проблемы с Java 11

## Проблема
```
java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException
```

## Причина
В Java 11 были удалены модули JAXB (Java Architecture for XML Binding), которые нужны для Hibernate и JPA.

## Решение

### Шаг 1: Используйте исправленный pom.xml
Файл `pom.xml` уже обновлен с необходимыми зависимостями JAXB.

### Шаг 2: Запустите с правильным скриптом
```bash
start-java11.bat
```

### Шаг 3: Если не работает, используйте ручной запуск
```bash
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--add-modules java.xml.bind --add-modules java.activation"
```

## Альтернативные способы запуска

### Вариант 1: С параметрами модулей
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--add-modules java.xml.bind,java.activation"
```

### Вариант 2: С системными свойствами
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djavax.xml.bind.JAXBContextFactory=com.sun.xml.bind.v2.ContextFactory"
```

### Вариант 3: Запуск с полными параметрами
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="--add-modules java.xml.bind,java.activation -Dorg.slf4j.simpleLogger.defaultLogLevel=INFO"
```

## Проверка исправления

После запуска вы должны увидеть:
```
2025-10-06 15:30:00 INFO ScalperBotApplication - Started ScalperBotApplication in 3.5 seconds
```

## Если проблема остается

### 1. Очистите кэш Maven
```bash
mvn clean
mvn dependency:purge-local-repository
```

### 2. Удалите папку target
```bash
rmdir /s target
```

### 3. Пересоберите проект
```bash
mvn clean compile
```

### 4. Проверьте версию Java
```bash
java -version
```
Должна быть Java 11 или выше.

## Дополнительные зависимости

В `pom.xml` добавлены:
- `jaxb-api` - JAXB API
- `jaxb-runtime` - JAXB Runtime
- `javax.activation-api` - Activation API
- `jaxb-core` - JAXB Core

## Скрипты запуска

- `start-java11.bat` - запуск с поддержкой Java 11
- `start-no-logback.bat` - запуск без Logback
- `start-simple.bat` - запуск с простым логированием

## Контакты

При проблемах проверьте:
1. Версию Java (должна быть 11+)
2. Версию Maven (должна быть 3.6+)
3. Свободность порта 8080
4. Наличие всех зависимостей в classpath
