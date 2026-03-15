# StudentBot для MAX

## Что внутри
- Java 17 + Spring Boot
- SQLite для хранения мероприятий и заявок
- Все кнопки — inline
- Админ-панель прямо в чате

## Переменные окружения
- `MAX_BOT_TOKEN` — токен бота MAX
- `MAX_API_BASE` — базовый URL API (по умолчанию `https://platform-api.max.ru`)
- `DB_PATH` — путь к SQLite (по умолчанию `/data/bot.db`)
- `MAX_LONG_POLLING_ENABLED` — включить long polling (по умолчанию `true`)
- `MAX_LONG_POLLING_TIMEOUT` — таймаут long polling в секундах (по умолчанию `30`)
- `MAX_LONG_POLLING_LIMIT` — лимит апдейтов за запрос (по умолчанию `100`)
- `MAX_LONG_POLLING_TYPES` — типы апдейтов (по умолчанию `message_created,message_callback`)

## Локальный запуск
```bash
mvn -q -DskipTests package
java -jar target/studentbot.jar
```

## Docker
```bash
docker build -t studentbot .
```

```bash
docker run -d \
  --name studentbot \
  -p 8080:8080 \
  -e MAX_BOT_TOKEN=YOUR_TOKEN \
  -e DB_PATH=/data/bot.db \
  -v "$(pwd)/data:/data" \
  studentbot
```

## Long polling
Бот работает через long polling (`GET /updates`) и не использует вебхуки. Если ранее был включён webhook, удалите подписку в MAX.
- `GET /health` — проверка живости

## Админ-панель
Отправьте в чат пароль `Teacher2026`, чтобы открыть админ-панель.
