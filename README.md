# Confluence Data Center MCP Server

Безопасный MCP-сервер на Spring Boot для ограниченной работы LLM с Confluence Data Center от имени пользователя. Все REST-запросы используют Personal Access Token (PAT) текущего пользователя.

## Возможности

Сервер публикует ровно семь MCP tools:

| Tool | Назначение |
|---|---|
| `search_pages` | Полнотекстовый поиск только опубликованных страниц; возвращает title, excerpt и компактные metadata без body. |
| `get_page` | Chunked-чтение страницы как Markdown или исходного Confluence STORAGE XHTML. |
| `list_page_children` | Непосредственные дочерние страницы, один уровень, в порядке Confluence. |
| `get_page_ancestors` | Цепочка предков от корня до непосредственного parent. |
| `list_spaces` | Доступные global/current Spaces с опциональным точным фильтром по key. |
| `list_page_attachments` | Только metadata вложений. |
| `create_page` | Создание опубликованной страницы непосредственно под настроенным parent. |

Произвольные CQL, REST URL, статусы и рекурсивная выгрузка дерева не допускаются и не реализованы.

## Требования и запуск

- Java 21;
- доступ по сети к Confluence Data Center;
- PAT с нужными правами чтения и создания страницы под разрешённым parent.

Пример переменных окружения:

```bash
CONFLUENCE_BASE_URL=https://confluence.company.ru
CONFLUENCE_TOKEN=replace-with-your-personal-access-token
CONFLUENCE_PARENT_PAGE_ID=123456789
```

## Конфигурация

Все параметры привязаны к type-safe `ConfluenceProperties` и проверяются при запуске:

```yaml
confluence:
  base-url: https://confluence.company.ru
  token: ${CONFLUENCE_TOKEN}
  connect-timeout: 5s
  read-timeout: 30s
  read:
    default-max-chars: 20000
    max-chars: 50000
  write:
    parent-page-id: "123456789"
    max-content-size: 1MB
```

`max-content-size` имеет default 1 MiB и может быть изменён. Размер считается по UTF-8 bytes, а не по числу Java-символов. `get_page` по умолчанию возвращает до 20 000 символов и никогда не превышает серверный предел 50 000 за вызов.

HTTP-клиент отправляет `Authorization: Bearer <PAT>` и `Accept: application/json`; POST дополнительно использует `Content-Type: application/json`. Настроены connect/read timeout. Автоматического retry для POST нет.

## Чтение и преобразование страниц

`get_page` поддерживает:

- `MARKDOWN` — Confluence STORAGE сначала обрабатывается Jsoup-препроцессором для `ac:*`/`ri:*`, затем преобразуется библиотекой Flexmark HTML-to-Markdown;
- `STORAGE` — исходный `body.storage.value` без преобразования, в том числе для изучения страницы-шаблона и fallback.

Поддерживаются обычные headings, paragraphs, списки, bold/italic, links, tables, code/pre и macros `code`, `info`, `note`, `warning`, `tip`, `expand`. Code macro преобразуется в fenced block с языком, когда он указан. Неизвестный macro сохраняется как заметный placeholder вместе с извлекаемым содержимым и warning. Если преобразование страницы не удалось, ответ имеет `conversionStatus=FAILED` и предлагает повторить запрос с `format=STORAGE`.

Большие страницы читаются через `offset`/`nextOffset`. Первый вызов последовательности должен использовать `offset=0`. Сервер запоминает номер версии и отклоняет последующий chunk, если страница изменилась; чтение нужно начать заново с нулевого offset.

## Write security model

MCP может только создавать новые страницы. Update, delete и move отсутствуют. Страница всегда создаётся как `type=page`, `status=current`, `representation=storage` и только как непосредственный ребёнок `confluence.write.parent-page-id`.

LLM не может передать parent или Space. Перед каждой операцией создания сервер заново читает настроенный parent, проверяя его доступность, и получает Space key из фактического ответа Confluence. Отдельного write `spaceKey` или `expected-space-key` нет.

`create_page` принимает только `title` и Confluence STORAGE XHTML `content`. Выполняются базовые проверки непустых значений и UTF-8 byte limit; валидность STORAGE окончательно проверяет Confluence, а его безопасное диагностическое сообщение возвращается вызывающей модели.

Если страница с запрошенным заголовком уже существует в Space, MCP автоматически пытается использовать заголовки `1 - title`, `2 - title` и т.д., чтобы не потерять сгенерированный результат. Повтор выполняется только для распознанного duplicate-title ответа и ограничен 100 переименованиями.

Создание страницы в MVP не является идемпотентным. При timeout или другом неопределенном результате POST страница могла быть создана. MCP не делает автоматический retry. Перед повторной попыткой необходимо проверить Confluence вручную, иначе возможно создание дубликата.

## Ограничение вложений

В MVP поддерживается только получение метаданных вложений. Загрузка, чтение, извлечение текста и анализ содержимого вложений не поддерживаются.

Сервер не вызывает download URL или `/extractedtext` и не предоставляет LLM tool для скачивания произвольного URL.

## Не поддерживается в MVP

- update, delete и move страниц;
- drafts и публикация drafts;
- чтение версий, comments, labels, permissions/restrictions management;
- чтение или анализ содержимого attachments;
- произвольные REST-запросы и произвольный CQL;
- recursive page-tree download;
- idempotency keys и автоматическое восстановление неопределённого POST.

Для read tools ответы Confluence 403 и 404 нормализуются в одинаковые безопасные сообщения, чтобы не раскрывать существование закрытого ресурса.
