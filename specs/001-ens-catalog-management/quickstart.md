# Guía de validación rápida

Esta guía valida el proyecto Gradle Android y los flujos implementados. Los escenarios esperados y
las tablas se describen en [data-model.md](data-model.md); los ficheros de salida en
[contracts/export-format.md](contracts/export-format.md).

## Requisitos

- JDK 17, Android SDK con API 35 y Android Gradle Plugin compatible.
- Android Emulator o dispositivo API 26+ para pruebas instrumentadas de Keystore, Room y UI.
- Sin credenciales ni conexión de red; el manifest de la app no debe declarar `INTERNET`.

## Pruebas unitarias de dominio

Desde la raíz del repositorio, después de añadir el wrapper Gradle:

```powershell
.\gradlew.bat :domain:test
```

**Esperado**: pruebas JUnit 5 para la matriz de aplicabilidad categoría/dimensión y la regla de
selección única; respuesta L0 diferenciada de la ausencia de respuesta; validación explícita de
metadatos que requieren revisión.

## Importación SQLite del catálogo

```powershell
.\gradlew.bat :data:testDebugUnitTest
```

**Esperado**: importador valida el asset OSCAL, preserva los 3 marcos, 15 familias, 73 medidas, 133
refuerzos, partes anidadas y 467 `item`; clasifica 386 como respondibles por tener padre inmediato
`name: "requisitos"`, 76 como subitems (padre `item`) y 5 como descriptivos (padre `overview`).
Comprueba que subitems forman parte del contenido renderizado del requisito padre y no generan
respuestas propias. Conserva las diez elecciones disyuntivas y falla con error explícito ante
estructura/códigos desconocidos. Al importar una revisión nueva no elimina sistemas ni respuestas.

## Persistencia cifrada y aislamiento multi-sistema

Con emulador/dispositivo conectado:

```powershell
.\gradlew.bat :data:connectedDebugAndroidTest
```

**Esperado**: dos sistemas conservan configuraciones y respuestas independientes; AES-GCM recupera
valores correctos y detecta ciphertext/tags inválidos; no existe fila de respuesta al inicio y L0
persiste como evaluación explícita. La pérdida de clave produce error visible, no una base vacía.

## Flujos end-to-end de interfaz y exportación

```powershell
.\gradlew.bat :presentation:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug
```

En la app, crear dos sistemas con categorías y dimensiones distintas, navegar por `org`, `op` y `mp`,
verificar N/A y color/texto/icono, guardar L0 y una nota en un requisito respondible, y confirmar que
otro sistema no cambia. Probar cada grupo disyuntivo con una sola opción y cambiar la alternativa.
Exportar PDF y CSV durante una evaluación parcial, comprobar el contrato de archivo y medir menos de
5 segundos; validar TalkBack y escalado de fuente.

## Informes

La instantánea corresponde al sistema seleccionado; el CSV contiene una fila por cada requisito
normativo, incluye `NO_APLICA`/`REVISAR` y conserva vacío distinto de `L0`. El PDF contiene
configuración CIDAT, resumen y detalle jerárquico. La app pide un destino mediante el selector
Android y muestra la advertencia de seguridad antes de exportar: el archivo final PDF/CSV no queda
cifrado por ENSapp y su custodia corresponde al usuario/organización.

Validación unitaria de los formatos y composición Android:

```powershell
.\gradlew.bat :domain:test
.\gradlew.bat :data:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

La fuente OSCAL empaquetada vigente es `datos/ENS_Anexo_II_rev_10.json` (versión 10). Los 41
refuerzos sin regla propia siguen la asunción explícita `APLICABILIDAD_HEREDADA_DE_MEDIDA` descrita
abajo; no se presenta como interpretación normativa verificada.

## Asunción normativa visible: herencia de 41 refuerzos

Cuando falta `aplicacion-por` en un refuerzo, la política provisional
`APLICABILIDAD_HEREDADA_DE_MEDIDA` le asigna la regla de categoría/dimensión de su Medida padre. Es
una **asunción de diseño pendiente de validación normativa posterior**, no un hecho confirmado por
el JSON ni por el PDF AEAD. La prueba de dominio debe verificar la herencia, su procedencia y que no
se aplique cuando el refuerzo sí declara regla propia.

IDs de los 41 refuerzos afectados:

```text
org.2.r1, org.3.r1, op.pl.5.r1, op.pl.5.r2, op.acc.2.r2, op.acc.3.r2, op.acc.3.r3,
op.acc.5.r6, op.acc.5.r7, op.exp.1.r1, op.exp.1.r2, op.exp.1.r3, op.exp.1.r4,
op.exp.3.r4, op.exp.3.r5, op.exp.4.r3, op.exp.4.r4, op.exp.6.r5, op.exp.7.r4,
op.exp.10.r2, op.ext.3.r1, op.ext.3.r2, op.ext.3.r3, op.cont.2.r1, op.cont.2.r2,
op.cont.4.r1, op.mon.1.r3, op.mon.3.r7, mp.per.1.r1, mp.eq.4.r2, mp.com.2.r4,
mp.com.2.r5, mp.com.3.r5, mp.si.1.r1, mp.si.5.r2, mp.sw.1.r5, mp.sw.2.r2,
mp.info.3.r5, mp.info.4.r1, mp.s.3.r2, mp.s.4.r2
```
