# TelegramImagePrinter
The intention of this project is to make an app that upon receiving an image from a select Telegram bot, prints the image over bluetooth

Written in Kotlin to get acquainted with the language

App secrets are stored in app.properties in the root directory. Boy do I hope I manage to scrub them all!
```
appId=<integer>
appHash="<string>"
secretEncryptionKey="<string>"
printerMacAddress="00:00:00:00:00:00"
botUserId=<integer>
```

Uses https://github.com/Aliaksei-Karaliou/Tdlib-Jitpack-Android for TdLib
