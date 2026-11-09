# JAVA

Откройте папку `java` в VS Code с расширением Extension Pack for Java. `lab5`, `lab6` и `kurs` импортируются как отдельные Maven-проекты. Для `lab1`–`lab4` и примеров `eth` настройте пути исходников локально. Для лабораторных подходит JDK 17, для `kurs` нужен JDK 21 или новее.

Если VS Code уже открывал этот проект, выполните `Java: Clean Java Language Server Workspace` из палитры команд и дождитесь повторного импорта.

Чтобы проверить UDP, запустите `UdpReceiver.java` через `Run Java`, затем так же запустите `UdpSender.java`. Получатель ждёт пакет на порту 5000 и завершится после его получения.

Сборка лабораторных из корня репозитория:

```sh
mvn -f lab5/pom.xml package
mvn -f lab6/pom.xml package
```

Готовые JAR находятся в `lab5/target/lab5.jar` и `lab6/build/lab6.jar`.
