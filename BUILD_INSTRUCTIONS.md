# Инструкции по сборке APK

## Быстрая сборка Debug APK

### Через командную строку:

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

APK будет находиться в: `app/build/outputs/apk/debug/app-debug.apk`

## Сборка Release APK (для публикации)

### 1. Создайте keystore (только один раз):

```bash
keytool -genkey -v -keystore clickerapp-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias clickerapp
```

### 2. Создайте файл `keystore.properties` в корне проекта:

```properties
storePassword=ваш_пароль
keyPassword=ваш_пароль
keyAlias=clickerapp
storeFile=clickerapp-release-key.jks
```

### 3. Обновите `app/build.gradle.kts` для подписи:

```kotlin
android {
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = java.util.Properties()
            keystoreProperties.load(keystorePropertiesFile.inputStream())
            
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 4. Соберите Release APK:

```bash
gradlew.bat assembleRelease
```

APK будет в: `app/build/outputs/apk/release/app-release.apk`

## Размещение на GitHub Releases

1. Перейдите на страницу репозитория: https://github.com/temiixxx/kozaklik
2. Нажмите **"Releases"** → **"Create a new release"**
3. Заполните:
   - **Tag version**: `v1.0.0`
   - **Release title**: `Версия 1.0.0 - Кликер козы`
   - **Description**: Описание изменений
4. Загрузите APK файл в раздел **"Attach binaries"**
5. Нажмите **"Publish release"**

## Автоматическая сборка через GitHub Actions (опционально)

Создайте файл `.github/workflows/build.yml`:

```yaml
name: Build APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```
