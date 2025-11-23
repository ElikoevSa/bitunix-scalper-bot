# Система ограничения запросов (Rate Limiting)

## Обзор

В проекте реализована система ограничения запросов, которая ограничивает количество API запросов до 7 запросов в секунду для каждого источника.

## Компоненты системы

### 1. RateLimiterService
Основной сервис для управления ограничениями запросов.

**Основные методы:**
- `canMakeRequest(String apiName)` - проверяет, можно ли выполнить запрос
- `waitIfNeeded(String apiName)` - ждет, если лимит превышен
- `getCurrentRequestCount(String apiName)` - получает текущий счетчик запросов
- `getTimeUntilReset(String apiName)` - получает время до сброса счетчика
- `resetCounter(String apiName)` - сбрасывает счетчик для конкретного API
- `resetAllCounters()` - сбрасывает все счетчики

### 2. RateLimiterController
REST API для мониторинга и управления rate limiting.

**Доступные эндпоинты:**
- `GET /api/rate-limiter/status` - статус всех API
- `GET /api/rate-limiter/info/{apiName}` - информация о конкретном API
- `GET /api/rate-limiter/check/{apiName}` - проверка возможности запроса
- `GET /api/rate-limiter/reset/{apiName}` - сброс счетчика API
- `GET /api/rate-limiter/reset-all` - сброс всех счетчиков

### 3. RateLimiterInterceptor
Глобальный интерцептор для всех HTTP запросов к приложению.

### 4. WebConfig
Конфигурация для регистрации интерцептора.

## Настроенные API

Система отслеживает следующие API:
- `bitunix` - запросы к Bitunix API
- `binance` - запросы к Binance API (резервный)
- `coingecko` - запросы к CoinGecko API (резервный)
- `trading_cycle` - торговые циклы
- `dashboard` - загрузка дашборда
- `trading_control` - управление торговлей

## Применение в коде

### В сервисах
```java
@Autowired
private RateLimiterService rateLimiterService;

public void someApiCall() {
    // Ждем, если лимит превышен
    rateLimiterService.waitIfNeeded("api_name");
    
    // Или проверяем перед запросом
    if (!rateLimiterService.canMakeRequest("api_name")) {
        System.out.println("Rate limit exceeded");
        return;
    }
    
    // Выполняем API запрос
}
```

### В контроллерах
```java
@GetMapping("/some-endpoint")
public String someEndpoint() {
    if (!rateLimiterService.canMakeRequest("endpoint_name")) {
        return "rate_limit_exceeded";
    }
    // Логика эндпоинта
}
```

## Мониторинг

### Проверка статуса
```bash
curl http://localhost:8080/api/rate-limiter/status
```

### Проверка конкретного API
```bash
curl http://localhost:8080/api/rate-limiter/info/bitunix
```

### Сброс счетчика
```bash
curl http://localhost:8080/api/rate-limiter/reset/bitunix
```

## Настройки

Текущие настройки (в RateLimiterService.java):
- Максимум запросов в секунду: 7
- Время окна: 1000 мс (1 секунда)
- Интервал ожидания при превышении лимита: 100 мс

## Глобальный Rate Limiting

Глобальный интерцептор применяется ко всем HTTP запросам и использует комбинацию IP адреса клиента и URI запроса для создания уникального ключа.

Исключения из глобального rate limiting:
- `/static/**` - статические ресурсы
- `/css/**` - CSS файлы
- `/js/**` - JavaScript файлы
- `/images/**` - изображения
- `/favicon.ico` - иконка сайта

## Тестирование

Для тестирования системы создан `RateLimiterServiceTest.java` с тестами:
- Проверка лимитов запросов
- Отслеживание счетчиков
- Сброс счетчиков
- Независимость разных API

## Логирование

Система выводит сообщения в консоль при превышении лимитов:
- "Rate limit exceeded for trading cycle, skipping this iteration"
- "Rate limit exceeded for dashboard, using cached data"
- "Rate limit exceeded for trading control"

## Рекомендации

1. **Мониторинг**: Регулярно проверяйте статус rate limiter через API
2. **Настройка**: При необходимости измените лимиты в RateLimiterService
3. **Кэширование**: Используйте кэширование данных при превышении лимитов
4. **Логирование**: Добавьте более детальное логирование для отладки
