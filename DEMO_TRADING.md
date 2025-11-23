# Bybit Demo Trading Integration

## Обзор

Проект интегрирован с Bybit Demo Trading API для безопасного тестирования торговых стратегий без использования реальных средств.

## Настройка

### 1. Создание API ключа для демо-торговли

1. Войдите в свой аккаунт на [Bybit](https://www.bybit.com/)
2. Переключитесь в режим **"Demo Trading"**
3. Наведите курсор на иконку профиля и выберите **"API"**
4. Сгенерируйте API ключ и секретный ключ для демо-торговли

### 2. Настройка конфигурации

Обновите файл `src/main/resources/application.yml`:

```yaml
bitunix:
  api:
    base-url: https://api-demo.bybit.com
    api-key: YOUR_DEMO_API_KEY
    secret-key: YOUR_DEMO_SECRET_KEY
    demo:
      enabled: true
      recv-window: 5000
```

Или используйте переменные окружения:

```bash
set BITUNIX_API_KEY=your-demo-api-key
set BITUNIX_SECRET_KEY=your-demo-secret-key
```

## Домены API

- **REST API**: `https://api-demo.bybit.com`
- **WebSocket**: `wss://stream-demo.bybit.com` (только приватные потоки)
- **Публичные данные**: `wss://stream.bybit.com` (используйте основной домен)

## Доступные функции

### 1. Запрос демо-средств

**Эндпоинт**: `POST /demo/funds/request`

**Параметры**:
- `adjustType`: 0 (добавить) или 1 (уменьшить)
- `coin`: BTC, ETH, USDT, USDC
- `amount`: Сумма (максимум: BTC: 15, ETH: 200, USDT/USDC: 100000)

**Пример**:
```bash
curl -X POST "http://localhost:8080/demo/funds/request?adjustType=0&coin=USDT&amount=100000"
```

### 2. Получение баланса

**Эндпоинт**: `GET /demo/balance`

**Параметры**:
- `accountType`: UNIFIED (по умолчанию)

**Пример**:
```bash
curl "http://localhost:8080/demo/balance?accountType=UNIFIED"
```

### 3. Размещение ордера

**Эндпоинт**: `POST /demo/order/place`

**Параметры**:
- `category`: linear, spot, option
- `symbol`: BTCUSDT, ETHUSDT и т.д.
- `side`: Buy или Sell
- `orderType`: Market или Limit
- `qty`: Количество
- `price`: Цена (для лимитных ордеров)

**Пример**:
```bash
curl -X POST "http://localhost:8080/demo/order/place?category=linear&symbol=BTCUSDT&side=Buy&orderType=Market&qty=0.001"
```

### 4. Получение открытых ордеров

**Эндпоинт**: `GET /demo/orders/open`

**Параметры**:
- `category`: linear, spot, option
- `symbol`: (опционально) конкретный символ

**Пример**:
```bash
curl "http://localhost:8080/demo/orders/open?category=linear"
```

### 5. Отмена ордера

**Эндпоинт**: `POST /demo/order/cancel`

**Параметры**:
- `category`: linear, spot, option
- `symbol`: BTCUSDT и т.д.
- `orderId`: (опционально) ID ордера
- `orderLinkId`: (опционально) Link ID ордера

**Пример**:
```bash
curl -X POST "http://localhost:8080/demo/order/cancel?category=linear&symbol=BTCUSDT&orderId=123456"
```

### 6. Получение позиций

**Эндпоинт**: `GET /demo/positions`

**Параметры**:
- `category`: linear, spot, option
- `symbol`: (опционально) конкретный символ

**Пример**:
```bash
curl "http://localhost:8080/demo/positions?category=linear"
```

### 7. Информация об аккаунте

**Эндпоинт**: `GET /demo/account/info`

**Пример**:
```bash
curl "http://localhost:8080/demo/account/info"
```

## Веб-интерфейс

Откройте в браузере: `http://localhost:8080/demo/dashboard`

Веб-интерфейс предоставляет:
- Информацию об аккаунте
- Баланс кошелька
- Размещение ордеров
- Просмотр открытых ордеров
- Просмотр позиций
- Запрос демо-средств

## Ограничения демо-торговли

1. **Срок хранения ордеров**: Ордера хранятся 7 дней
2. **Лимиты запросов**: Стандартные лимиты, не обновляются
3. **Доступные API**: Не все API доступны в демо-режиме (см. [документацию](https://bybit-exchange.github.io/docs/v5/demo))

## Начальный баланс

При создании демо-аккаунта автоматически предоставляются:
- 50,000 USDT
- 50,000 USDC
- 1 BTC
- 1 ETH

## Важные замечания

1. **Изолированная среда**: Демо-торговля полностью изолирована от реальной торговли
2. **Отдельный User ID**: Демо-аккаунт имеет свой собственный User ID
3. **Не используйте Testnet**: Не создавайте ключи из Testnet демо-торговли, используйте только Mainnet демо-торговлю

## Интеграция с торговым ботом

Демо-торговля автоматически используется, если:
- `base-url` настроен на `https://api-demo.bybit.com`
- API ключи настроены для демо-аккаунта

Торговый бот будет использовать демо-API для всех операций:
- Получение котировок
- Размещение ордеров
- Управление позициями
- Получение баланса

## Отладка

Если возникают проблемы:

1. **Проверьте API ключи**: Убедитесь, что используете ключи из демо-торговли
2. **Проверьте домен**: Должен быть `https://api-demo.bybit.com`
3. **Проверьте логи**: Смотрите консоль для ошибок аутентификации
4. **Проверьте подпись**: Убедитесь, что подпись генерируется правильно

## Документация

Полная документация Bybit Demo Trading API:
https://bybit-exchange.github.io/docs/v5/demo

