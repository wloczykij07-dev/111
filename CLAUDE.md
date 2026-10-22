# Dieta – plan posiłków (Android, Kotlin + Jetpack Compose)

- Przy każdej zmianie aplikacji zwiększ `appVersion` w `gradle.properties` o 1.
  CI buduje wtedy `dieta-v<N>.apk` i publikuje wydanie `v<N>` na GitHubie.
- Testy logiki (parser przepisów, lista zakupów): `./gradlew testDebugUnitTest`.
