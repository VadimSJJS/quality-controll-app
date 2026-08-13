@echo off
echo Добавление тестовых данных через API...
curl -X POST http://localhost:8080/api/seed/test-data
echo.
echo.
echo Готово! Обновите страницу журнала несоответствий.
