# Kepler Android — обновление существующего приложения Google Play

Редакция 2: Plus и In-app purchases полностью исключены из работающего кода по требованию владельца. Перенос подписки и backend проверки покупок больше не нужны.

## Установка

Имя для автоматического распаковщика: `iumrah-android-update-*.zip`. Загрузите архив в корень ветки main обычным push: это запускает apply-update; ручной workflow_dispatch запускает только сборку, не распаковку.

Распаковщик намеренно пропускает `.github/workflows/`. Поэтому `.github/workflows/iumrah-play-release.yml` из этого ZIP добавьте вручную по этому же пути через GitHub или git. Защита распаковщика не изменена.

Распакуйте ZIP в корень Kepler Android с заменой файлов. Основа — kepler-android-main(5).zip от 04.10.2026. Этот ZIP заменяет предыдущий патч Google Play и подходит как поверх исходной версии, так и поверх первого патча. Если вы уже меняли перечисленные файлы после этой основы, перед распаковкой сравните изменения.

Два прежних Kotlin-файла покупки заменены комментариями, чтобы обычная распаковка ZIP убрала их реализацию даже после установки первого патча. Их можно удалить вручную; для сборки этого не требуется. UI и запуск приложения возвращены к версии без Plus. Зависимость Google Play Billing убрана. Нет покупок, восстановления, запросов подписки или платного доступа. Ничего в каталоге Play Console патч не изменяет.

## Идентичность приложения

| Настройка | Значение |
|---|---|
| Release applicationId / package name | com.iumrah.app — точно как в старом Flutter |
| Debug applicationId | com.iumrah.beta — прежний тестовый Kepler |
| Kotlin namespace | com.iumrah.beta — внутренние имена классов, не идентификатор магазина |
| Последняя версия в старом ZIP | 1.0.4 (14) |
| Новая видимая версия по умолчанию | 2.0.3 |
| Новый versionCode | Обязательно задаётся при релизной сборке, выше ВСЕХ кодов в Console |
| Подпись | Действующий upload key существующего приложения |

Сборку загрузить в СУЩЕСТВУЮЩУЮ карточку com.iumrah.app в том же Play Console. Новое приложение создавать не нужно. Новый ключ вместо действующего не генерировать. При Play App Signing Google подписывает APK своим зарегистрированным app signing key; AAB загружается с действующим upload key.

Классы Application, Activity, services, receivers и launcher aliases указаны полными именами в manifest. Переключатель иконок учитывает различие package и namespace. FileProvider использует ${applicationId}.files. Debug остаётся отдельным приложением и не предназначен для обновления установленной версии из Google Play.

## Подготовка ключа — один раз

В исходном Flutter ZIP нет key.properties, keystore и паролей: есть только ссылки на них. Перенести отсутствующий ключ из ZIP нельзя.

В GitHub → Settings → Secrets and variables → Actions добавьте Secrets:
- PLAY_KEYSTORE_BASE64 — содержимое прежнего upload keystore в base64 одной строкой.
- PLAY_STORE_PASSWORD — пароль хранилища.
- PLAY_KEY_ALIAS — alias ключа.
- PLAY_KEY_PASSWORD — пароль ключа.

Добавьте Variable PLAY_UPLOAD_CERT_SHA256 — SHA-256 сертификата **Upload key certificate** из Play Console → App integrity. Это публичный fingerprint; не перепутайте с App signing key certificate.

Workflow сверит сертификат ключа с этим значением и остановится при несовпадении. Если ключ уже хранится в других GitHub Secrets, перенесите значения в указанные имена. При потере upload key его нужно сбросить через Play Console; патч этого не делает.

## Сборка

Actions → iumrah Google Play AAB → Run workflow:
- version_code: максимальный использованный versionCode во всех треках Console + 1. Например 15 ТОЛЬКО если максимум действительно 14.
- version_name: 2.0.3 или желаемая видимая версия.

Workflow запускает unit tests, lintRelease, bundleRelease, сверку подписи ключа, jarsigner verification и bundletool validation. В готовом AAB дополнительно проверяются com.iumrah.app, выбранный versionCode, отсутствие debug/test-only и разрешения/метаданных Billing. Результат — AAB и mapping.txt в artifacts. Автоматической публикации нет.

Локальная сборка требует тех же PLAY_* переменных подписи и явного -PPLAY_VERSION_CODE. Без них release останавливается. Debug не требует релизного ключа.

## Что проверять перед загрузкой

- Максимальный versionCode берётся из Console, не из ZIP. Доступа к Console у этой сборки нет, поэтому автоматически узнать его она не может.
- Firebase, Google OAuth и assetlinks.json должны быть настроены для com.iumrah.app и сертификата Play app signing. IUMRAH_FIREBASE_* secrets, если используются, должны относиться к production Android app. Это влияет на push/вход/ссылки, но не заменяет подпись AAB.
- Сначала загрузите AAB во внутреннее тестирование и обновите старое приложение из Google Play без его удаления. Проверьте запуск, вход, иконку и основные экраны.
- minSdk Kepler остаётся 26: более старые Android новую версию не получат.
- Переход Flutter → native не переносит автоматически Supabase-сессию в новый backend. Может потребоваться новый вход; это отдельный вопрос от идентификатора магазина. Патч не стирает старые данные приложения.

## Проверено в этой среде

Проверены XML, YAML, синтаксис shell/Python в workflow, отсутствие Billing в исходниках и статические ограничения идентичности. Проверена установка патча поверх исходной версии и поверх первого патча. Android-компиляция и обновление на устройстве не выполнены: здесь нет Android SDK/Gradle и действующего ключа. Окончательный результат сборки подтвердит GitHub Actions; принятие обновления — Play Console.

Официальные требования к обновлению: https://support.google.com/googleplay/android-developer/answer/9859350
Подпись: https://developer.android.com/studio/publish/app-signing
