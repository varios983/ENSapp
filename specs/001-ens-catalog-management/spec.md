# Especificación funcional: ENSapp

**Rama/feature**: `001-ensapp-mvp` (ajustar según convención Spec-Kit)
**Estado**: Revisión 2 — incorpora clarificaciones 5-7 surgidas durante `/speckit-plan` (granularidad
de requisitos, refuerzos sin regla propia, alcance del cifrado en exportaciones)
**Basado en**: descripción funcional del usuario + `constitution.md` v1.0.0 + `ENS_Anexo_II_rev_10.json` (OSCAL 1.1.3) + "Decisiones de diseño..." (AEAD, v1.0)

## 1. Resumen

ENSapp es una aplicación Android nativa, offline-first, que permite a un usuario gestionar de forma
interactiva el catálogo de medidas de seguridad del Anexo II del Esquema Nacional de Seguridad (ENS),
codificado en OSCAL 1.1.3. El usuario configura el sistema de información que va a evaluar (categoría
global y nivel por dimensión CIDAT), la app filtra automáticamente qué medidas/refuerzos/requisitos
aplican, el usuario responde a cada requisito aplicable, y en cualquier momento puede consultar el
estado de cumplimiento y exportar informes en PDF o CSV.

## 2. Usuarios y alcance

- **Usuario objetivo**: personal responsable de seguridad/cumplimiento normativo (p. ej. responsable
  de seguridad, auditor interno) de un organismo público sujeto al ENS.
- **Alcance de esta especificación**: la gestión de varios Sistemas de Información dentro de la misma
  instalación de la app, cada uno con su propia categoría (BÁSICA/MEDIA/ALTA) y niveles CIDAT
  independientes, y para cada uno la navegación del catálogo, cumplimentación de requisitos (con
  evidencia en texto libre), consulta de estado y exportación PDF/CSV. Quedan fuera de alcance en este
  documento: gestión multiusuario y sincronización entre dispositivos.

## 3. Historias de usuario y criterios de aceptación

### US1 — Gestionar varios Sistemas de Información y configurar cada uno (Prioridad: P1)

Como responsable de seguridad, quiero poder crear y mantener varios Sistemas de Información dentro de
la misma app, indicando para cada uno su categoría (BÁSICA/MEDIA/ALTA) y el nivel de cada dimensión
CIDAT (Confidencialidad, Integridad, Disponibilidad, Autenticidad, Trazabilidad), para evaluar de forma
independiente el cumplimiento de cada sistema que gestiono.

**Por qué esta prioridad**: sin esta configuración inicial por sistema no se puede filtrar el catálogo
ni distinguir la evaluación de un sistema de otra; es el punto de entrada obligatorio de toda la app.

**Prueba independiente**: se puede verificar por sí sola creando dos o más sistemas con configuraciones
distintas y comprobando que el conjunto de medidas/refuerzos/requisitos marcados como "aplica" es
correcto y no se mezcla entre sistemas, sin necesidad de que exista aún pantalla de respuesta.

**Criterios de aceptación**:
1. **Dado** que el usuario abre la app por primera vez, **cuando** no existe todavía ningún Sistema de
   Información, **entonces** la app le lleva a crear el primero antes de poder acceder al catálogo.
2. **Dado** que el usuario ya tiene uno o más sistemas creados, **cuando** abre la app, **entonces**
   puede elegir sobre qué sistema quiere trabajar o crear uno nuevo adicional.
3. **Dado** el asistente de creación/edición de un sistema, **cuando** el usuario selecciona una
   categoría global, **entonces** la app solicita también el nivel (BAJO/MEDIO/ALTO) de cada una de las
   5 dimensiones CIDAT para ese sistema.
4. **Dado** que existen controles con `aplicacion-por: categoria` y otros con
   `aplicacion-por: nivel-dimension`, **cuando** se guarda la configuración de un sistema, **entonces**
   la app calcula la aplicabilidad de cada medida/refuerzo/requisito para ese sistema combinando ambos
   criterios de forma independiente por control (nunca simplificando a un único criterio global).
5. **Dado** un sistema ya configurado, **cuando** el usuario decide modificar su categoría o algún
   nivel de dimensión, **entonces** la app permite editar la configuración y recalcula la aplicabilidad
   solo para ese sistema, avisando de que algunas respuestas ya dadas podrían dejar de aplicar.
6. **Dado** varios sistemas ya creados, **cuando** el usuario navega el catálogo o consulta el estado
   (US2, US5), **entonces** los datos mostrados (aplicabilidad y respuestas) corresponden siempre al
   sistema seleccionado en ese momento, sin mezclarse con los de otros sistemas.

### US2 — Navegar el catálogo filtrado por aplicabilidad (Prioridad: P1)

Como responsable de seguridad, quiero navegar el catálogo (marcos → familias → medidas → refuerzos →
requisitos) viendo con claridad qué aplica y qué no mediante el código de colores, para orientarme
rápidamente sobre dónde tengo trabajo pendiente.

**Por qué esta prioridad**: es la pantalla principal de trabajo; sin ella no hay forma de llegar a los
requisitos que hay que responder.

**Prueba independiente**: se puede probar cargando un sistema ya configurado (US1) y verificando que la
navegación y el color mostrado por nodo coinciden con la aplicabilidad calculada, sin necesidad de que
existan aún respuestas guardadas.

**Criterios de aceptación**:
1. **Dado** un sistema configurado, **cuando** el usuario abre el listado de marcos/familias/medidas,
   **entonces** cada nodo se muestra con el color correspondiente a su categoría de aplicabilidad
   efectiva (gris=N/A, verde=básica, amarillo=media, rojo=alta).
2. **Dado** una medida con refuerzos, **cuando** el usuario la abre, **entonces** ve el nombre,
   descripción y refuerzos de la medida, y solo los refuerzos/requisitos que aplican quedan habilitados
   para respuesta.
3. **Dado** un requisito que no aplica al sistema configurado, **cuando** el usuario lo visualiza,
   **entonces** aparece en gris y no es seleccionable para respuesta.
4. **Dado** que el usuario quiere ver también los elementos no aplicables, **cuando** activa un filtro
   "mostrar todo", **entonces** la app muestra igualmente los elementos en gris (N/A) sin permitir
   respuesta sobre ellos.

### US3 — Responder a los requisitos aplicables (Prioridad: P1)

Como responsable de seguridad, quiero responder cada requisito aplicable, para dejar constancia del
grado de cumplimiento real del sistema frente al ENS.

**Por qué esta prioridad**: es el núcleo funcional de la app — sin captura de respuestas no hay
evaluación de cumplimiento que mostrar ni exportar.

**Prueba independiente**: se puede probar de forma aislada sobre un conjunto de requisitos ya marcados
como aplicables (resultado de US1+US2), comprobando que las respuestas se guardan y son recuperables.

**Criterios de aceptación**:
1. **Dado** un requisito aplicable, **cuando** el usuario lo abre, **entonces** puede registrar su
   grado de cumplimiento eligiendo uno de los 6 niveles de madurez del ENS (L0 Inexistente, L1 Inicial,
   L2 Reproducible, L3 Proceso definido, L4 Gestionado y medible, L5 Optimizado) y añadir una nota de
   texto libre como evidencia (en esta v1 no se admiten ficheros adjuntos).
2. **Dado** un requisito ya respondido, **cuando** el usuario vuelve a él, **entonces** ve el nivel de
   madurez y la nota de texto previamente guardados y puede modificarlos.
3. **Dado** que el usuario cierra la app en cualquier momento, **cuando** vuelve a abrirla, **entonces**
   todas las respuestas (nivel de madurez + nota) dadas hasta ese momento se conservan (persistidas en
   SQLite, cifradas en reposo), asociadas al sistema de información correspondiente.
4. **Dado** un requisito sin respuesta todavía, **cuando** el usuario lo visualiza, **entonces** se
   distingue claramente de uno en nivel L0 (Inexistente), ya que "sin responder" y "L0" son estados
   distintos: el primero es ausencia de evaluación, el segundo es una evaluación explícita de
   incumplimiento total.

### US4 — Seleccionar el refuerzo aplicable en medidas con refuerzo disyuntivo (Prioridad: P2)

Como responsable de seguridad, quiero que, en medidas con refuerzo disyuntivo (`tipo-param:
seleccion-refuerzo`, p. ej. op.acc.5, op.acc.6, mp.com.4, mp.s.2), la app me obligue a elegir un único
refuerzo entre las alternativas, para no responder por error a opciones mutuamente excluyentes.

**Por qué esta prioridad**: afecta solo a un subconjunto concreto de medidas; el resto de la app
(US1-US3) es funcional sin esto, pero es necesario para la fidelidad normativa completa (Principio II).

**Prueba independiente**: se puede probar de forma aislada abriendo específicamente una de las medidas
con refuerzo disyuntivo conocido y verificando el comportamiento de selección única.

**Criterios de aceptación**:
1. **Dado** una medida con refuerzo disyuntivo, **cuando** el usuario accede a ella, **entonces** la
   app presenta las alternativas de refuerzo como una selección única (no como checklist múltiple).
2. **Dado** que el usuario ya seleccionó un refuerzo alternativo, **cuando** intenta responder a otro
   refuerzo alternativo de la misma medida, **entonces** la app le impide tener dos alternativas activas
   a la vez y le permite cambiar la selección explícitamente.

### US5 — Consultar el estado de cumplimiento (Prioridad: P2)

Como responsable de seguridad, quiero consultar en cualquier momento un resumen del estado de
cumplimiento (qué aplica, qué aplica por dimensión, qué está respondido y qué queda pendiente), para
saber en qué punto está la evaluación sin recorrer todo el catálogo.

**Por qué esta prioridad**: es una vista de valor añadido sobre los datos ya capturados en US1-US3; no
bloquea la captura de datos, pero es necesaria antes de que la exportación (US6/US7) tenga sentido.

**Prueba independiente**: se puede probar generando datos vía US1-US3 y comprobando que el resumen
mostrado (contadores/porcentajes) coincide con el estado real almacenado.

**Criterios de aceptación**:
1. **Dado** un sistema con evaluación en curso, **cuando** el usuario abre la vista de estado,
   **entonces** ve, como mínimo, el número/porcentaje de requisitos aplicables, respondidos y
   pendientes.
2. **Dado** la vista de estado, **cuando** el usuario quiere desagregar por dimensión CIDAT,
   **entonces** puede ver el estado de cumplimiento separado por cada una de las 5 dimensiones.
3. **Dado** la vista de estado, **cuando** el usuario navega desde un contador (p. ej. "pendientes"),
   **entonces** accede directamente al listado filtrado de esos requisitos.

### US6 — Exportar informe en PDF (Prioridad: P2)

Como responsable de seguridad, quiero exportar el estado de cumplimiento a PDF en cualquier momento
del proceso, para poder compartirlo o archivarlo como evidencia documental.

**Prueba independiente**: se puede probar de forma aislada sobre datos existentes (US1-US3),
verificando que el PDF generado refleja fielmente el estado almacenado en ese instante.

**Criterios de aceptación**:
1. **Dado** una evaluación en cualquier estado de avance (0% a 100%), **cuando** el usuario solicita
   exportar a PDF, **entonces** la app genera el documento usando `android.graphics.pdf.PdfDocument`
   (sin dependencias de terceros) sin necesidad de conexión a red.
2. **Dado** el PDF generado, **cuando** el usuario lo abre, **entonces** contiene la configuración del
   sistema (categoría, niveles por dimensión), el resumen de estado (US5) y el detalle de
   medidas/refuerzos/requisitos con su respuesta.
3. **Dado** un informe exportado, **cuando** se genera, **entonces** la app muestra un aviso de que el
   fichero no queda cifrado por la app una vez escrito en su destino (ver Clarificación 7).

### US7 — Exportar informe en CSV (Prioridad: P3)

Como responsable de seguridad, quiero exportar los datos de cumplimiento a CSV, para poder tratarlos
posteriormente en otras herramientas (hojas de cálculo, otros sistemas de gestión).

**Prueba independiente**: igual que US6 pero para formato CSV; se puede validar de forma aislada
comprobando que cada fila del CSV corresponde a un requisito con sus metadatos y respuesta.

**Criterios de aceptación**:
1. **Dado** una evaluación en cualquier estado de avance, **cuando** el usuario solicita exportar a
   CSV, **entonces** se genera un fichero con una fila por requisito, incluyendo como mínimo:
   identificador jerárquico (marco/familia/medida/refuerzo/requisito), aplicabilidad efectiva, y
   respuesta/estado.
2. **Dado** un informe exportado, **cuando** se genera, **entonces** la app muestra un aviso de que el
   fichero no queda cifrado por la app una vez escrito en su destino (ver Clarificación 7).

### US8 — Accesibilidad (Prioridad: P2, transversal)

Como usuario con necesidades de accesibilidad, quiero poder usar TalkBack y el escalado de fuente del
sistema en todas las pantallas, y quiero que la información de aplicabilidad no dependa solo del
color, para poder usar la app en igualdad de condiciones.

**Prueba independiente**: se puede verificar de forma transversal sobre cualquier pantalla ya
construida (US1-US7), sin ser una funcionalidad propia con datos independientes.

**Criterios de aceptación**:
1. **Dado** cualquier pantalla de la app, **cuando** se activa TalkBack, **entonces** todos los
   elementos interactivos y el estado de aplicabilidad/respuesta se anuncian correctamente.
2. **Dado** un nodo del catálogo mostrado con el código de color, **cuando** se visualiza,
   **entonces** el estado (N/A/básica/media/alta) se comunica también mediante texto o icono, no solo
   color.
3. **Dado** el sistema con fuente dinámica del sistema aumentada, **cuando** el usuario navega la app,
   **entonces** el contenido se reajusta sin cortes ni solapamientos.

## 4. Requisitos funcionales

- **RF-01**: El sistema MUST cargar y mantener localmente el catálogo completo de
  `ENS_Anexo_II_rev_10.json` (3 marcos, 15 familias, 73 medidas, 133 refuerzos, 386 requisitos
  respondibles). Las 4 medidas del marco organizativo (`org.1`-`org.4`) pertenecen directamente
  al marco, sin familia intermedia: el modelo MUST soportarlas sin sintetizar una familia artificial.
- **RF-02**: El sistema MUST calcular la aplicabilidad efectiva de cada medida/refuerzo/requisito
  combinando, control a control, el criterio `categoria` y el criterio `nivel-dimension` según
  corresponda (nunca un único criterio global simplificado).
- **RF-03**: El sistema MUST impedir respuestas múltiples simultáneas en medidas con refuerzo
  disyuntivo (`tipo-param: seleccion-refuerzo`).
- **RF-04**: El sistema MUST persistir toda la configuración y respuestas exclusivamente en SQLite
  local, cifradas en reposo.
- **RF-05**: El sistema MUST permitir generar informes PDF y CSV en cualquier momento del proceso de
  evaluación, reflejando el estado real en el instante de exportación.
- **RF-06**: El sistema MUST funcionar íntegramente sin conexión (sin permiso `INTERNET`, sin llamadas
  a servicios externos o cloud).
- **RF-07**: El sistema MUST aplicar el código de colores fijo (gris/verde/amarillo/rojo) de forma
  accesible (texto/icono adicional al color) en toda la UI.

## 5. Entidades clave

- **SistemaInformación**: uno de los varios sistemas gestionados por el usuario (nombre, categoría
  global, nivel por cada dimensión CIDAT, fecha de creación/modificación). La app MUST permitir crear,
  editar y mantener varios en paralelo.
- **Marco / Familia / Medida / Refuerzo / Requisito**: jerarquía tomada del catálogo OSCAL, con sus
  metadatos (id, nombre, descripción, criterio de aplicabilidad, tipo de refuerzo si aplica). Es común
  a todos los sistemas; solo la aplicabilidad efectiva y las respuestas varían por sistema (el
  Requisito corresponde solo a los 386 nodos oficiales — ver Clarificación 5). La Familia es opcional
  en el marco organizativo, cuyas 4 medidas dependen directamente del Marco.
- **RespuestaRequisito**: vínculo entre un Requisito y un SistemaInformación concreto, con nivel de
  madurez (L0-L5, o "sin responder"), nota de texto libre como evidencia y fecha de última
  modificación.
- **InformeExportado**: registro (no necesariamente persistente) de una exportación PDF/CSV generada,
  con fecha y estado de avance capturado en ese momento.

## 6. Clarificaciones resueltas

1. **Multi-sistema**: la app MUST soportar varios Sistemas de Información en la misma instalación,
   cada uno con su propia categoría (BÁSICA/MEDIA/ALTA) — ver US1.
2. **Escala de estado de cumplimiento**: se usa la escala de madurez del propio ENS, de 6 niveles:
   - **L0 — Inexistente**: medida requerida por el ENS pero no implementada, sin planes inmediatos.
   - **L1 — Inicial**: existe de forma rudimentaria, desorganizada, reactiva, dependiente de iniciativas
     puntuales sin criterio corporativo formalizado.
   - **L2 — Reproducible**: se realiza de forma intuitiva y regular, pero no está documentada de forma
     exhaustiva; depende del conocimiento de los empleados.
   - **L3 — Proceso definido**: formalizada, documentada y aprobada por la dirección, con procedimiento
     claro y comunicado a todo el personal implicado.
   - **L4 — Gestionado y medible**: opera bajo supervisión constante, con KPIs/KRIs medidos con
     regularidad.
   - **L5 — Optimizado**: forma parte de un ciclo de mejora continua (p. ej. PDCA), evaluada
     proactivamente y ajustada mediante auditorías y lecciones aprendidas.

   Un requisito "sin responder" es un estado distinto de L0 (ver US3, criterio 4).
3. **Evidencia**: en esta primera versión, solo texto libre; no se admiten ficheros adjuntos.
4. **Plantilla del PDF**: no se usará ninguna plantilla de referencia en esta fase; el formato del
   informe queda a criterio de diseño de la app.
5. **Granularidad de "requisito" en el JSON**: el catálogo OSCAL contiene 467 nodos `item` anidados
   bajo las medidas, pero solo 386 corresponden a la unidad oficial de "requisito" del Anexo II del
   ENS. Los demás son sub-viñetas descriptivas dentro del texto (`prose`) de un requisito padre. La
   app MUST tratar como entidad `Requisito` independiente (con su propio estado de madurez y
   evidencia) únicamente los 386 nodos que correspondan a esa unidad oficial, clasificados por su
   atributo `name`/`class` real en el JSON; el resto se integra como parte del texto descriptivo del
   requisito al que pertenece, sin generar entidad ni pregunta propia al usuario.
6. **Refuerzos sin criterio de aplicabilidad propio**: 41 de los 133 refuerzos del catálogo no tienen
   su propia propiedad `aplicacion-por`. En ausencia de regla propia, la app MUST aplicar por defecto
   que ese refuerzo hereda la aplicabilidad (categoría y/o dimensión) de la Medida a la que pertenece.
   Esta regla se documenta como una asunción de diseño pendiente de validación normativa posterior
   (p. ej. frente al CCN-CERT), no como un hecho confirmado por el catálogo oficial; la app MUST dejar
   constancia de cuáles son esos 41 refuerzos concretos para facilitar su revisión futura.
7. **Alcance del cifrado en las exportaciones**: el cifrado en reposo (Principio I/VI de la
   Constitución) cubre los datos mientras residen en el almacenamiento interno de la app (SQLite +
   Android Keystore). Un informe PDF o CSV (US6, US7), una vez entregado a su destino final
   (almacenamiento compartido, envío, impresión...), MUST NOT asumirse cifrado por la app: queda
   fuera del perímetro de cifrado garantizado por ENSapp, y la protección de esa copia es
   responsabilidad del usuario/organización desde el momento de la exportación. Este alcance limitado
   MUST indicarse al usuario en el momento de exportar (p. ej. mediante un aviso en la UI).

## 7. Criterios de éxito (medibles, independientes de la implementación)

- Un usuario puede crear un nuevo Sistema de Información y completar su configuración inicial (US1)
  en menos de 2 minutos sin ayuda externa.
- Un usuario puede mantener al menos 2 Sistemas de Información simultáneamente sin que las respuestas
  o la aplicabilidad de uno afecten al otro.
- El 100% de los controles con `aplicacion-por: nivel-dimension` presentes en
  `ENS_Anexo_II_rev_10.json` se filtran correctamente al variar únicamente el nivel de una dimensión,
  sin tocar la categoría global (verificable con tests, Principio III).
- Un informe PDF o CSV exportado a mitad de evaluación puede generarse en menos de 5 segundos sin
  conexión a red.
- Ninguna pantalla depende exclusivamente del color para comunicar aplicabilidad (verificable con
  TalkBack activado).
