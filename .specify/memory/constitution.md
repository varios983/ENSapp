<!--
Sync Impact Report
Version change: [template] → 1.0.0 (ratificación inicial)
Amendment 1.0.2 (PATCH): corregido el recuento de familias del catálogo (17 → 15; 4 medidas del
  marco organizativo sin familia propia), verificado contra ENS_Anexo_II_rev_10.json.
Modified principles: N/A (creación inicial, no hay renombrados)
Added sections:
  - Core Principles: I. Seguridad por Diseño; II. Fidelidad al Catálogo ENS/OSCAL;
    III. Test-First en Lógica de Negocio; IV. Arquitectura Mantenible y Modular;
    V. Accesibilidad y Coherencia Visual; VI. Persistencia Local Única y Portabilidad de Datos
  - Restricciones Técnicas y Stack (SECTION_2)
  - Flujo de Desarrollo y Puertas de Calidad (SECTION_3)
  - Governance
Removed sections: ninguna (documento inicial)
Deferred / TODO: ninguno. RATIFICATION_DATE y LAST_AMENDED_DATE se fijan a la fecha de esta
  ratificación inicial (2026-09-24) al no existir una fecha anterior de adopción.
Templates requiring follow-up: revisar .specify/templates/plan-template.md,
  spec-template.md y tasks-template.md para alinear "Constitution Check" con los
  principios I-VI antes de ejecutar /speckit-plan.
-->

# ENSapp Constitution

## Core Principles

### I. Seguridad por Diseño (NON-NEGOTIABLE)
Todos los datos sensibles MUST cifrarse en reposo y en tránsito. La aplicación MUST funcionar
íntegramente offline: no se permite el consumo de APIs externas ni de servicios cloud de ningún
tipo. La superficie de ataque se minimiza por diseño (sin permisos de red, sin componentes
exportados innecesarios, principio de mínimo privilegio en permisos de Android). Rationale: ENSapp
gestiona el nivel de cumplimiento de seguridad de sistemas de información de una Administración
Pública; una brecha en la propia herramienta de evaluación sería contradictoria con su propósito y
comprometería la confidencialidad de datos de cumplimiento normativo.

### II. Fidelidad al Catálogo ENS/OSCAL
El modelo de datos de la aplicación MUST reflejar fielmente la estructura jerárquica del catálogo
de medidas del Anexo II del Esquema Nacional de Seguridad (Real Decreto 311/2022, BOE-A-2022-7191),
tal como está codificado en formato OSCAL 1.1.3 en `ENS_Anexo_II_rev_10.json`: 3 marcos (organizativo, operacional, medidas de protección) → familias (15 en total: 7 en el marco operacional y 8 en el de medidas de protección; las 4 medidas del marco organizativo, `org.1`-`org.4`, cuelgan directamente del marco sin pertenecer a ninguna familia) → 73 medidas → 133 refuerzos (`class: "ens-refuerzo"`) → 386 requisitos. El catálogo define **dos criterios de aplicabilidad mutuamente
excluyentes por control**, señalados mediante la propiedad `aplicacion-por` (namespace `urn:es:ens`),
y la app MUST leer y respetar ambos, no solo uno:
- `categoria`: el control aplica según la categoría global del sistema (BÁSICA/MEDIA/ALTA).
- `nivel-dimension`: el control aplica según el nivel (BAJO/MEDIO/ALTO) asignado individualmente a
  una o varias dimensiones concretas (Confidencialidad, Integridad, Disponibilidad, Autenticidad,
  Trazabilidad — CIDAT), con independencia de la categoría global.

Por tanto, el flujo de entrada de datos NO puede limitarse a pedir una única categoría global: MUST
solicitar también, cuando existan controles `nivel-dimension` en el ámbito evaluado, el nivel de cada
una de las 5 dimensiones. Además, la app MUST soportar el patrón de refuerzo disyuntivo (`tipo-param:
seleccion-refuerzo`, presente en medidas como `op.acc.5`, `op.acc.6`, `mp.com.4` y `mp.s.2`), donde el
usuario debe seleccionar un único refuerzo aplicable de entre varios alternativos, no todos a la vez.
El código de colores es fijo y MUST respetarse en toda la UI: gris = no aplica (N/A), verde = básica,
amarillo = media, rojo = alta. Rationale: la utilidad y la validez normativa de ENSapp dependen por
completo de no desviarse del catálogo oficial ni de simplificar sus reglas reales de aplicabilidad.

### III. Test-First en Lógica de Negocio (NON-NEGOTIABLE)
Toda la lógica de dominio y los casos de uso (cálculo de aplicabilidad por categoría/dimensión,
cálculo de estado de cumplimiento, generación de informes) MUST desarrollarse con TDD: test escrito →
aprobado → test en rojo → implementación → test en verde. MUST usarse JUnit 5 y Mockito. La cobertura
de tests unitarios sobre la lógica de negocio es obligatoria y se verifica en cada Pull Request.

### IV. Arquitectura Mantenible y Modular
La aplicación MUST estructurarse como una app Android nativa multi-módulo, aplicando patrones de
diseño probados (p. ej. Clean Architecture con separación domain/data/presentation, MVVM en la capa
de presentación) para maximizar la facilidad de mantenimiento. SHOULD priorizarse la simplicidad
(YAGNI) sobre la abstracción prematura; cualquier complejidad añadida MUST justificarse
explícitamente en el plan técnico correspondiente.

### V. Accesibilidad y Coherencia Visual
La interfaz MUST seguir Material Design 3. MUST soportarse TalkBack y el escalado de fuentes
dinámicas del sistema en todas las pantallas. El código de colores por categoría (Principio II)
MUST implementarse de forma accesible (no solo por color, también por texto/icono) para no excluir
a usuarios con daltonismo.

### VI. Persistencia Local Única y Portabilidad de Datos
SQLite MUST ser la única fuente de datos local (sin backend remoto, en coherencia con el Principio I).
La aplicación MUST permitir exportar el estado de cumplimiento en dos formatos: PDF (generado con
`android.graphics.pdf.PdfDocument`, sin dependencias de terceros como iText en esta fase inicial) y
CSV. Los informes exportados MUST poder generarse en cualquier momento del proceso de evaluación, no
solo al finalizarlo.

## Restricciones Técnicas y Stack

- Lenguaje: Kotlin 2.0+; toolchain Android sobre Java 17 como mínimo.
- Plataforma: Android nativo; minSdk 26; targetSdk 35; compatible con teléfonos y tabletas.
- Persistencia: SQLite exclusivamente (ver Principio VI).
- Generación de PDF: `android.graphics.pdf.PdfDocument` (sin iText en la versión inicial).
- Sin conexión a red: la app MUST declararse y funcionar sin el permiso `INTERNET`.
- Testing: JUnit 5 + Mockito para dominio y casos de uso (ver Principio III).
- UI: Material Design 3 + soporte TalkBack y fuentes dinámicas (ver Principio V).

## Flujo de Desarrollo y Puertas de Calidad

Toda Pull Request MUST verificar el cumplimiento de los seis Core Principles antes de aprobarse.
Ninguna PR que introduzca una llamada de red, una dependencia cloud, o almacenamiento fuera de SQLite
puede aprobarse (Principios I y VI). Ninguna PR que module lógica de dominio o casos de uso sin sus
tests unitarios correspondientes (JUnit 5/Mockito) puede aprobarse (Principio III). Los cambios en el
modelo de datos de medidas/refuerzos/requisitos MUST justificar su trazabilidad contra
`ENS_Anexo_II_rev_10.json` (Principio II).

## Governance

Esta Constitución prevalece sobre cualquier otra práctica, plantilla o convención usada en el
desarrollo de ENSapp. Toda enmienda MUST documentarse en este fichero junto con su Sync Impact
Report, MUST justificar el tipo de incremento de versión (MAJOR: incompatibilidad o eliminación de
principios; MINOR: nuevo principio o ampliación material; PATCH: aclaraciones sin cambio semántico), y
MUST revisar la alineación de `.specify/templates/plan-template.md`, `spec-template.md` y
`tasks-template.md` tras el cambio. La complejidad añadida en cualquier plan técnico MUST justificarse
explícitamente frente a los principios aquí recogidos.


**Version**: 1.0.2 | **Ratified**: 2026-09-24 | **Last Amended**: 2026-10-08
