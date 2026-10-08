# Tasks: ENSapp — gestión del catálogo ENS

**Input**: Design documents from `specs/001-ens-catalog-management/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/export-format.md`, `quickstart.md`

**Testing**: Obligatorio por Constitución, Principio III: JUnit 5 + Mockito y TDD para lógica de
dominio/casos de uso. En los bloques TDD el orden es prueba escrita → revisión/aprobación → ejecución
en rojo → implementación → ejecución en verde. Las pruebas de reglas puras no mockean el propio
cálculo; Mockito se usa en los límites de repositorio/casos de uso.

**Organización**: tareas agrupadas por historia de usuario para permitir implementaciones e
integraciones incrementales. Todos los paths son relativos a la raíz del repositorio.

## Phase 1: Setup — scaffolding compartido

**Purpose**: el repositorio actualmente no tiene Gradle ni módulos Android. Este scaffolding es un
paso explícito e independiente; debe preservar el archivo de política de dominio que ya existe.

- [X] T001 Crear `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` y Gradle Wrapper; declarar los módulos `:app`, `:domain`, `:data` y `:presentation` según `specs/001-ens-catalog-management/plan.md`, sin eliminar ni sobrescribir `domain/src/main/kotlin/com/ensapp/domain/catalog/ApplicabilityPolicy.kt`.
- [X] T002 Crear los `build.gradle.kts` y manifests mínimos de `app/`, `domain/`, `data/` y `presentation/`; configurar Kotlin 2.0+, Java toolchain 17, `minSdk 26`, `targetSdk 35` y dependencias dirigidas `app → presentation + data + domain`, `presentation → domain`, `data → domain`.
- [X] T003 Configurar JUnit 5, Mockito para JUnit 5, AndroidX Test/Room testing, Compose Material 3, Room, kotlinx.serialization y Coroutines en `gradle/libs.versions.toml` y en los `build.gradle.kts` de módulos; confirmar que `domain/src/test/kotlin/` ejecuta con `.\gradlew.bat :domain:test`.

---

## Phase 2: Foundational — catálogo, base local y seguridad

**Purpose**: requisitos transversales que deben estar listos antes de implementar las historias.

- [X] T004 Definir entidades Kotlin sin dependencias Android en `domain/src/main/kotlin/com/ensapp/domain/catalog/` y `domain/src/main/kotlin/com/ensapp/domain/system/` para Marco, Familia, Medida, Refuerzo, CatalogPart, Requisito, categoría `BASICA|MEDIA|ALTA`, dimensiones CIDAT y niveles `BAJO|MEDIO|ALTO`.
- [X] T005 Escribir pruebas JUnit 5 de `CatalogJsonImporter` en `data/src/test/kotlin/com/ensapp/data/catalog/CatalogJsonImporterTest.kt`; usar `datos/ENS_Anexo_II_rev_10.json` y comprobar 3 marcos, 15 familias, 73 medidas, 133 refuerzos, 467 parts `item`, las diez selecciones disyuntivas y la conservación de IDs, props repetidas, prosa y orden.
- [X] T006 Revisar y aprobar las aserciones de `CatalogJsonImporterTest.kt` contra `datos/ENS_Anexo_II_rev_10.json` y las reglas OSCAL documentadas en `specs/001-ens-catalog-management/research.md`; validar específicamente la ubicación OSCAL de `props`, `params[].props`, `select.choice[]` y `parts[].name`.
- [X] T007 Ejecutar `.\gradlew.bat :data:testDebugUnitTest --tests "*CatalogJsonImporterTest"` y registrar en `data/src/test/kotlin/com/ensapp/data/catalog/CatalogJsonImporterTest.kt` que las pruebas fallan por ausencia del importador, no por error de compilación o de configuración del runner.
- [X] T008 Implementar DTOs kotlinx.serialization, validación y `CatalogJsonImporter` recursivo en `data/src/main/kotlin/com/ensapp/data/catalog/`; empaquetar `datos/ENS_Anexo_II_rev_10.json` como `data/src/main/assets/catalog/ENS_Anexo_II_rev_10.json`, preservando `groups`, controles anidados, props, params y parts sin aplanarlos.
- [X] T009 Ejecutar `.\gradlew.bat :data:testDebugUnitTest --tests "*CatalogJsonImporterTest"` y dejar verdes las pruebas de T005; mostrar errores de parseo/estructura desconocida explícitamente y no dejar importaciones parciales.
- [X] T010 Implementar tablas Room, relaciones, DAOs y migraciones no destructivas en `data/src/main/kotlin/com/ensapp/data/local/`; registrar UUID/revisión/checksum del catálogo e importar/upsert de tablas de catálogo de forma transaccional sin borrar sistemas ni respuestas.
- [X] T011 Implementar el cifrado AES-GCM de campos sensibles con clave no exportable Android Keystore en `data/src/main/kotlin/com/ensapp/data/security/`; versionar ciphertext/nonce, reportar fallos de clave/tag explícitamente y no guardar valores sensibles en logs.
- [X] T012 Configurar `app/src/main/AndroidManifest.xml` y `app/build.gradle.kts` sin permiso `INTERNET` ni dependencias de red/cloud; excluir datos cifrados del Auto Backup hasta definir recuperación coordinada con la clave.

**Checkpoint**: wrapper y cuatro módulos compilan; catálogo importable a Room; cifrado y base común listos.

---

## Phase 3: User Story 1 — gestionar y configurar varios Sistemas de Información (P1, MVP)

**Goal**: crear/editar sistemas independientes con categoría y cinco niveles CIDAT; aplicar las reglas
de categoría/dimensión y la asunción explícita de herencia para refuerzos sin criterio propio.

**Independent Test**: crear dos sistemas con categorías y niveles distintos y verificar que cada uno
recibe resultados de aplicabilidad independientes; variar una sola dimensión no debe modificar
controles de otras dimensiones ni controles de categoría.

### TDD: cálculo de aplicabilidad por categoría, dimensión y herencia

- [X] T013 [US1] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/catalog/ApplicabilityCalculatorTest.kt` y `domain/src/test/kotlin/com/ensapp/domain/system/ConfigureSystemUseCaseTest.kt` con JUnit 5 + Mockito: casos para cada categoría `BASICA|MEDIA|ALTA`, reglas `categoria` y `nivel-dimension`, las cinco dimensiones CIDAT, cambios aislados de nivel, propiedades repetidas y refuerzo sin regla propia; mockear únicamente el repositorio en la prueba del caso de uso y comprobar procedencia heredada mediante `APLICABILIDAD_HEREDADA_DE_MEDIDA`.
- [X] T014 [US1] Revisar/aprobar las expectativas de T013 contra `datos/ENS_Anexo_II_rev_10.json`, `specs/001-ens-catalog-management/spec.md` y `specs/001-ens-catalog-management/research.md`; incluir las cuatro medidas disyuntivas y una regla propia de refuerzo que debe prevalecer frente a herencia.
- [X] T015 [US1] Ejecutar `.\gradlew.bat :domain:test` y registrar resultado rojo esperado en los tests de T013 antes de implementar `ApplicabilityCalculator`; resolver cualquier error de compilación/test harness antes de aceptar el rojo.
- [X] T016 [US1] Implementar `ApplicabilityCalculator` y `ConfigureSystemUseCase` en `domain/src/main/kotlin/com/ensapp/domain/catalog/ApplicabilityCalculator.kt` y `domain/src/main/kotlin/com/ensapp/domain/system/ConfigureSystemUseCase.kt`; actualizar `domain/src/main/kotlin/com/ensapp/domain/catalog/ApplicabilityPolicy.kt` existente para aplicar `APLICABILIDAD_HEREDADA_DE_MEDIDA` solo a refuerzos sin criterio propio, conservando la procedencia heredada y sin recrear el archivo/constante desde cero.
- [X] T017 [US1] Ejecutar `.\gradlew.bat :domain:test` y dejar verdes los tests T013: evaluación independiente por control, coincidencia de nivel configurado con niveles OSCAL, precedencia de regla propia y herencia identificada como asunción.

### Configuración y selección de sistemas

- [X] T018 [US1] Implementar `InformationSystem`, `SystemDimension` y `InformationSystemRepository` en `domain/src/main/kotlin/com/ensapp/domain/system/`; exigir categoría `BASICA|MEDIA|ALTA` y exactamente las dimensiones `CONFIDENCIALIDAD`, `INTEGRIDAD`, `DISPONIBILIDAD`, `AUTENTICIDAD`, `TRAZABILIDAD`, cada una con nivel `BAJO|MEDIO|ALTO`.
- [X] T019 [US1] Implementar DAOs/repositorio SQLite para alta, edición, listado, selección y aislamiento multi-sistema en `data/src/main/kotlin/com/ensapp/data/system/`; cifrar nombre, categoría y niveles antes de guardarlos y almacenar cinco filas dimensionales al completar configuración.
- [X] T020 [US1] Crear ViewModel MVVM para lista, selección y formulario del sistema en `presentation/src/main/kotlin/com/ensapp/presentation/system/`; dirigir a creación inicial cuando no haya sistemas y permitir seleccionar/crear cuando ya existan.
- [X] T021 [US1] Crear pantallas Compose de alta/edición/listado de sistemas en `presentation/src/main/kotlin/com/ensapp/presentation/system/`; solicitar categoría y los cinco niveles y guardar cada sistema de forma independiente.
- [X] T022 [US1] Al editar categoría o nivel, recalcular aplicabilidad solo para el sistema activo y mostrar aviso de que respuestas existentes pueden dejar de aplicar en `presentation/src/main/kotlin/com/ensapp/presentation/system/EditSystemScreen.kt` y su ViewModel.

**Checkpoint**: US1 cumple los seis criterios de aceptación sin pantallas de respuesta.

---

## Phase 4: User Story 2 — navegar catálogo filtrado por aplicabilidad (P1)

**Goal**: navegar marcos, familias, medidas, refuerzos y los 386 requisitos respondibles, mostrando
aplicabilidad efectiva y permitiendo ver N/A sin habilitar respuestas.

**Independent Test**: con un sistema configurado, comparar estado/color/texto de nodos con el cálculo
por dominio; activar “mostrar todo” y comprobar que N/A se ve en gris y no se puede seleccionar.

### TDD: clasificación estructural de los 386 Requisito

- [X] T023 [US2] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/catalog/RequirementClassifierTest.kt` y `data/src/test/kotlin/com/ensapp/data/catalog/RequirementImportMappingTest.kt` con JUnit 5: clasificar como respondibles solo los `item` cuyo padre inmediato es `requisitos`; afirmar 386 respondibles, 76 items con padre `item`, 5 items con padre `overview`, 467 items preservados, cero `class` y 23 padres con subpartes.
- [X] T024 [US2] Revisar/aprobar T023 verificando en el JSON real `part.name`, `part.class`, padre inmediato `parts[]`, `props.label` y distribución 386/76/5; confirmar que hijos anidados se renderizan como contenido del requisito padre y nunca reciben respuesta independiente.
- [X] T025 [US2] Ejecutar `.\gradlew.bat :domain:test :data:testDebugUnitTest --tests "*Requirement*Test"` y registrar fallo rojo esperado de clasificación antes de implementarla; distinguir fallo funcional de errores de compilación/configuración.
- [X] T026 [US2] Implementar `RequirementClassifier` en `domain/src/main/kotlin/com/ensapp/domain/catalog/RequirementClassifier.kt` y el mapeo de importación en `data/src/main/kotlin/com/ensapp/data/catalog/RequirementImportMapper.kt`; crear Requisito únicamente para `name == "item"` con padre inmediato `name == "requisitos"` y conservar los otros nodos como parts anidadas.
- [X] T027 [US2] Ejecutar `.\gradlew.bat :domain:test :data:testDebugUnitTest --tests "*Requirement*Test"` y dejar verdes T023: 386 respuestas independientes, 76 subitems, 5 items de overview y árbol OSCAL intacto.

### Consulta y navegación del catálogo

- [X] T028 [US2] Implementar consultas/repositorio de jerarquía Marco→Familia→Medida→Refuerzo→Requisito en `data/src/main/kotlin/com/ensapp/data/catalog/CatalogRepositoryImpl.kt`; soportar las cuatro medidas directamente en el marco `org` sin sintetizar una Familia.
- [X] T029 [US2] Implementar casos de uso de navegación y árbol de catálogo enriquecido con aplicabilidad del sistema seleccionado en `domain/src/main/kotlin/com/ensapp/domain/catalog/BrowseCatalogUseCases.kt`.
- [X] T030 [US2] Crear ViewModels y pantallas Compose para marcos, familias, medidas, refuerzos y detalle de requisito en `presentation/src/main/kotlin/com/ensapp/presentation/catalog/`; usar texto/icono y colores fijos gris=N/A, verde=básica, amarillo=media, rojo=alta.
- [X] T031 [US2] Implementar filtro “mostrar todo” en `presentation/src/main/kotlin/com/ensapp/presentation/catalog/`; conservar visibles en gris los elementos no aplicables y deshabilitar navegación de respuesta para ellos.

**Checkpoint**: US2 se prueba con un sistema y con datos de catálogo cargados, sin respuestas persistidas.

---

## Phase 5: User Story 3 — responder requisitos aplicables (P1)

**Goal**: registrar, editar y recuperar madurez L0–L5 y nota por requisito y sistema; distinguir fila
ausente (sin responder) de L0 explícito.

**Independent Test**: guardar y reabrir respuestas de dos sistemas, verificar cifrado/persistencia y
que L0 nunca se interpreta como falta de respuesta.

### TDD: estado de respuesta y validación de requisito

- [X] T032 [US3] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/response/SaveRequirementResponseUseCaseTest.kt` con JUnit 5 + Mockito; comprobar sin fila = “sin responder”, niveles válidos `L0|L1|L2|L3|L4|L5`, modificación de nota y rechazo de requisito no aplicable/no respondible con repositorio mockeado.
- [X] T033 [US3] Revisar/aprobar expectativas T032 contra US3 y Clarificación 5 de `specs/001-ens-catalog-management/spec.md` y el modelo `RequirementResponse` de `specs/001-ens-catalog-management/data-model.md`.
- [X] T034 [US3] Ejecutar `.\gradlew.bat :domain:test --tests "*SaveRequirementResponseUseCaseTest"` y registrar el rojo esperado antes de implementar el caso de uso.
- [X] T035 [US3] Implementar `RequirementResponse`, puerto/repositorio y `SaveRequirementResponseUseCase` en `domain/src/main/kotlin/com/ensapp/domain/response/`; modelar “sin responder” por ausencia de fila y solo aceptar madurez `L0`–`L5`.
- [X] T036 [US3] Ejecutar `.\gradlew.bat :domain:test --tests "*SaveRequirementResponseUseCaseTest"` y dejar verdes los casos T032, incluidos el L0 explícito y el aislamiento por `(system_id, requirement_id)`.

### Persistencia cifrada y UI de respuestas

- [X] T037 [US3] Implementar entidades/DAO y repositorio SQLite de respuestas en `data/src/main/kotlin/com/ensapp/data/response/`; usar clave primaria `(system_id, requirement_id)`, cifrar madurez/nota AES-GCM y no insertar una fila para un requisito aún no respondido.
- [X] T038 [US3] Crear ViewModel y formulario Compose de respuesta en `presentation/src/main/kotlin/com/ensapp/presentation/response/`; permitir L0–L5 y nota libre, precargar valores guardados y bloquear requisitos N/A.
- [X] T039 [US3] Añadir pruebas instrumentadas de persistencia en `data/src/androidTest/kotlin/com/ensapp/data/response/RequirementResponsePersistenceTest.kt`; cerrar/reabrir repositorio y comprobar durabilidad, aislamiento por sistema y distinción de fila ausente/L0.

**Checkpoint**: US3 funciona sobre los 386 nodos Requisito estructurales y solo cuando aplican.

---

## Phase 6: User Story 4 — selección única de refuerzos disyuntivos (P2)

**Goal**: aplicar los diez params OSCAL `tipo-param: seleccion-refuerzo` con cardinalidad `one`.

**Independent Test**: abrir cada parámetro de `op.acc.5`, `op.acc.6`, `mp.com.4`, `mp.s.2`; nunca
mantener dos alternativas activas simultáneamente por sistema/parámetro y permitir cambio explícito.

### TDD: selección válida y exclusión única

- [X] T040 [US4] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/catalog/SelectReinforcementUseCaseTest.kt` con JUnit 5 + Mockito; usar los diez grupos de selección/`choice` reales y comprobar cardinalidad `one`, rechazo de ID fuera de opciones, una selección por `(system_id, parameter_id)`, cambio de alternativa y aislamiento entre sistemas.
- [X] T041 [US4] Revisar/aprobar fixtures/assertions de T040 contra `datos/ENS_Anexo_II_rev_10.json` (`params[].props`, categoría/nivel y `select.choice[]`) y contra US4, criterios 1–2.
- [X] T042 [US4] Ejecutar `.\gradlew.bat :domain:test --tests "*SelectReinforcementUseCaseTest"` y registrar el rojo esperado antes de implementar selección única.
- [X] T043 [US4] Implementar validación y `SelectReinforcementUseCase` en `domain/src/main/kotlin/com/ensapp/domain/catalog/`; cambiar la elección de forma atómica y rechazar opciones que no estén declaradas en `choice`.
- [X] T044 [US4] Ejecutar `.\gradlew.bat :domain:test --tests "*SelectReinforcementUseCaseTest"` y dejar verdes los casos T040.

### Persistencia y presentación de selección

- [X] T045 [US4] Implementar persistencia Room de selección en `data/src/main/kotlin/com/ensapp/data/catalog/DisjunctiveSelectionDao.kt` y `DisjunctiveSelectionRepository.kt`; imponer `UNIQUE(system_id, parameter_id)` y cifrar el refuerzo elegido.
- [X] T046 [US4] Renderizar grupos disyuntivos como selección única en `presentation/src/main/kotlin/com/ensapp/presentation/catalog/ReinforcementSelection.kt`; mostrar únicamente opciones OSCAL válidas y permitir cambiar la selección sin activar dos alternativas.

**Checkpoint**: selección única persistida, independiente por sistema y trazable al parámetro OSCAL.

---

## Phase 7: User Story 5 — consultar estado de cumplimiento (P2)

**Goal**: resumir requisitos aplicables, respondidos y pendientes, con desglose CIDAT y navegación
desde contadores.

**Independent Test**: fabricar configuraciones/respuestas conocidas en SQLite de prueba y verificar
contadores/porcentajes globales y por dimensión; abrir desde un contador el listado correspondiente.

### TDD: cálculo de estado

- [X] T047 [US5] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/status/GetComplianceSummaryUseCaseTest.kt` con JUnit 5 + Mockito; cubrir aplicables/respondidos/pendientes, denominador, respuesta L0, requisitos no aplicables y desglose por las cinco dimensiones CIDAT.
- [X] T048 [US5] Revisar/aprobar T047 contra US5 y las reglas de madurez/aplicabilidad de `specs/001-ens-catalog-management/data-model.md`; acordar resultados de cero aplicables y evitar doble conteo de dimensiones.
- [X] T049 [US5] Ejecutar `.\gradlew.bat :domain:test --tests "*GetComplianceSummaryUseCaseTest"` y registrar el rojo esperado antes de implementar el resumen.
- [X] T050 [US5] Implementar cálculo de `ComplianceSummary` y `GetComplianceSummaryUseCase` en `domain/src/main/kotlin/com/ensapp/domain/status/`; excluir N/A del denominador aplicable y contabilizar L0 como respondido.
- [X] T051 [US5] Ejecutar `.\gradlew.bat :domain:test --tests "*GetComplianceSummaryUseCaseTest"` y dejar verdes los casos T047.

### Integración de estado

- [X] T052 [US5] Implementar consultas agregadas por sistema y dimensión en `data/src/main/kotlin/com/ensapp/data/status/ComplianceStatusRepository.kt`; leer de SQLite las respuestas cifradas y descifrarlas antes del cálculo de dominio.
- [X] T053 [US5] Crear ViewModel/dashboard de estado en `presentation/src/main/kotlin/com/ensapp/presentation/status/`; presentar número/porcentaje general y por CIDAT y enlazar contadores a filtros navegables de requisitos.

**Checkpoint**: los totales del dashboard coinciden con el almacenamiento del sistema seleccionado.

---

## Phase 8: User Story 6 — exportar informe PDF (P2)

**Goal**: generar en cualquier avance un PDF local con configuración, resumen y detalle jerárquico.

**Independent Test**: exportar una evaluación parcial, contrastar sistema/configuración/estado/respuestas
con la base y completar en menos de 5 segundos sin red.

### TDD: instantánea y contenido del PDF

- [X] T054 [US6] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/report/BuildReportSnapshotUseCaseTest.kt` con JUnit 5 + Mockito; comprobar sistema seleccionado, resumen, orden/jerarquía, 386 Requisitos, respuestas presentes y diferencia entre vacío y L0.
- [X] T055 [US6] Revisar/aprobar las expectativas de T054 contra US6 y `specs/001-ens-catalog-management/contracts/export-format.md`, usando una evaluación parcial representativa.
- [X] T056 [US6] Ejecutar `.\gradlew.bat :domain:test --tests "*BuildReportSnapshotUseCaseTest"` y registrar el rojo esperado antes de implementar la instantánea.
- [X] T057 [US6] Implementar `BuildReportSnapshotUseCase` en `domain/src/main/kotlin/com/ensapp/domain/report/`; tomar un estado consistente del sistema y exponer solo datos del mismo `system_id`.
- [X] T058 [US6] Ejecutar `.\gradlew.bat :domain:test --tests "*BuildReportSnapshotUseCaseTest"` y dejar verdes los casos T054.

### Generación e interacción PDF

- [X] T059 [US6] Implementar generador PDF en `data/src/main/kotlin/com/ensapp/data/report/PdfReportWriter.kt` usando exclusivamente `android.graphics.pdf.PdfDocument`; incluir configuración CIDAT, resumen, aplicabilidad y detalle en jerarquía normativa.
- [X] T060 [US6] Implementar exportación a URI elegida por el usuario y aviso previo visible de seguridad en `presentation/src/main/kotlin/com/ensapp/presentation/report/PdfExportViewModel.kt` y `PdfExportScreen.kt`; explicar que el fichero final no lo cifra la app y su protección recae en usuario/organización, sin cambiar AES-GCM/SQLite internos.
- [ ] T061 [US6] Medir generación desde una instantánea de prueba y validar campos/paginación en `data/src/androidTest/kotlin/com/ensapp/data/report/PdfReportWriterTest.kt`; cumplir el objetivo de menos de 5 segundos y funcionar sin red.

**Checkpoint**: PDF auditable en progreso y aviso de limitación visible antes de escritura.

---

## Phase 9: User Story 7 — exportar informe CSV (P3)

**Goal**: generar CSV UTF-8 con una fila por requisito normativo, metadatos, aplicabilidad y respuesta.

**Independent Test**: validar cabecera, número de filas, escaping RFC 4180, campos de aplicación y
diferencia de madurez vacía/L0 con un sistema y una evaluación parcial.

### TDD: formato CSV

- [X] T062 [US7] Escribir primero `domain/src/test/kotlin/com/ensapp/domain/report/CsvReportWriterTest.kt` con JUnit 5; comprobar UTF-8, cabecera del contrato, 386 filas de requisito, escaping de comas/comillas/saltos de línea, N/A, `REVISAR`, madurez vacía y L0.
- [X] T063 [US7] Revisar/aprobar T062 contra `specs/001-ens-catalog-management/contracts/export-format.md` y confirmar que items subviñeta/overview nunca crean filas independientes.
- [X] T064 [US7] Ejecutar `.\gradlew.bat :domain:test --tests "*CsvReportWriterTest"` y registrar el rojo esperado antes de implementar serialización CSV.
- [X] T065 [US7] Implementar `CsvReportWriter` en `domain/src/main/kotlin/com/ensapp/domain/report/CsvReportWriter.kt`; seguir cabecera/orden del contrato y escaping RFC 4180, incluyendo todos los requisitos aunque sean N/A.
- [X] T066 [US7] Ejecutar `.\gradlew.bat :domain:test --tests "*CsvReportWriterTest"` y dejar verdes los casos T062.
- [X] T067 [US7] Implementar selector URI y aviso de cifrado externo previo a escribir CSV en `presentation/src/main/kotlin/com/ensapp/presentation/report/CsvExportViewModel.kt` y `CsvExportScreen.kt`; no persistir copia exportada interna en claro.

**Checkpoint**: CSV corresponde exactamente a la instantánea seleccionada y advierte sobre custodia externa.

---

## Phase 10: User Story 8 — accesibilidad (P2, transversal)

**Goal**: que todas las pantallas sean utilizables con TalkBack y escalado dinámico de fuentes; no
comunicar aplicabilidad únicamente por color.

**Independent Test**: pruebas semánticas de componentes y recorrido manual/instrumentado de todos los
flujos con TalkBack y fuentes grandes.

- [X] T068 [US8] Añadir pruebas UI de semántica accesible en `presentation/src/androidTest/kotlin/com/ensapp/presentation/AccessibilitySemanticsTest.kt`; comprobar anuncios de aplicabilidad, respuesta, selección interactiva, labels/acciones y ausencia de nodos interactivos mudos.
- [X] T069 [US8] Revisar la semántica requerida contra US8 y Material 3; implementar descripciones/roles/iconos/texto para gris=N/A, verde=básica, amarillo=media y rojo=alta en componentes de catálogo, respuesta, selección y estado bajo `presentation/src/main/kotlin/com/ensapp/presentation/`.
- [ ] T070 [US8] Corregir layouts para escalado de fuente dinámica en pantallas Compose bajo `presentation/src/main/kotlin/com/ensapp/presentation/`; evitar alturas rígidas y verificar ausencia de cortes/solapamientos con fuente ampliada.
- [ ] T071 [US8] Ejecutar `.\gradlew.bat :presentation:connectedDebugAndroidTest` y comprobar T068 más los flujos de TalkBack/fuente aumentada en el emulador.

**Checkpoint**: estado visible y anunciado sin depender solo del color.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: seguridad, compatibilidad normativa, regresión completa y documentación de entrega.

- [X] T072 Añadir una prueba de regresión del catálogo completo en `data/src/test/kotlin/com/ensapp/data/catalog/CatalogRevisionRegressionTest.kt`; fijar conteos 3/15/73/133/386, la estructura especial de `org`, 10 params y el conjunto de 41 refuerzos enumerado en `specs/001-ens-catalog-management/quickstart.md`.
- [ ] T073 Verificar migraciones Room, ausencia de operaciones destructivas y claves/ciphertext no respaldados en `data/src/androidTest/kotlin/com/ensapp/data/security/EncryptedPersistenceTest.kt`; confirmar que AES-GCM permanece aplicado a datos internos aunque las exportaciones externas no se cifren.
- [X] T074 Añadir prueba de manifest/artefacto en `app/src/test/kotlin/com/ensapp/app/OfflineManifestTest.kt`; fallar si el APK declara permiso `android.permission.INTERNET` o introduce dependencias de red/cloud.
- [X] T075 Revisar cobertura TDD JUnit 5 + Mockito de todos los casos de uso de dominio y cerrar el conjunto completo con `.\gradlew.bat testDebugUnitTest`; corregir regresiones en módulos afectados.
- [ ] T076 Ejecutar validación end-to-end de `specs/001-ens-catalog-management/quickstart.md`, incluidos `.\gradlew.bat :app:assembleDebug`, las pruebas instrumentadas disponibles y el objetivo PDF/CSV menor de 5 segundos; registrar limitaciones de entorno en el informe de ejecución.
- [X] T077 Actualizar `README.md` y `specs/001-ens-catalog-management/quickstart.md` con módulos, comandos Gradle reales, revisión OSCAL activa, la asunción normativa `APLICABILIDAD_HEREDADA_DE_MEDIDA` y la responsabilidad del usuario/organización sobre copias PDF/CSV exportadas.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: iniciar primero; scaffolding Gradle/módulos es requisito explícito porque no existe en el repositorio.
- **Foundational (Phase 2)**: depende del setup y bloquea todos los user stories: dominio común, importación JSON, Room, cifrado y manifest offline.
- **User stories (Phases 3–10)**: empiezan tras Phase 2; respetar dependencias de producto abajo y el orden TDD dentro de cada bloque.
- **Polish (Phase 11)**: después de las historias que se vayan a entregar.

### User story dependencies

```text
US1 (P1, configuración + aplicabilidad)
 ├── US2 (P1, navegación + clasificación 386 Requisito)
 │    ├── US3 (P1, respuestas)
 │    │    ├── US5 (P2, resumen)
 │    │    │    ├── US6 (P2, PDF)
 │    │    │    └── US7 (P3, CSV)
 │    │    └── US4 (P2, selección de refuerzo; requiere catálogo/navegación)
 │    └── US4 (P2, selección única de refuerzo)
 └── US8 (P2, transversal a pantallas construidas; completar después de integrar pantallas)
```

La selección de refuerzo US4 necesita US2 para acceder a los controles/params; puede implementarse
en paralelo con la captura de respuesta US3 si el modelo de catálogo de US2 ya está terminado. US5
requiere resultados de US1 y respuestas de US3. Las exportaciones dependen de la instantánea y resumen
de US5. US8 puede adelantarse por componente, pero su comprobación completa espera las pantallas US1–US7.

### TDD obligatorio por grupo de lógica

| Lógica | Orden de tareas |
|---|---|
| Importación/fidelidad del catálogo | T005 prueba escrita → T006 revisión/aprobación → T007 rojo → T008 implementación → T009 verde |
| Aplicabilidad categoría/dimensión/herencia | T013 prueba escrita → T014 revisión/aprobación → T015 rojo → T016 implementación (reutilizar `ApplicabilityPolicy.kt`) → T017 verde |
| Clasificación de Requisito 386/76/5 | T023 prueba escrita → T024 revisión/aprobación → T025 rojo → T026 implementación → T027 verde |
| Respuestas L0–L5 | T032 prueba escrita → T033 revisión/aprobación → T034 rojo → T035 implementación → T036 verde |
| Selección única disyuntiva | T040 prueba escrita → T041 revisión/aprobación → T042 rojo → T043 implementación → T044 verde |
| Resumen de cumplimiento | T047 prueba escrita → T048 revisión/aprobación → T049 rojo → T050 implementación → T051 verde |
| Instantánea de informe | T054 prueba escrita → T055 revisión/aprobación → T056 rojo → T057 implementación → T058 verde |
| Serialización CSV | T062 prueba escrita → T063 revisión/aprobación → T064 rojo → T065 implementación → T066 verde |

## Parallel opportunities

- Dentro de Setup, T001 es prerequisito de T002/T003; luego documentación Gradle, configuración JUnit y preparación de manifests pueden dividirse si no editan los mismos archivos.
- En Foundational, T004 puede desarrollarse mientras se prepara el test T005; Room (T010), Keystore (T011) y manifest offline (T012) pueden separarse tras definir los módulos, evitando conflictos en `data/build.gradle.kts`.
- Tras la fundación, la revisión/aprobación de fixtures puede ser trabajo separado de la UI de la historia correspondiente; los pasos rojo/implementación/verde de cada bloque TDD permanecen secuenciales.
- Al terminar US2, US3 y US4 se pueden implementar en paralelo en archivos/módulos distintos. US6 y US7 también pueden avanzar en paralelo tras completar la base de informes de US5.
- US8 puede distribuirse por grupos de pantallas; la prueba global se ejecuta cuando estén integradas todas.

## Parallel Example: User Story 1

```text
Secuencia obligatoria TDD en dominio:
1. T013 escribir pruebas
2. T014 revisar/aprobar fixtures y assertions
3. T015 ejecutar y comprobar rojo
4. T016 implementar el cálculo, actualizando el ApplicabilityPolicy.kt existente
5. T017 ejecutar y comprobar verde

Después, con contratos de dominio estables:
- Un implementador puede completar T019 (Room/repositorio de sistemas).
- Otro puede preparar T020–T021 (ViewModel/Compose), integrando cuando T018–T019 estén disponibles.
```

## Implementation Strategy

### MVP primero (US1)

1. Completar Phase 1: crear explícitamente Gradle Wrapper y los cuatro módulos.
2. Completar Phase 2: importar catálogo, habilitar SQLite/Room, cifrado interno y modo offline.
3. Completar Phase 3 (US1): TDD de aplicabilidad y gestión multi-sistema.
4. **Detener y validar**: crear dos sistemas y comprobar su configuración/aplicabilidad independiente; ejecutar tests de dominio antes de añadir respuestas.

### Entrega incremental

1. Setup + Foundation → build instalable y catálogo importado.
2. US1 → MVP de configuración/aplicabilidad por sistema.
3. US2 → navegación filtrada y clasificación de 386 requisitos independientes.
4. US3 → respuestas persistidas; US4 → exclusividad de refuerzos; US5 → dashboard.
5. US6/US7 → exportaciones con aviso explícito de custodia externa.
6. US8 + Polish → accesibilidad y gates de regresión/seguridad.

## Notes

- Cada línea de tarea comienza con checkbox, ID secuencial `T001`–`T077` y un path concreto.
- Las fases de historia usan etiquetas `[US1]`–`[US8]`; Setup, Foundational y Polish no llevan etiqueta.
- `[P]` se incluye solo en tareas realmente paralelizables; ninguna pareja TDD rojo/implementación/verde se marca paralela.
- `domain/src/main/kotlin/com/ensapp/domain/catalog/ApplicabilityPolicy.kt` ya existe: T016 lo amplía y prueba, no lo recrea.
- El detalle de 386 respondibles, herencia normativa de 41 refuerzos y limitación aceptada de cifrado externo sigue las decisiones actualizadas en `spec.md`/plan y los diseños enlazados.
