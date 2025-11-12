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

## Подключаем к проекту
**Добавляем в settings.gradle.kts**
```gradle
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}
```

**Добавляем в build.gradle.kts**
```gradle
	dependencies {
	        implementation("com.github.DevAnlim:livesurf-android-sdk:v.1.0.0")
	}
```

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

## Пример использования (Java)
```java
import ru.decpro.livesurfsdk.LiveSurfApi;
import org.json.JSONArray;
import org.json.JSONObject;

public class LiveSurfJavaExample {

    public static void main(String[] args) {
        new Thread(() -> {
            LiveSurfApi api = new LiveSurfApi("ВАШ_API_КЛЮЧ");

            try {
                // 1️⃣ Информация о пользователе
                JSONObject user = (JSONObject) api.getUser();
                System.out.println("Баланс: " + user.getInt("credits") +
                                   " / Режим: " + user.getString("workmode"));

                // 2️⃣ Список категорий
                JSONObject categories = (JSONObject) api.getCategories();
                System.out.println("Доступные категории:");
                if (categories.has("data")) {
                    JSONArray cats = categories.getJSONArray("data");
                    for (int i = 0; i < cats.length(); i++) {
                        JSONObject cat = cats.getJSONObject(i);
                        System.out.println("- " + cat.getInt("id") + ": " + cat.getString("name"));
                    }
                }

                // 3️⃣ Создаём новую группу
                JSONObject groupData = new JSONObject();
                groupData.put("name", "Тестовая группа Java SDK");
                groupData.put("hour_limit", 50);
                groupData.put("day_limit", 1000);
                groupData.put("category", 1);
                groupData.put("language", 1);
                groupData.put("timezone", "Europe/Moscow");
                groupData.put("use_profiles", true);
                groupData.put("geo", new int[]{1, 2});
                JSONArray pages = new JSONArray();
                JSONObject page = new JSONObject();
                page.put("url", new String[]{"https://example.com"});
                page.put("showtime", new int[]{15, 30});
                pages.put(page);
                groupData.put("pages", pages);

                JSONObject newGroup = (JSONObject) api.createGroup(groupData);
                System.out.println("Создана группа ID: " + newGroup.getInt("id"));

                // 4️⃣ Получаем все группы
                JSONObject groups = (JSONObject) api.getGroups();
                System.out.println("Всего групп: " + groups.length());

                // 5️⃣ Клонируем группу
                JSONObject clone = (JSONObject) api.cloneGroup(newGroup.getInt("id"), new JSONObject().put("name", "Копия тестовой группы"));
                System.out.println("Создан клон группы ID: " + clone.getInt("id"));

                // 6️⃣ Добавляем кредиты
                api.addGroupCredits(newGroup.getInt("id"), 100);
                System.out.println("Кредиты успешно зачислены.");

                // 7️⃣ Статистика
                JSONObject stats = (JSONObject) api.getStats(Map.of(
                        "group_id", String.valueOf(newGroup.getInt("id")),
                        "date", "2025-11-11"
                ));
                System.out.println("Статистика за сегодня: " + stats);

                // 8️⃣ Удаляем группу
                api.deleteGroup(newGroup.getInt("id"));
                System.out.println("Группа успешно удалена.");

            } catch (Exception e) {
                System.err.println("⚠️ Ошибка: " + e.getMessage());
            }
        }).start();
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
| `getGroup(id: Int)` | Информация о конкретной группе |
| `updateGroup(id: Int, data: JSONObject)` | Изменение настроек группы |
| `deleteGroup(id: Int)` | Удаление группы |
| `createGroup(data: JSONObject)` | Создание новой группы |
| `cloneGroup(id: Int, data: JSONObject? = null)` | Клонирование группы |
| `addGroupCredits(id: Int, credits: Int)` | Зачисление кредитов группы |

### Страницы
| Метод | Описание |
|-------|----------|
| `getPage(id: Int)` | Информация о конкретной странице |
| `updatePage(id: Int, data: JSONObject)` | Изменение настроек страницы |
| `deletePage(id: Int)` | Удаление страницы |
| `createPage(data: JSONObject)` | Создание новой страницы |
| `clonePage(id: Int)` | Клонирование страницы |
| `movePageUp(id: Int)` | Перемещение страницы вверх |
| `movePageDown(id: Int)` | Перемещение страницы вниз |
| `startPage(id: Int)` | Запуск страницы в работу |
| `stopPage(id: Int)` | Остановка работы страницы |
| `getStats(params: Map<String, String>)` | Статистика показа страницы |

## Лицензия

MIT License — свободное использование, копирование и модификация.
