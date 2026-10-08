# ENSapp

Aplicación Android nativa y offline-first para gestionar Sistemas de Información ENS,
explorar el catálogo OSCAL y registrar respuestas L0–L5. Los datos de usuario se cifran con
AES-GCM y Android Keystore dentro del almacenamiento SQLite local.

## Módulos

- `domain`: modelos, aplicabilidad, respuestas, estado de cumplimiento e informes.
- `data`: importación OSCAL, Room/SQLite, repositorios y cifrado; los informes PDF usan
  `android.graphics.pdf.PdfDocument`.
- `presentation`: pantallas Compose, ViewModels y exportación mediante el selector de documentos.
- `app`: composición Android y arranque offline.

El catálogo activo empaquetado es `datos/ENS_Anexo_II_rev_10.json` (revisión 10). Su estructura
recursiva y los 386 requisitos respondibles se validan en las pruebas de importación.

## Construir y probar

Desde PowerShell en la raíz:

```powershell
.\gradlew.bat :domain:test
.\gradlew.bat :data:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Con un emulador/dispositivo Android conectado, ejecutar las pruebas instrumentadas:

```powershell
.\gradlew.bat :data:connectedDebugAndroidTest
.\gradlew.bat :presentation:connectedDebugAndroidTest
```

## Aplicabilidad y exportaciones

`APLICABILIDAD_HEREDADA_DE_MEDIDA` es una asunción de diseño provisional para los 41 refuerzos que
no definen una regla propia; no es una conclusión normativa confirmada. La app genera PDF y CSV
bajo demanda sin conexión y muestra una advertencia antes de exportar. Las copias guardadas fuera
de ENSapp no se cifran por la aplicación; su protección y custodia corresponden al usuario o a la
organización. Este límite no cambia el cifrado interno AES-GCM/Keystore.