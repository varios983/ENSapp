# Gestión de ENS en Android

Aplicacion Android offline-first para la gestión del ENS en Android.

Para arrancar el emulador, es necesario quitar el proxy:

```powershell
$env:HTTP_PROXY=""
$env:HTTPS_PROXY=""
$env:NO_PROXY=""

cd $env:ANDROID_HOME\emulator
.\emulator.exe -avd medium_phone -verbose
```
