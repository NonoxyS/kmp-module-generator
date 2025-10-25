# KMP Module Generator

<!-- Plugin description -->
A powerful plugin for IntelliJ IDEA and Android Studio that automates Kotlin Multiplatform and Android module creation using customizable FreeMarker templates.

**Key Features:**
- FreeMarker-based templates - create templates as simple text files
- Visual template wizards - intuitive UI for creating and editing templates
- Automatic settings.gradle updates
- Preview module structure before generation
- Configurable template storage for team sharing
<!-- Plugin description end -->

## 🚀 Основные возможности

- **FreeMarker шаблоны** - создавайте и редактируйте шаблоны как обычные файлы
- **Визуальный UI** - создавайте и редактируйте шаблоны через удобный интерфейс
- **Автоматическая интеграция** - обновление settings.gradle
- **Предпросмотр** - см. структуру перед генерацией
- **Настраиваемое хранилище** - используйте общую папку шаблонов для всех проектов

## 📦 Установка

### Из JetBrains Marketplace
```
Settings → Plugins → Marketplace → "KMP Module Generator" → Install
```

### Из исходников
```bash
./gradlew buildPlugin
# Файл будет в build/distributions/
```

## 🎯 Быстрый старт

1. **Создать свой первый шаблон**
   ```
   Правый клик на папке → New → Generate Module → New Template...
   ```

2. **Сгенерировать модуль из шаблона**
   ```
   Правый клик на папке → New → Generate Module → From Template...
   ```

3. **Редактировать существующий шаблон**
   ```
   Правый клик на папке → New → Generate Module → Edit Template...
   ```

## ⚙️ Настройка папки с шаблонами

По умолчанию шаблоны хранятся в `.idea/kmp-templates/` вашего проекта.

**Изменить папку:**
```
Settings → Tools → KMP Module Templates
→ ☑ Use custom template folder
→ Выберите папку
→ Нажмите "Open Templates Folder" для быстрого доступа
```

**Зачем это нужно:**
- Общая папка для всех проектов
- Шаблоны вне репозитория
- Подключение внешней библиотеки шаблонов
- Удобное управление через проводник

## 📝 Создание своих шаблонов

### Структура шаблона

```
<папка-с-шаблонами>/my-template/
├── template.xml           # Конфигурация
├── root/                  # Файлы для генерации
│   ├── build.gradle.kts.ftl
│   └── src/
│       └── main/
│           └── kotlin/
│               └── ${packagePath}/
│                   └── MyClass.kt.ftl
```

### template.xml

```xml
<?xml version="1.0"?>
<template>
    <id>my-template</id>
    <name>My Custom Template</name>
    <description>What this template does</description>
    
    <parameters>
        <parameter name="moduleName">
            <displayName>Module Name</displayName>
            <description>Name of the module</description>
            <type>TEXT</type>
            <required>true</required>
        </parameter>
        
        <parameter name="packageName">
            <displayName>Package Name</displayName>
            <type>PACKAGE</type>
            <required>true</required>
        </parameter>
        
        <parameter name="useCompose">
            <displayName>Use Jetpack Compose</displayName>
            <type>BOOLEAN</type>
            <default>true</default>
        </parameter>
    </parameters>
</template>
```

### Файлы .ftl

FreeMarker шаблоны с полной поддержкой условий и циклов:

**build.gradle.kts.ftl:**
```kotlin
plugins {
    kotlin("android")
<#if useCompose == "true">
    id("org.jetbrains.compose")
</#if>
}

android {
    namespace = "${packageName}"
    compileSdk = 34
    
<#if useCompose == "true">
    buildFeatures {
        compose = true
    }
</#if>
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
<#if useCompose == "true">
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
</#if>
}
```

**MyClass.kt.ftl:**
```kotlin
package ${packageName}

<#if useCompose == "true">
import androidx.compose.runtime.Composable

@Composable
fun MyScreen() {
    // Compose UI here
}
<#else>
class MyClass {
    fun doSomething() {
        println("Hello from ${moduleName}!")
    }
}
</#if>
```

## 🎨 Возможности FreeMarker

### Переменные
```
${variableName}       - Вставка переменной
${packagePath}        - Автоматически из packageName (com/example/app)
```

### Условия
```ftl
<#if condition == "true">
    // код если true
<#elseif otherCondition>
    // код если другое
<#else>
    // иначе
</#if>
```

### Циклы
```ftl
<#list items as item>
    implementation("${item}")
</#list>
```

### Функции
```ftl
${moduleName?cap_first}           - Первая буква заглавная
${packageName?replace(".", "/")}  - Замена символов
```

## 📚 Примеры шаблонов

Смотрите готовые примеры в папке `template-examples/`:

- **kmp-module** - Kotlin Multiplatform модуль с Android/iOS
- Используйте эти примеры как основу для своих шаблонов

## 🎨 Создание шаблонов через UI

**New Template...** - создаёт новый шаблон через визард:
- Указываете ID, имя и описание
- Добавляете параметры (moduleName, packageName и т.д.)
- Плагин создаёт структуру папок и `template.xml`
- Дальше редактируете .ftl файлы вручную

**Edit Template...** - редактирует существующий:
- Выбираете шаблон из списка
- Изменяете имя, описание, параметры
- Сохраняете изменения в `template.xml`

## ⚙️ Типы параметров

| Тип | Описание | Пример |
|-----|----------|--------|
| `TEXT` | Обычный текст | Module name |
| `PACKAGE` | Имя пакета | com.example.app |
| `BOOLEAN` | Да/Нет | true/false |
| `NUMBER` | Число | 24 |

## 🎯 Использование в команде

1. Создайте шаблоны в `.idea/kmp-templates/`
2. Закоммитьте их в репозиторий
3. Вся команда использует одинаковые шаблоны
4. Изменения шаблонов = просто изменение файлов

## 📖 Документация FreeMarker

Полная документация: https://freemarker.apache.org/docs/

Основные возможности:
- Условия: `<#if>`, `<#elseif>`, `<#else>`
- Циклы: `<#list>`, `<#items>`
- Функции: `?upper_case`, `?lower_case`, `?cap_first`, `?replace`
- Макросы: `<#macro>`, `<#nested>`

## 🤝 Contributing

Pull requests приветствуются! Особенно:
- Новые шаблоны
- Улучшения UI
- Баг-фиксы

## 📄 License

MIT License

## 🔗 Links

- Issues: https://github.com/NonoxyS/kmp-module-generator/issues
- Discussions: https://github.com/NonoxyS/kmp-module-generator/discussions

---

**Сделано с ❤️ для Kotlin Multiplatform community**
