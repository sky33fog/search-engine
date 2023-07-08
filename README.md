
# Lynx search BOT

![](readme_assets/LYNXX.jpg)

## Описание
Проект является итоговой работой курса Java-разработчик 
образовательной платформы Skillbox.
В проекте изначально была реализована часть, отвечающая за
визуализацию (view), моей задачей являлось создание структуры 
представления и хранения данных (model), а также всей логики 
обработки данных (controller) и представления их в слой визуализации.

Проект представляет собой простой поисковый сервис,
осуществляющий поиск информации по введенному запросу в 
содержимом заранее определенных, проиндексированных сайтов.
На уровне визуализации сервис представлен тремя разделами:
- раздел статистики и состояния индексации сайтов;
- раздел запуска и остановки индексации всех сайтов, а 
также индексации отдельно взятой страницы из перечня сайтов, определенных в конфигурационной файле;
- раздел поиска, в котором осуществляется ввод поискового запроса с параметрами поиска, а также вывод результатов поиска.
  
## Технологии применяемые в проекте
- java version "18.0.1.1" 2022-04-22;
- Java(TM) SE Runtime Environment (build 18.0.1.1+2-6);
- Java HotSpot(TM) 64-Bit Server VM (build 18.0.1.1+2-6, mixed mode, sharing);
- MySQL version 8.0.30;
- фреймворк **_Spring_** version 3.0.1;
- фреймворк **_Hibernate_**;
- библиотека **_Lombok_**;
- модуль морфологического анализа **_Lucene Morphology_**;
- система сборки **_Maven_**
  

 ## Техническое описание проекта
  ### Запуск
  Запуск проекта осуществляется через исполняемый файл ***.jar**.

Например:

`java -jar SearchEngine.jar`

  Для работы приложения необходимо подключение к СУБД **Mysql**.

### Настройка
Параметры работы приложение задаются в конфигурационном файле **application.yml**:
- перечень сайтов, на которых будет осуществляться поиск;
- параметры подключения к базе данных;
- параметры, используемые при подключении к страницам при индексации сайтов.

![](readme_assets/lynx2.jpg)