SmartThingCloud - облачное приложение для доступа к маршрутизатору SmartThingGateway из глобальной сети интернет
Версия Java - 17.

# Запуск
Запуск приложения производится с помощью docker compose.
1. Необходимо создать файл `.env` по примеру `.env-example` со своими значениями;
2. Собрать образ `docker compose build`;
3. Запустить с помощью `docker compose up -d`.