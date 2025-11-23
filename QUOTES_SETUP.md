# Настройка получения котировок

## Проблема
Приложение не получает реальные котировки с биржи Bitunix.

## Решение

### 1. Проверьте подключение к интернету
```bash
ping api.bitunix.com
ping api.binance.com
ping api.coingecko.com
```

### 2. Тестируйте API подключения
```bash
test-api.bat
```

### 3. Настройте API ключи Bitunix

#### Вариант A: Через переменные окружения
```bash
set BITUNIX_API_KEY=your-api-key
set BITUNIX_SECRET_KEY=your-secret-key
```

#### Вариант B: Через файл конфигурации
Создайте файл `src/main/resources/application-local.yml`:
```yaml
bitunix:
  api:
    api-key: YOUR_REAL_API_KEY
    secret-key: YOUR_REAL_SECRET_KEY
```

### 4. Альтернативные источники данных

Приложение автоматически попробует получить данные из:
1. **Bitunix API** (основной)
2. **Binance API** (резервный)
3. **CoinGecko API** (резервный)
4. **Демо-данные** (если все API недоступны)

### 5. Проверка работы

После запуска приложения проверьте логи:
```
Successfully fetched X trading pairs from Binance
Successfully fetched X trading pairs from CoinGecko
Successfully fetched X trading pairs from Bitunix
```

### 6. Ручная проверка API

#### Bitunix API:
```bash
curl "https://api.bitunix.com/api/v1/ticker/24hr"
```

#### Binance API:
```bash
curl "https://api.binance.com/api/v3/ticker/24hr"
```

#### CoinGecko API:
```bash
curl "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=5&page=1"
```

### 7. Устранение неполадок

#### Проблема: "Connection refused"
**Решение:**
- Проверьте подключение к интернету
- Проверьте настройки файрвола
- Попробуйте другой API endpoint

#### Проблема: "API key invalid"
**Решение:**
- Проверьте правильность API ключей
- Убедитесь, что ключи активны
- Проверьте права доступа ключей

#### Проблема: "Rate limit exceeded"
**Решение:**
- Уменьшите частоту запросов
- Используйте кэширование данных
- Переключитесь на другой API

### 8. Настройка прокси (если нужно)

Если у вас есть прокси, добавьте в `application.yml`:
```yaml
http:
  proxy:
    host: your-proxy-host
    port: your-proxy-port
    username: your-username
    password: your-password
```

### 9. Мониторинг подключения

Добавьте в логи информацию о подключении:
```yaml
logging:
  level:
    com.bitunix.scalper.service.BitunixApiService: DEBUG
    com.bitunix.scalper.service.AlternativeDataService: DEBUG
```

### 10. Автоматическое обновление данных

Приложение автоматически обновляет данные каждые 30 секунд через планировщик задач.

## Контакты

При проблемах:
1. Проверьте логи приложения
2. Убедитесь в правильности API ключей
3. Проверьте подключение к интернету
4. Попробуйте альтернативные API endpoints
