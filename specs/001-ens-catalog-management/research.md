# Investigación: ENSapp — catálogo y persistencia

## Ingesta versionada del catálogo

**Decisión**: empaquetar el JSON original como asset y analizarlo solo en primera inicialización o
cuando la revisión de catálogo de la app cambie. Importar en segundo plano, validar y actualizar las
tablas de catálogo en una transacción Room. Registrar `catalog.uuid`, `metadata.version`,
`oscal-version`, `last-modified` y checksum en SQLite. No parsear el archivo en cada apertura.

**Rationale**: el fichero real tiene 442.339 bytes y contiene un catálogo manejable. El parseo único
evita penalizar todos los arranques; conservar el JSON como artefacto versionado facilita comparar
una rev. 11 con la fuente actual. La actualización en SQLite mantiene una única fuente local y evita
reemplazar la DB que almacena sistemas y respuestas. Importar todo en una transacción previene un
catálogo parcialmente cargado.

**Alternativas consideradas**:

- Parsear en cada inicio: rechazado porque repite trabajo sin beneficio; SQLite debe ser la fuente de
  lectura normal después de la importación.
- Preconstruir y copiar una DB semilla: válido técnicamente y puede acortar el primer llenado, pero
  Room ofrece precarga desde asset para una base nueva, no actualización general del catálogo en una
  DB existente. La versión JSON mantiene mejor la trazabilidad normativa y permite una importación
  versionada sin sustituir respuestas.

Room documenta precarga y migraciones separadamente: [pre-populate a Room database](https://developer.android.com/training/data-storage/room/prepopulate)
y [migrate Room databases](https://developer.android.com/training/data-storage/room/migrating-db-versions).
Para la importación fuera del hilo UI, seguir la recomendación de funciones coroutine main-safe:
[coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

## Cifrado local

**Decisión**: cifrar los campos sensibles del usuario con AES-GCM y una clave no exportable de
Android Keystore antes de persistirlos en SQLite/Room. Guardar nonce y versión criptográfica junto al
ciphertext; tratar fallos de descifrado como errores visibles.

**Rationale**: el catálogo es público; los datos confidenciales son nombres/configuración del sistema,
respuestas, notas y selecciones. Cifrar esos valores deja que Room use SQLite estándar y conserva
identificadores/relaciones indexables. Keystore protege el material de clave. Las fuentes Android
recomiendan AES-GCM y detallan almacenamiento de claves: [cryptography](https://developer.android.com/privacy-and-security/cryptography),
[Android Keystore](https://developer.android.com/privacy-and-security/keystore).

**Alternativas consideradas**:

- SQLCipher para cifrar el archivo completo: opción si la política exige cifrado integral de DB.
  Debe tratarse como motor SQLite alternativo, no como una función de Room; requiere diseñar cómo
  derivar o recuperar la clave para abrir la base. Revisar integración oficial y distribución antes
  de adoptarlo: [SQLCipher Android / Room integration](https://github.com/sqlcipher/sqlcipher-android#sqlcipher-for-android-room-integration).
- Cifrado solo por el almacenamiento del dispositivo: insuficiente para satisfacer el requisito de
  cifrado de campos y para controlar los datos cifrados de aplicación.

PDF/CSV son salidas legibles requeridas por la spec y no continúan dentro de SQLite; su protección
fuera de la app corresponde al usuario/organización (limitación aceptada H-03). Desactivar el backup
automático de la base cifrada mientras no exista recuperación coordinada con su clave, para evitar
restaurar ciphertext sin clave utilizable.

## Correspondencia OSCAL real

**Decisión**: preservar la estructura recursiva y sus identificadores, sin aplanar y sin crear grupos
que el fichero no contiene. Los atributos se extraen de listas de props por `ns` y `name`, incluyendo
valores repetidos. Guardar textualmente `parts.prose`, orden y anidamiento.

**Rationale**: el JSON contiene:

| Estructura del catálogo | Recuento observado |
|---|---:|
| Grupos raíz `catalog.groups[]` (marcos) | 3 |
| Grupos descendientes (familias) | 15 |
| Medidas (`controls` sin clase `ens-refuerzo`) | 73 |
| Controles de refuerzo (`class: ens-refuerzo`) | 133 |
| Partes `name: requisitos` | 206 |
| Partes `name: item` | 467 |
| Items hijos directos de `requisitos` (`Requisito` respondible) | 386 |
| Items hijos directos de otro `item` (texto/subviñeta) | 76 |
| Items hijos directos de `overview` (contenido descriptivo) | 5 |
| Items padre con subpartes | 23 |
| Items con atributo `class` presente | 0 |
| Grupos con parts de overview | 17 |
| Parámetros `tipo-param: seleccion-refuerzo` | 10 |

Los 4 controles `org` son hijos directos del marco `org`; `op` y `mp` tienen 7 y 8 familias
respectivamente. Esto concuerda con el texto actual de RF-01, que ya documenta la forma excepcional
de `org`.

### Clasificación de `item` respondible

**Decisión**: clasificar como entidad independiente `Requisito` cada part que cumpla ambas
condiciones: `part.name == "item"` y el padre inmediato en la jerarquía `parts[]` tenga
`name == "requisitos"`. El JSON da exactamente 386 coincidencias, alineadas con la unidad oficial
de requisitos que recoge RF-01. Se usa el nombre del padre y la posición estructural; `class` no
participa en la clasificación porque está ausente en los 467 items. El ID de la part (y su
`props.label` cuando existe) es su identidad/trazabilidad normativa.

Los 76 items cuyo padre inmediato es otro `item` se conservan como subviñetas/desgloses textuales
dentro del `prose` del requisito padre, no como respuestas independientes. Los otros 5 items cuelgan
directamente de una `overview` y son texto descriptivo, tampoco respondible. Así se preservan los
467 nodos originales, pero solo 386 tienen respuesta propia. Hay 23 items padre con subpartes; sus
subitems se representan como contenido anidado, manteniendo el orden y la jerarquía OSCAL.

**Alternativas consideradas**: hacer respondibles todos los 467 items o solo hojas (444);
rechazadas porque duplicarían subviñetas como requisitos independientes o excluirían requisitos
oficiales cuyos items principales contienen subapartados. El criterio de padre `requisitos` produce
el recuento exacto 386.

## Aplicabilidad y selección de refuerzos

**Decisión**: modelar dos reglas distintas, asignadas al control que las declara:

- `aplicacion-por: categoria`: lista de props `categoria` bajo `urn:es:ens`.
- `aplicacion-por: nivel-dimension`: listas de props `dimension` y `nivel` bajo `urn:es:ens`.

Se encontraron 94 controles con criterio de categoría y 71 con criterio nivel-dimensión. No se
encontró otro valor de `aplicacion-por`. Los parámetros disyuntivos se leen de
`controls[].params[]`: `tipo-param: seleccion-refuerzo` vive en las props del parámetro; el scope
`categoria` o `nivel` y `select.choice[]` determinan opciones. El JSON contiene 3 parámetros en cada
una de `op.acc.5` y `op.acc.6`, y 2 en cada una de `mp.com.4` y `mp.s.2`, todos con
`select.how-many: "one"`.

**Rationale**: la distinción está escrita explícitamente en los controles/params y coincide con la
convención que describe AEAD. No generalizar reglas de categoría a todo el catálogo ni inferir
selecciones desde títulos/prose.

**Alternativas consideradas**: una sola regla global o una selección codificada con IDs de las cuatro
medidas; rechazadas porque borrarían reglas por control o duplicarían la fuente OSCAL.

## Asunciones y límites que deben quedar visibles en la implementación

1. **H-02 — regla de aplicabilidad ausente en 41 refuerzos**: el catálogo no declara `aplicacion-por`
   en estos controles. Por decisión de producto, la regla de diseño por defecto es heredar categoría
   y/o dimensión de seguridad de la Medida padre. Se registra en código con la constante nombrada
   `APLICABILIDAD_HEREDADA_DE_MEDIDA` en
   `domain/src/main/kotlin/com/ensapp/domain/catalog/ApplicabilityPolicy.kt`, acompañada por un
   comentario que identifica explícitamente una **asunción de diseño pendiente de validación
   normativa posterior**. No presentar esta herencia como hecho confirmado por OSCAL/AEAD. Al
   resolverse una futura validación, modificar la política y sus pruebas con trazabilidad a la norma.
2. **Metadatos ausentes en refs seleccionables**: cuatro refuerzos de `mp.com.4`/`mp.s.2` no tienen
   props `categoria` y ocho de `op.acc.5`/`op.acc.6` no tienen props `nivel`; sus pertenencias a
   alternativas sí aparecen en `params[].select.choice[]`. El importador preserva ambas fuentes y el
   dominio debe resolver su aplicabilidad desde los parámetros, no rellenar props artificialmente.
3. **H-03 — exportación fuera del perímetro de cifrado de la app**: decisión aceptada: ENSapp no cifra
   el documento PDF/CSV una vez escrito en su destino final fuera del almacenamiento cifrado interno.
   Desde ese momento, proteger la copia exportada es responsabilidad del usuario/organización. Esto no
   cambia el cifrado AES-GCM/Keystore de los datos dentro de la app, ni la persistencia exclusiva en
   SQLite.

La revisión normativa del supuesto H-02 queda como verificación posterior, no como bloqueo para
generar tasks: la política de comportamiento por defecto ya está definida, visible y probada como
asunción.

## Fuentes de diseño Android

- Android Room: [guía](https://developer.android.com/training/data-storage/room), [precarga](https://developer.android.com/training/data-storage/room/prepopulate),
  [migraciones](https://developer.android.com/training/data-storage/room/migrating-db-versions).
- Android cryptography y Keystore: [cryptography](https://developer.android.com/privacy-and-security/cryptography),
  [Keystore](https://developer.android.com/privacy-and-security/keystore).
- Android Auto Backup: [backup guidance](https://developer.android.com/identity/data/autobackup).
- AEAD, *Catálogo ENS: Esquema Nacional de Seguridad—Anexo II en OSCAL JSON: Decisiones de diseño
  y descripción de la conversión de texto a código*, v1.0: secciones 4.1.1 (p. 9), 4.5.3–4.5.8
  (pp. 18–20), 4.7–4.8.7 (pp. 20–23). En particular: grupos frente a marcos, controles frente a
  medidas/refuerzos, items y subitems, las dos formas de aplicabilidad y params de elección única.
- Catálogo fuente: `datos/ENS_Anexo_II_rev_10.json`, `catalog.metadata.oscal-version = 1.1.3`;
  `catalog.groups[]`, `controls[].props[]`, `controls[].params[]`, `controls[].parts[]` y controles
  anidados.
