# Plan de implementación: ENSapp — gestión del catálogo ENS

**Branch**: `001-ens-catalog-management` | **Fecha**: 2026-10-07 | **Spec**: [spec.md](spec.md)

## Resumen

Aplicación Android nativa offline-first para mantener varios Sistemas de Información, navegar el
catálogo ENS/OSCAL y responder a sus requisitos. Se importará el JSON OSCAL empaquetado una vez por
revisión del catálogo en una base Room/SQLite; Room será la única fuente local de datos. El dominio
mantendrá explícita la estructura jerárquica del OSCAL y calculará la aplicabilidad por regla de cada
control. Los módulos `domain`, `data`, `presentation` y `app` separan reglas de negocio, importación y
persistencia, MVVM/UI y composición de la aplicación, respectivamente.

La inspección confirma 3 grupos raíz, 15 subgrupos de familia, 73 medidas y 133 controles de refuerzo.
El marco `org` tiene 4 medidas directamente bajo el marco; no se crea una familia ficticia para
ubicarlas. De las 467 partes `name: "item"`, son respondibles los 386 items hijos directos de una
parte `name: "requisitos"`; 76 items anidados bajo otro item y 5 bajo una overview forman texto
descriptivo, no entidades `Requisito`. Los 41 refuerzos sin criterio propio heredarán por defecto la
aplicabilidad de su Medida como asunción pendiente de validación normativa. La copia PDF/CSV exportada
fuera de la app no será cifrada por ENSapp; el usuario/organización protege ese fichero, sin alterar
el cifrado de los datos internos.

## Contexto técnico

**Lenguaje/versión**: Kotlin 2.0+; Java toolchain 17.  
**Dependencias principales**: AndroidX Room sobre SQLite, kotlinx.serialization JSON,
Kotlin Coroutines, AndroidX Lifecycle/ViewModel, Jetpack Compose Material 3, JUnit 5, Mockito y APIs
de Android Keystore/JCA AES-GCM. No se añade SDK de red ni backend.  
**Persistencia**: una base Room/SQLite local. El catálogo público se guarda en tablas SQLite tras
importarse desde un asset JSON versionado; valores de los sistemas y evaluaciones sensibles se
cifran antes de persistirlos.  
**Testing**: JUnit 5 + Mockito para dominio/casos de uso (TDD); pruebas de importador y repositorios;
pruebas instrumentadas para Room y Android Keystore.  
**Plataforma**: Android nativo, minSdk 26, targetSdk 35, teléfonos y tabletas.  
**Tipo de proyecto**: aplicación Android multi-módulo, sin API/backend.  
**Objetivos de rendimiento**: no parsear el catálogo en cada inicio; importar fuera del hilo UI y en
transacción. Generación de informes PDF/CSV en menos de 5 segundos, según spec.md. Medir el tiempo de
importación inicial; no se fija un umbral adicional no especificado.  
**Restricciones**: funcionamiento sin permiso `INTERNET`; SQLite como única fuente local; cifrado de
datos sensibles; conservación exacta de IDs, orden, texto y anidamiento OSCAL; accesibilidad
Material 3/TalkBack/fuentes dinámicas.  
**Escala**: una instalación local, varios sistemas; catálogo OSCAL rev. 10 con 3 marcos, 15
subgrupos, 73 medidas, 133 refuerzos y 467 partes `item`.

## Inspección del catálogo y hallazgos

Fuentes examinadas: `datos/ENS_Anexo_II_rev_10.json` (OSCAL 1.1.3; 442.339 bytes) y
`datos/Decisiones-ENS-OSCAL-JSON_v1-3_acc_2.pdf` (AEAD, versión 1.0). El detalle y las referencias
están en [research.md](research.md).

| ID | Hallazgo observado | Tratamiento en este plan |
|---|---|---|
| H-01 | 467 partes tienen `name: "item"`; RF-01 identifica 386 requisitos oficiales. Ningún item tiene atributo `class`. | Resuelto por la posición estructural real: `name == "item"` y padre OSCAL inmediato `name == "requisitos"` identifica 386 respuestas independientes. Los 76 cuyo padre es `item` y los 5 cuyo padre es `overview` se conservan como contenido textual anidado. |
| H-02 | 41 controles `class: "ens-refuerzo"` no declaran `aplicacion-por`. | Asunción de diseño explícita: heredar categoría y/o dimensión de la Medida padre. Se deja identificada como hipótesis a validar contra la normativa, no como semántica confirmada por el catálogo. |
| H-03 | La app no controla la protección del documento una vez que PDF/CSV sale de su perímetro. | Limitación aceptada: ENSapp no cifra el fichero exportado en su destino final; desde la exportación, el usuario/organización es responsable de proteger esa copia. El cifrado interno SQLite/Keystore permanece sin cambios. |

Los nodos `op.acc.5.r1`–`r4`, `op.acc.6.r1`–`r4`, `mp.com.4.r1`–`r3` y `mp.s.2.r1` carecen de
alguna propiedad propia `nivel`/`categoria`; en estos casos la selección OSCAL se expresa mediante
`params[].props` y `select.choice`. El importador conservará tanto esa configuración como las props
de los controles; la selección disyuntiva no se deducirá de un listado codificado a mano.

## Constitution Check

**Puerta antes del diseño**: compatible con los seis principios; no se incorpora conectividad ni
almacenamiento alternativo. H-01 se resuelve mediante la estructura del JSON y el recuento oficial.
H-02 adopta herencia como valor predeterminado implementable, señalada como asunción que requiere
validación normativa posterior. H-03 acepta explícitamente que la app no cifra exportaciones fuera
de su perímetro; no modifica el cifrado en reposo interno de datos SQLite/Keystore.

| Principio | Decisión de diseño | Estado |
|---|---|---|
| I. Seguridad por Diseño | Keystore-backed AES-GCM para datos locales sensibles, sin permiso `INTERNET`. El fichero exportado no se cifra fuera del perímetro app; protección asignada al usuario/organización. | Conforme dentro del límite aceptado de exportación |
| II. Fidelidad ENS/OSCAL | Importación recursiva de grupos, controles, props, params y parts; 386 requisitos respondibles clasificados por el padre OSCAL `name: requisitos`. Herencia de aplicabilidad para refuerzos sin regla marcada como asunción. | Conforme con asunción H-02 pendiente de validación normativa |
| III. Test-First | JUnit 5 + Mockito; TDD obligatorio para cálculo por categoría/dimensión, estados y selección única. | Conforme |
| IV. Arquitectura Mantenible | Cuatro módulos con dependencias dirigidas hacia domain; complejidad justificada por las capas y el aislamiento de responsabilidades. | Conforme |
| V. Accesibilidad | Compose Material 3, semántica TalkBack, texto/icono además de color y fuentes dinámicas. | Conforme |
| VI. Persistencia y Portabilidad | SQLite/Room como única fuente de datos local; PDF con `PdfDocument` y CSV bajo acción explícita. Exportaciones externas fuera de la garantía de cifrado interna. | Conforme; limitación H-03 aceptada |

## Decisiones técnicas

### Ingesta y revisiones del catálogo

El asset JSON (442.339 bytes) se analiza con kotlinx.serialization **en el primer uso de la base y
solo cuando cambia la revisión embebida**, nunca en cada arranque. El importador corre fuera del hilo
principal, valida la estructura y persiste el catálogo dentro de una transacción Room; la revisión,
UUID OSCAL, versión, fecha de modificación y checksum se guardan en SQLite con los datos importados.
La UI espera a que finalice la importación inicial y muestra errores explícitos si el asset no puede
validarse. Una transacción evita dejar catálogos parciales.

Se elige JSON empaquetado + importación versionada frente a una SQLite semilla preconstruida: el JSON
original sigue siendo revisable y trazable frente al Anexo II; una revisión como rev. 11 sustituye el
asset y el importador actualiza solo tablas de catálogo, sin reemplazar la base que contiene sistemas
y respuestas. Para 442 KB, el coste de parsear una vez es proporcionado; no se paga en aperturas
posteriores. Una DB semilla podría reducir trabajo en la primera instalación, pero su copia inicial
no resuelve por sí sola las actualizaciones de una DB ya existente y acopla el proceso al artefacto
SQLite generado. No se usará una segunda DB como fuente permanente.

La revisión del catálogo se mantiene separada de la versión del esquema Room. No se permite una
migración destructiva. Antes de importar una revisión futura se valida el catálogo completo; la
actualización de tablas ocurre atómicamente y nunca borra respuestas. El tratamiento de IDs normativos
retirados o modificados en revisiones posteriores deberá conservar la trazabilidad histórica.

### Mapeo OSCAL → dominio

- **`catalog.groups[]`**: los tres grupos raíz `org`, `op`, `mp` son `Marco`. Los 15 `groups[]`
  descendientes son `Familia` bajo su marco. Las cuatro medidas de `org.controls[]` tienen marco
  `org` y `family_id = null`; se conserva la forma original en lugar de crear una familia artificial.
  Las parts de overview de grupo se conservan.
- **`controls[]`**: cada control sin `class: "ens-refuerzo"` es `Medida` (73 en este catálogo).
  Cada control con `class: "ens-refuerzo"` es `Refuerzo` (133); `parent_control_id` representa su
  medida/control padre y permite anidamiento recursivo. Se conservan ID, title, class, props y orden.
- **Herencia de aplicabilidad del refuerzo**: cuando un `Refuerzo` carece de su propia prop
  `aplicacion-por`, su aplicabilidad efectiva hereda la regla y los valores categoría/dimensión de
  su Medida padre. Es una asunción de diseño pendiente de validación normativa posterior, no un hecho
  afirmado por OSCAL. Se codifica en
  `domain/src/main/kotlin/com/ensapp/domain/catalog/ApplicabilityPolicy.kt` con la constante nombrada
  `APLICABILIDAD_HEREDADA_DE_MEDIDA`; el resultado debe conservar la procedencia como heredada.
- **`props[]`**: propiedades ENS se seleccionan por `ns == "urn:es:ens"` y se almacenan como
  colecciones, nunca como mapa de clave única (hay valores repetidos para `dimension`, `categoria` y
  `nivel`). `name == "label"` sin namespace conserva la etiqueta normativa. No se descartarán props
  desconocidas durante la importación.
- **Aplicabilidad**: `aplicacion-por == "categoria"` crea una regla `CATEGORY` y toma los valores
  `categoria` del mismo control. `aplicacion-por == "nivel-dimension"` crea `DIMENSION_LEVEL` y toma
  las colecciones `dimension` y `nivel`. Estas son las únicas dos cadenas `aplicacion-por` halladas;
  ninguna regla se combina globalmente: se evalúa en su control, y las reglas del refuerzo no se
  confunden con las de la medida. Si el control es un refuerzo y no declara la propiedad, se aplica
  `APLICABILIDAD_HEREDADA_DE_MEDIDA`; si es una medida sin criterio o un futuro valor desconocido,
  se registra explícitamente y no se aplica una regla predeterminada silenciosa.
- **`params[]`**: `tipo-param == "seleccion-refuerzo"` se busca en `params[].props` con namespace
  ENS, no en las props del control. Cada parámetro es un grupo de selección; `select.how-many ==
  "one"` y `select.choice[]` definen cardinalidad y IDs admisibles. Los 10 grupos observados están
  en `op.acc.5` (3), `op.acc.6` (3), `mp.com.4` (2) y `mp.s.2` (2).
- **`parts[]`**: se conserva recursivamente con `id`, `name`, `ns`, `props`, `prose`, posición y
  `parent_part_id`, respetando saltos de línea. `name: "requisitos"` es contenedor textual. Un
  `name: "item"` cuyo padre inmediato es `requisitos` es `Requisito` independiente (386). Los 76
  items con padre inmediato `item` se renderizan como subviñetas/prose del requisito padre, no como
  respuestas; los cinco items cuyo padre es `overview` son contenido descriptivo de esa overview.
  Se almacenan los 467 nodos originales con su estructura exacta.

La aplicabilidad efectiva de un requisito hereda su control normativo dueño; la aplicabilidad de una
familia/marco se deriva de sus descendientes para presentación, no de una regla inventada para el
grupo. Para un refuerzo sin regla propia se aplica la constante de herencia descrita arriba, dejando
constancia de esa procedencia. En una regla `nivel-dimension`, las dimensiones y los niveles se
conservan como listas OSCAL independientes y el cálculo comprueba el nivel configurado de cada
dimensión contra los valores `nivel` declarados; no se fabrica una asociación dimensión-nivel que el
JSON no expresa.

### Persistencia y cifrado

Room es la capa de acceso a SQLite; el cifrado de campos se ejecuta antes de escribirlos, mediante
AES-GCM con clave no exportable administrada por Android Keystore. Se cifran nombre/configuración
del sistema, nivel por dimensión, nivel de madurez, nota y elección de refuerzo. Cada campo cifrado
incluye nonce aleatorio y versión del formato criptográfico; no se registran valores en claro en logs.
IDs aleatorios y datos públicos del catálogo permanecen indexables en SQLite. No se usa backup que
separe el ciphertext de su clave; la pérdida/invalidez de clave se notifica como error, nunca como
estado vacío. Android Keystore almacena material criptográfico, no una fuente adicional de datos de
dominio.

Se prefiere cifrado AES-GCM de campos de usuario frente a SQLCipher porque el requisito es proteger
datos sensibles y el catálogo es público; Room conserva su SQLite estándar y la clave permanece
protegida por Keystore. SQLCipher es alternativa si la revisión determina que debe cifrarse todo el
archivo, pero implica integrar otro motor y diseñar explícitamente cómo alimentar una clave al abrir
la DB. Los límites y consecuencias para informes fuera de SQLite están en H-03.

### Arquitectura y módulos Gradle

```text
settings.gradle.kts
app/                         # Android Application, arranque y composición de dependencias
domain/                      # Kotlin JVM puro: entidades, puertos, reglas y casos de uso
data/                        # Android library: Room/SQLite, importador JSON, cifrado, repositorios
presentation/                # Android library: Compose Material 3, navegación, ViewModels
datos/ENS_Anexo_II_rev_10.json  # fuente versionada en el repositorio; empaquetar como asset de data
```

Dirección de dependencias: `app → presentation + data + domain`; `presentation → domain`;
`data → domain`; `domain` no depende de Android ni de data. La composición de `app` construye los
repositorios/casos de uso y los proporciona a ViewModels; no se añade framework de DI hasta que sea
necesario.

- **domain**: `Marco`, `Familia`, `Medida`, `Refuerzo`, `Requisito`, configuración del sistema,
  `ApplicabilityRule`, resultado de aplicabilidad, respuesta, niveles y elección; puertos de
  repositorio; casos de uso; servicios puros de aplicabilidad, estado y validación de selección.
- **data**: DTOs OSCAL serializables; importador/validador; mapeadores a entidades Room; entidades,
  DAOs, base de datos y migraciones; repositorios; cifrado AES-GCM/Keystore. No decide reglas que
  corresponden al dominio.
- **presentation**: pantallas y ViewModels MVVM que invocan casos de uso; selección de sistema,
  catálogo, respuestas, estado y exportación; semántica accesible y representación de colores.
- **app**: `Application`, Activity, configuración de módulos, navegación raíz, asset/build config y
  manifest sin permiso `INTERNET`.

La separación en cuatro módulos se justifica por los límites obligatorios de Clean Architecture,
SQLite/importación y MVVM; evita acoplar la lógica normativa a Android y permite TDD unitario puro.

### Estrategia de testing

Aplicar TDD en dominio/casos de uso: escribir la prueba JUnit 5 primero y comprobar que falla,
implementar el mínimo y comprobarla en verde; Mockito se reserva para puertos/repositorios
colaboradores. Las reglas puras se prueban sin mockear el propio cálculo.

1. **Categoría**: tabla de casos para BÁSICA/MEDIA/ALTA contra cada conjunto explícito `categoria`;
   prueba que variar categoría sin modificar CIDAT solo afecta controles `CATEGORY`.
2. **Dimensión**: para cada una de Confidencialidad, Integridad, Disponibilidad, Autenticidad y
   Trazabilidad, variar solo su nivel y comprobar que únicamente cambian controles que declaran esa
   dimensión. Probar múltiples dimensiones y listas de niveles, además de la ausencia/incompletitud
   de metadatos como resultado explícito de revisión, no como valor predeterminado silencioso.
3. **Selección disyuntiva**: fixtures del JSON real de las cuatro medidas; rechazar refuerzos fuera de
   `choice`, impedir dos opciones simultáneas para `(sistema, parámetro)`, permitir el cambio
   explícito y probar aislamiento entre sistemas. SQLite añade restricción única y el cambio se
   ejecuta transaccionalmente.
4. **Importación/persistencia**: comprobar IDs, jerarquía, props repetidas, saltos de línea, los 467
   `item`, 10 params y las cardinalidades registradas; la importación de una revisión no borra
   sistemas ni respuestas. Room valida claves foráneas y que ausencia de fila de respuesta sea
   distinta del valor cifrado `L0`.
5. **Seguridad/UI/exportación**: tests instrumentados de cifrado/descifrado AES-GCM y error de clave;
   UI verifica TalkBack, texto/icono además de color y escalado de fuente. Exportes contrastados con
   [contracts/export-format.md](contracts/export-format.md), incluidos CSV con madurez vacía frente a
   `L0`.

Los tests verificarán los 386 requisitos directos de parts `requisitos`, que la herencia H-02 se
aplique solo cuando falte criterio propio, y que la exportación H-03 no altere el cifrado interno.
La semántica de nivel por dimensión usa pertenencia al conjunto de niveles publicado por OSCAL para
cada dimensión afectada; las listas se preservan para auditar el resultado.

## Estructura de esta documentación

```text
specs/001-ens-catalog-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── export-format.md
```

## Revisión de Constitución tras el diseño

**Resultado**: arquitectura, dependencias, persistencia y estrategia de pruebas son compatibles con
la Constitución. H-01 está resuelto por el criterio estructural 386/76/5; H-02 tiene regla por
defecto implementable y queda pendiente solo de validación normativa posterior; H-03 es una
limitación de producto aceptada con responsabilidad de protección transferida al usuario/organización.
No se añaden desviaciones justificadas mediante complejidad adicional; los cuatro módulos responden a
las separaciones exigidas por el Principio IV.
