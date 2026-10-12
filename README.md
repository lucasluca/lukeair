# LukeAir

Controle remoto Android (Kotlin + Compose) para o ar-condicionado Samsung Digital Inverter WindFree (AR12AVHABWKNAZ), usando o emissor infravermelho do celular (testado num Redmi Note 7).

- Protocolo Samsung A/C portado do [IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266) (`app/src/main/java/com/lukeair/ir/SamsungAc.kt`); cada comando envia o estado completo.
- Ligar/desligar usa a mensagem estendida de 21 bytes; demais comandos, a de 14 bytes.

```sh
./gradlew :app:testDebugUnitTest   # valida contra capturas reais
./gradlew :app:installDebug
```
