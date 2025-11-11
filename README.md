# LiveSurf Android SDK (Kotlin)

Этот репозиторий содержит Android-библиотеку на Kotlin.

- PHP SDK для LiveSurf API (https://api.livesurf.ru/).
- Документация API (https://livesurf.ru/api_documentation.html)
- Получить API-ключ (https://livesurf.ru/api)

## Возможности
- Авторизация по API-ключу через заголовок `Authorization`
- HTTP: GET / POST / PATCH / DELETE
- Контроль лимита запросов (по умолчанию 10 запросов/сек)
- Повторы при 429 / 5xx с экспоненциальным бэкоффом и джиттером
- Все методы — `suspend` (вызывайте из корутин)

## Как открыть
1. Скачать и распаковать архив.
2. Открыть проект в Android Studio (File → Open -> выберите папку).
3. Build → Make Project. Модуль `livesurf-android-sdk` сгенерирует AAR.

## Пример использования (Kotlin)
```kotlin
import ru.decpro.livesurfsdk.LiveSurfApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

fun exampleLiveSurfKotlin() {
    CoroutineScope(Dispatchers.IO).launch {
        val api = LiveSurfApi("ВАШ_API_КЛЮЧ")

        try {
            // 1️⃣ Информация о пользователе
            val user = api.getUser() as JSONObject
            println("Баланс: ${user.optInt("credits")} / Режим: ${user.optString("workmode")}")

            // 2️⃣ Получаем список категорий
            val categories = api.getCategories() as JSONObject // Если API возвращает массив, то JSONArray
            println("Доступные категории:")
            if (categories.has("data")) {
                val cats = categories.getJSONArray("data")
                for (i in 0 until cats.length()) {
                    val cat = cats.getJSONObject(i)
                    println("- ${cat.getInt("id")}: ${cat.getString("name")}")
                }
            }

            // 3️⃣ Создаём новую группу
            val newGroup = api.createGroup(JSONObject().apply {
                put("name", "Тестовая группа Kotlin SDK")
                put("hour_limit", 50)
                put("day_limit", 1000)
                put("category", 1)
                put("language", 1)
                put("timezone", "Europe/Moscow")
                put("use_profiles", true)
                put("geo", listOf(1, 2))
                put("pages", listOf(
                    mapOf("url" to listOf("https://example.com"), "showtime" to listOf(15, 30))
                ))
            }) as JSONObject
            println("Создана группа ID: ${newGroup.getInt("id")}")

            // 4️⃣ Получаем список всех групп
            val groups = api.getGroups() as JSONObject
            println("Всего групп: ${groups.length()}")

            // 5️⃣ Клонируем группу
            val clone = api.cloneGroup(newGroup.getInt("id"), JSONObject().apply {
                put("name", "Копия тестовой группы")
            }) as JSONObject
            println("Создан клон группы ID: ${clone.getInt("id")}")

            // 6️⃣ Добавляем кредиты в группу
            api.addGroupCredits(newGroup.getInt("id"), 100)
            println("Кредиты успешно зачислены.")

            // 7️⃣ Получаем статистику
            val stats = api.getStats(mapOf(
                "group_id" to newGroup.getInt("id").toString(),
                "date" to "2025-11-11"
            )) as JSONObject
            println("Статистика за сегодня: $stats")

            // 8️⃣ Удаляем группу
            api.deleteGroup(newGroup.getInt("id"))
            println("Группа успешно удалена.")

        } catch (e: Exception) {
            println("⚠️ Ошибка: ${e.message}")
        }
    }
}
```

## Доступные методы API

### Пользователь
| Метод | Описание |
|-------|----------|
| `getUser()` | Получение информации о пользователе |
| `setAutoMode()` | Включение автоматического режима (АРК) |
| `setManualMode()` | Включение ручного режима работы |

### Категории, страны, языки, источники
| Метод | Описание |
|-------|----------|
| `getCategories()` | Список возможных категорий |
| `getCountries()` | Список возможных стран |
| `getLanguages()` | Список доступных языков |
| `getSourcesAd()` | Список рекламных площадок |
| `getSourcesMessengers()` | Список мессенджеров |
| `getSourcesSearch()` | Список поисковых систем |
| `getSourcesSocial()` | Список социальных сетей |

### Группы
| Метод | Описание |
|-------|----------|
| `getGroups()` | Информация о всех добавленных группах |
| `getGroup(int $groupId)` | Информация о конкретной группе |
| `updateGroup(int $groupId, array $data)` | Изменение настроек группы |
| `deleteGroup(int $groupId)` | Удаление группы |
| `createGroup(array $data)` | Создание новой группы |
| `cloneGroup(int $groupId, string $name)` | Клонирование группы |
| `addGroupCredits(int $groupId, int $credits)` | Зачисление кредитов группы |

### Страницы
| Метод | Описание |
|-------|----------|
| `getPage(int $pageId)` | Информация о конкретной странице |
| `updatePage(int $pageId, array $data)` | Изменение настроек страницы |
| `deletePage(int $pageId)` | Удаление страницы |
| `createPage(array $data)` | Создание новой страницы |
| `clonePage(int $pageId)` | Клонирование страницы |
| `movePageUp(int $pageId)` | Перемещение страницы вверх |
| `movePageDown(int $pageId)` | Перемещение страницы вниз |
| `startPage(int $pageId)` | Запуск страницы в работу |
| `stopPage(int $pageId)` | Остановка работы страницы |
| `getStats(array $params)` | Статистика показа страницы |

## Лицензия

MIT License — свободное использование, копирование и модификация.
