# HTTPS для Quality Control App: как убрать «Не защищено» на https://devteam7:8082/

## 1. Почему Chrome перечёркивает `https`

Сервер `devteam7:8082` отдавал **старый самоподписанный** сертификат:

```
Subject : CN=localhost, OU=Quality Control, O=BMZ, ...
Issuer  : CN=localhost, ... (сам себе)
SAN     : DNS Name=localhost, IP Address=127.0.0.1      <-- нет имени devteam7
Protocol: TLS 1.2
```

Chrome/Edge считают соединение защищённым только если выполняются оба условия:

1. **Имя сайта совпадает** с именем в сертификате (SAN) — здесь его не было: браузер открывает
   `devteam7`, а сертификат выписан на `localhost`.
2. **Цепочка сертификатов заканчивается доверенным корневым сертификатом** — самоподписанный
   сертификат по умолчанию не доверенный (`NET::ERR_CERT_AUTHORITY_INVALID`).

## 2. Как сделано правильно (схема в этом проекте)

```
        pki\root-ca.p12            (закрытый ключ CA — только у администратора, в git не попадает)
               |  подписывает
               v
   серверный сертификат CN=devteam7
   SAN: devteam7, devteam7.bsw.iron, localhost, 172.16.21.2, 127.0.0.1
               |
               +-- keystore.p12  -> его использует приложение (Tomcat)
               |
               +-- root-ca.crt   -> устанавливается в «Доверенные корневые ЦС» на сервере и на ПК
```

Клиентским машинам не нужны приватные ключи — только публичный `root-ca.crt`.

## 3. Файлы, которые за это отвечают

| Файл | Назначение |
| --- | --- |
| `create-devteam7-certificate.ps1` | Создаёт корневой CA + серверный сертификат, собирает `keystore.p12` с цепочкой. |
| `install-devteam7-cert.ps1` | Ставит `root-ca.crt` в «Доверенные корневые центры сертификации» (сервер + ПК). |
| `trust-root-ca.bat` | То же самое одной кнопкой для обычных пользователей: двойной клик + UAC. `/check` — только проверить. |
| `check-server-certificate.ps1` | Показывает, какой сертификат реально отдаёт сервер, доверенный ли он на этом ПК, отвечает ли приложение. |
| `http-probe.ps1` | «Сырая» диагностика по сети: открыт ли порт, сертификат, HTTP-ответ. |
| `start-server.bat` | Запуск jar на сервере с кейстором из папки `cert` (вне jar). |
| `src/main/resources/keystore/keystore.p12` | Кейстор по умолчанию (используется, если не задан `SERVER_SSL_KEY_STORE`). |
| `src/main/resources/keystore/root-ca.crt` | Публичный корневой сертификат для раздачи на ПК. |

## 4. Шаг 1. Выпуск сертификатов

Выполнить один раз (там, где есть JDK `keytool`, например на рабочем ПК разработчика):

```powershell
cd D:\Vadim\pet-projects\quality-controll-app
.\create-devteam7-certificate.ps1
```

Скрипт создаст:

* `pki\root-ca.p12`, `pki\root-ca.crt` — корневой центр сертификации (10 лет);
* `src\main\resources\keystore\keystore.p12` — серверный кейстор с цепочкой «сертификат → CA»;
* `src\main\resources\keystore\server.crt` — серверный сертификат (для справки);
* `src\main\resources\keystore\root-ca.crt` — публичный корневой сертификат.

Полезные параметры:

```powershell
# Другие имена/адреса, которые будут работать
.\create-devteam7-certificate.ps1 -AdditionalDnsNames qc.bsw.iron -AdditionalIpAddresses 172.16.21.2

# Свой пароль кейстора
.\create-devteam7-certificate.ps1 -KeyStorePassword 'SlozhnyjParol123'

# Полностью пересоздать CA (после этого root-ca.crt нужно заново раздать на все ПК!)
.\create-devteam7-certificate.ps1 -NewRootCa
```

Повторный запуск без `-NewRootCa` переиспользует существующий CA — при продлении сертификата
на клиентских ПК ничего переустанавливать не нужно.

## 5. Шаг 2. Развернуть на сервере devteam7

Собрать jar:

```powershell
.\mvnw.cmd -DskipTests clean package
```

Скопировать на сервер (например, в `C:\quality-control-app\`):

```
quality-control-app-0.0.1-SNAPSHOT.jar
start-server.bat
.env                                  <- реальные DB_URL / DB_USERNAME / DB_PASSWORD / *_KEY
cert\keystore.p12                     <- из src\main\resources\keystore\
cert\root-ca.crt                      <- из src\main\resources\keystore\
src\main\java\.. (не нужно, только файлы выше)
```

Запускать приложение **через `start-server.bat`**: он сам находит кейстор вне jar и подставляет его в переменные окружения (`SERVER_SSL_KEY_STORE` и т.д.).

```bat
set SERVER_SSL_KEY_STORE=file:./cert/keystore.p12
set SERVER_SSL_KEY_STORE_PASSWORD=changeit
set SERVER_SSL_KEY_ALIAS=qualitycontrollapp
java -jar quality-control-app-0.0.1-SNAPSHOT.jar
```

> Важно: раньше на сервере оставался **старый jar/кейстор**, поэтому отдавался сертификат
> `CN=localhost`. После обновления обязательно проверьте сервер:
>
> ```powershell
> .\check-server-certificate.ps1 -ServerName devteam7
> ```
>
> В выводе должно быть `Subject: CN=devteam7`, `Issuer: CN=BMZ Quality Control Root CA`
> и `SAN: DNS Name=devteam7, ...`.

### 5.1. Где `start-server.bat` ищет кейстор

| Расположение файла | Что будет подставлено |
| --- | --- |
| `cert\keystore.p12` | `SERVER_SSL_KEY_STORE=file:./cert/keystore.p12` |
| `keystore.p12` рядом с jar | `SERVER_SSL_KEY_STORE=file:./keystore.p12` |
| `src\main\resources\keystore\keystore.p12` (когда приложение запускают из исходников) | `SERVER_SSL_KEY_STORE=file:./src/main/resources/keystore/keystore.p12` |
| внешнего кейстора нет | переменные не задаются — используется кейстор **внутри jar** (актуальный на момент сборки) |

Если переменные `SERVER_SSL_KEY_STORE` / `SERVER_SSL_KEY_STORE_PASSWORD` / `SERVER_SSL_KEY_ALIAS`
уже заданы (в переменных среды Windows или в `.env` рядом с jar), скрипт их не переопределяет.

> Свежий jar уже содержит правильный кейстор, поэтому сертификат будет корректным даже без
> внешнего файла — внешний нужен только для того, чтобы продлевать сертификат без пересборки jar.


## 6. Шаг 3. Сделать сертификат доверенным (главный шаг!)

### 6.1. На сервере devteam7 (PowerShell от имени администратора)

```powershell
copy \\Vadim-plus\...\root-ca.crt C:\quality-control-app\cert\   # или скопировать любым способом
cd C:\quality-control-app
.\install-devteam7-cert.ps1 -CertificatePath .\cert\root-ca.crt
```

### 6.2. На каждом ПК пользователя (PowerShell от имени администратора)

```powershell
# вариант 1: сертификат рядом со скриптом

### 6.4. Если Chrome всё ещё пишет «Подключение не защищено»

Сертификат на сервере уже корректен (проверяется скриптом `check-server-certificate.ps1`),
значит проблема на стороне клиента. Действуйте **на том ПК, где видно предупреждение**:

1. **Проверьте, доверяет ли этот ПК сертификату:**

   ```powershell
   .\check-server-certificate.ps1 -ServerName devteam7
   ```

   * «ИТОГ: всё в порядке» → причина в браузере (пункт 3);
   * «сертификат не считается доверенным» → установите корневой сертификат (пункт 2).

2. **Установите корневой сертификат** (или GUI: правый клик по `root-ca.crt` → «Установить сертификат»
   → «Локальный компьютер» → «Доверенные корневые центры сертификации»):

   ```powershell
   # от имени администратора — для всех пользователей ПК
   .\install-devteam7-cert.ps1 -CertificatePath D:\projects_stpc2\value_quality_control_project\root-ca.crt -RemoveLegacy
   ```

3. **Полностью перезапустите браузер** (Chrome кэширует ошибку сертификата до перезапуска):
   закройте все окна, проверьте, что в диспетчере задач нет процессов `chrome.exe`,
   либо откройте `chrome://restart`.

4. **Посмотрите точную причину в Chrome** (если предупреждение осталось):
   значок в адресной строке → «Соединение не защищено» → «Сертификат (Недействителен)»,
   либо F12 → вкладка **Security**. Коды:

   | Ошибка Chrome | Что означает |
   | --- | --- |
   | `NET::ERR_CERT_AUTHORITY_INVALID` | корневой сертификат не установлен на этом ПК (пункт 2) |
   | `NET::ERR_CERT_COMMON_NAME_INVALID` | сертификат выписан не на то имя (см. `-AdditionalDnsNames`) |

5. **Сбросьте состояние HSTS** (приложение отдаёт заголовок `Strict-Transport-Security`):
   `chrome://net-internals/#hsts` → в разделе *Delete domain security policies* введите `devteam7` → Delete.

6. **Уберите старые самоподписанные сертификаты** предыдущих версий
   (в хранилище могут оставаться `CN=localhost` / `CN=devteam7`, выписанные сами себе):

   ```powershell
   # посмотреть, что есть
   Get-ChildItem Cert:\LocalMachine\Root, Cert:\CurrentUser\Root |
       Where-Object { $_.Subject -eq $_.Issuer -and $_.Subject -match 'CN=(localhost|devteam7)' } |
       Select-Object Subject, Thumbprint, NotAfter

   # удалить (часть сертификатов Windows не даёт удалять из скрипта — тогда через certmgr.msc)
   Get-ChildItem Cert:\LocalMachine\Root, Cert:\CurrentUser\Root |
       Where-Object { $_.Subject -eq $_.Issuer -and $_.Subject -match 'CN=(localhost|devteam7)' } |
       Remove-Item
   ```

\\devteam7\c$\quality-control-app\install-devteam7-cert.ps1
# в папке должен лежать root-ca.crt

# вариант 2: явно указать файл сертификата
.\install-devteam7-cert.ps1 -CertificatePath '\\devteam7\c$\quality-control-app\cert\root-ca.crt'

# вариант 3: без прав администратора (только для текущего пользователя)
.\install-devteam7-cert.ps1 -CertificatePath .\root-ca.crt -CurrentUser
```

Без параметров скрипт ставит сертификат в хранилище **LocalMachine** (для всех пользователей ПК),
если запущен от администратора.

### 6.3. Массово через групповую политику (рекомендуется для домена)

1. `gpmc.msc` → создать/выбрать объект групповой политики для OU с компьютерами.
2. `Конфигурация компьютера` → `Политики` → `Конфигурация Windows` → `Параметры безопасности`
   → `Политики открытого ключа` → `Доверенные корневые центры сертификации`.
3. Импортировать `root-ca.crt`.
4. На клиентах: `gpupdate /force` (или дождаться обновления политик).

После этого на всех доменных ПК сертификат доверенный без запуска скриптов.

### 6.5. Как раздать сертификат на другие ПК

**На других ПК «просто открыть ссылку» не получится без предупреждения.** Доверие к сертификату
хранится на каждой машине отдельно: где `root-ca.crt` не установлен, Chrome/Edge покажут
«Подключение не защищено» (страница откроется только после «Дополнительно → Перейти на сайт»).
При этом трафик всё равно шифруется — предупреждение означает лишь, что браузер не может
проверить, что сервер действительно тот, за кого себя выдаёт.

Варианты установки (в порядке удобства):

1. **`trust-root-ca.bat`** — положите рядом с ним `root-ca.crt` и запустите двойным кликом
   (подтвердить UAC). Годится и как скрипт входа/запуска в групповой политике.
   Проверка без установки: `trust-root-ca.bat /check`
2. **`install-devteam7-cert.ps1`** — см. п. 6.1–6.2 (больше возможностей, тот же результат).
3. **Групповая политика** — п. 6.3: один раз настраивается для всего домена.
4. **Корпоративный УЦ** (раздел 8) — раздавать вообще ничего не нужно.

После установки обязательно **закрыть и снова открыть браузер** (Chrome кэширует ошибку
сертификата до перезапуска).

**Firefox** использует собственное хранилище сертификатов (не хранилище Windows). Либо разрешите
ему брать сертификаты из Windows: `about:config` → `security.enterprise_roots.enabled = true`
(или политика `Certificates.ImportEnterpriseRoots = true`), либо импортируйте `root-ca.crt`
вручную: Настройки → Приватность и защита → Сертификаты → Просмотр сертификатов →
Центры сертификации → Импорт.

**Мобильные устройства** (Android/iOS) требуют ручной установки сертификата в настройках
устройства — если с телефонов сайт не нужен, можно ничего не делать.

Проверка на любом ПК после установки:

```powershell
.\check-server-certificate.ps1 -ServerName devteam7
# в "Итоге проверки" строка "сертификат доверенный на этом ПК : ДА"
```


## 7. Шаг 4. Проверка

```powershell
# с рабочего ПК: что отдаёт сервер и доверяет ли клиент
.\check-server-certificate.ps1 -ServerName devteam7
```

Ожидаемый результат:

```
Subject   : CN=devteam7, OU=Quality Control, O=BMZ, L=Zhlobin, S=Brest, C=BY
Issuer    : CN=BMZ Quality Control Root CA, ...
SAN       : DNS Name=devteam7, DNS Name=devteam7.bsw.iron, DNS Name=localhost, IP Address=172.16.21.2, ...
ИТОГ: всё в порядке — браузер должен показывать защищённое соединение.
```

В браузере:

1. Полностью закрыть браузер (в Chrome можно `chrome://restart`).
2. Открыть `https://devteam7:8082/`.
3. Замок в адресной строке → «Соединение защищено» → сертификат выдан
   `BMZ Quality Control Root CA`.

### 7.1. Что смотреть в выводе скрипта

| Строка в «Итоге проверки» | Значит |
| --- | --- |
| `имя 'devteam7' есть в сертификате : ДА` | сертификат выписан на то имя, по которому открывают сайт |
| `сертификат доверенный на этом ПК : ДА` | корневой сертификат установлен на этом компьютере |
| `срок действия : в порядке` | сертификат не истёк |
| `приложение отвечает по HTTPS : ДА (200)` | сервер запущен и отдаёт страницу входа |
| `смешанный контент : нет` | страница не подгружает ресурсы по `http://` |

Если вывод начинается с `Порт devteam7:8082 ЗАКРЫТ` — приложение на сервере не запущено,
это не проблема сертификата (запустите `start-server.bat`).

Эталонный вывод (проверено локально на `https://localhost:8082/`):

```
Subject      : CN=devteam7, OU=Quality Control, O=BMZ, L=Zhlobin, S=Brest, C=BY
Issuer       : CN=BMZ Quality Control Root CA, OU=Quality Control, O=BMZ, ...
SAN          : DNS Name=devteam7, DNS Name=devteam7.bsw.iron, DNS Name=localhost, IP Address=172.16.21.2, IP Address=127.0.0.1

Цепочка сертификатов:
  -> CN=devteam7, ...
  -> CN=BMZ Quality Control Root CA, ...

Корневой сертификат (6E7ABEF2...):
  LocalMachine\Root : нет
  CurrentUser\Root  : установлен

Ответ приложения (GET /login):
  HTTP-статус : 200
  Смешанный контент: нет

=== Итог проверки ===
  имя 'localhost' есть в сертификате : ДА
  сертификат доверенный на этом ПК     : ДА
  срок действия                        : в порядке
  приложение отвечает по HTTPS         : ДА (200)
  смешанный контент                    : нет

ИТОГ: всё в порядке — браузер должен показывать защищённое соединение.
```

### 7.2. Проверка без скриптов

```powershell
# 1. Что лежит в кейсторе приложения (цепочка должна быть из 2 сертификатов)
keytool -list -v -keystore cert\keystore.p12 -storepass changeit

# 2. Что сервер отдаёт «наружу» + проверка цепочки средствами Windows
#    (имя devteam7 должно быть в SAN, Issuer = BMZ Quality Control Root CA)

# 3. Где установлен корневой сертификат
Get-ChildItem Cert:\LocalMachine\Root, Cert:\CurrentUser\Root |
    Where-Object { $_.Subject -like '*BMZ Quality Control Root CA*' } |
    Select-Object Subject, Thumbprint, NotAfter

# 4. Через OpenSSL (если установлен)
openssl s_client -connect devteam7:8082 -servername devteam7 -showcerts
```

В браузере (Chrome/Edge):

1. Значок слева от адреса → «Соединение защищено» → «Сертификат действителен»:
   «Выдан для: devteam7», «Кем выдан: BMZ Quality Control Root CA».
2. `F12` → вкладка **Security** → «This page is secure (valid HTTPS)».
3. `http://devteam7:8082/` должен сам переадресовать на `https://` (то есть http-версии у сайта нет).


## 8. Альтернатива (лучший вариант): сертификат от корпоративного УЦ

Если `devteam7` входит в домен `bsw.iron` и есть корпоративный центр сертификации (AD CS),
лучше выпускать сертификат там. **Корневой сертификат корпоративного УЦ уже установлен
на всех доменных ПК автоматически** (при вводе компьютера в домен), поэтому никакая
раздача `root-ca.crt` через GPO и установка на клиентов не нужны — на любом компьютере
браузер сразу покажет защищённое соединение.

### 8.1. Что запросить у ИТ

Сертификат по шаблону **Web Server** (назначение *Server Authentication*), экспортированный
**вместе с закрытым ключом** в файл `.pfx` (пароль передать отдельно), со свойствами:

| Параметр | Значение |
| --- | --- |
| Subject (CN) | `devteam7` |
| SAN (доп. имена) | `devteam7`, `devteam7.bsw.iron` |
| Алгоритм ключа | RSA (2048+ бит) |
| Срок действия | 1 год и более |

### 8.2. Как применить полученный файл

Скрипт проверяет сертификат (закрытый ключ, SAN, назначение) и пересобирает его
в `cert\keystore.p12` с тем alias'ом (`qualitycontrollapp`) и паролем (`changeit`),
которые ожидает приложение — на сервере ничего настраивать не нужно:

```powershell
powershell -ExecutionPolicy Bypass -File import-corporate-cert.ps1 ^
    -PfxPath C:\temp\devteam7.pfx -PfxPassword '<пароль от pfx>'
```

1. Получившийся `cert\keystore.p12` скопировать на сервер вместо старого.
2. Перезапустить приложение (`start-server.bat`).
3. Проверить с любого ПК: `.\check-server-certificate.ps1 -ServerName devteam7`.

Скрипты `create-devteam7-certificate.ps1` / `install-devteam7-cert.ps1` в этом случае
нужны только для тестового стенда (если корпоративного УЦ нет).

## 9. Диагностика

| Симптом в Chrome | Причина | Что делать |
| --- | --- | --- |
| `NET::ERR_CERT_AUTHORITY_INVALID` | цепочка не доверенная | установить `root-ca.crt` (`install-devteam7-cert.ps1`) |
| В консоли `[ERROR] ... cert\keystore.p12` при запуске | запущен старый `start-server.bat`, а кейстора в папке `cert` нет | скопировать новый `start-server.bat` (он ищет кейстор и рядом с jar) либо положить `keystore.p12` в `cert\` |
| Сертификат всё ещё `CN=localhost` после замены файлов | не был остановлен старый процесс приложения | остановить `java.exe` (диспетчер задач) и запустить приложение заново |
| `NET::ERR_CERT_COMMON_NAME_INVALID` | в SAN нет имени, которое набрано в браузере | перевыпустить сертификат с нужным именем/перезапустить сервер с новым кейстором |
| «Срок действия сертификата истёк» | истёк validity | `.\create-devteam7-certificate.ps1` и обновить `cert\keystore.p12` на сервере |
| Скрипт проверки говорит ОК, а браузер ругается | браузер закэшировал старое состояние, либо открыт не тот адрес | `chrome://restart`; проверить, что адрес `https://devteam7:8082/` |
| «Не защищено» остаётся на одной странице | смешанный контент (картинка/скрипт по http) | проверить консоль браузера (F12) |

Полезные команды:

```powershell
# посмотреть установленные корневые сертификаты проекта
Get-ChildItem Cert:\LocalMachine\Root | Where-Object { $_.Subject -like '*BMZ Quality Control*' }

# посмотреть сертификат в кейсторе
keytool -list -v -keystore cert\keystore.p12 -storepass changeit
```

## 10. Пароль кейстора

По умолчанию используется `changeit` (как и было в проекте). Для реальной эксплуатации
пароль лучше сменить:

1. выпустить сертификаты с новым паролем:
   `.\create-devteam7-certificate.ps1 -KeyStorePassword '<новый пароль>'`;
2. на сервере задать переменную окружения `SERVER_SSL_KEY_STORE_PASSWORD` (или строку в `.env`)
   с этим паролем — в `application.properties` ничего править не нужно.

Скрипты и кейсторы с приватными ключами (`pki/`, `keystore.p12`) в git не попадают
(см. `.gitignore`).

