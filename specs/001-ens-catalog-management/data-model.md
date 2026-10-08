# Modelo de datos: ENSapp

La única fuente persistente de información de dominio es la base Room/SQLite. Las tablas de catálogo
reflejan el asset OSCAL importado; las columnas de usuario sensibles se cifran antes de almacenarse.
Las cardinalidades/campos marcados como pendientes no se resuelven por inferencia.

## Entidades

| Entidad/tabla | Campos principales | Relaciones y validación |
|---|---|---|
| `CatalogRevision` | `revision_id`, `catalog_uuid`, `version`, `oscal_version`, `last_modified`, `checksum`, `imported_at`, `active` | Una revisión activa. Registrar revisión y datos importados dentro de una transacción; no cambiar `schema_version` de Room al cambiar el catálogo. |
| `Framework` (`Marco`) | `id`, `revision_id`, `title`, `label`, `sort_order` | 3 marcos raíz `org`, `op`, `mp`. |
| `Family` (`Familia`) | `id`, `revision_id`, `framework_id`, `title`, `label`, `sort_order` | 15 grupos OSCAL descendientes, FK a `Framework`. No sintetizar familia para `org`. |
| `CatalogControl` | `id`, `revision_id`, `kind` (`MEASURE`/`REINFORCEMENT`), `parent_control_id?`, `framework_id`, `family_id?`, `title`, `class`, `label`, `sort_order` | 206 controles; 73 medidas y 133 refuerzos. El padre recursivo conserva `controls[]` anidados. Una medida raíz tiene el framework/familia de su grupo OSCAL; un refuerzo sigue al control padre. IDs fuente estables dentro de la revisión. |
| `ApplicabilityRule` | `control_id`, `criterion` (`CATEGORY`, `DIMENSION_LEVEL`, `INHERITED_FROM_MEASURE`, `UNSPECIFIED`), `source_properties` | `INHERITED_FROM_MEASURE` se usa para los 41 refuerzos sin regla propia; `UNSPECIFIED` conserva la ausencia en los demás controles, no como “aplica siempre”. |
| `CatalogProperty` | `owner_kind`, `owner_id`, `ns?`, `name`, `value`, `source_order` | Conserva todas las props de grupos/controles/params/parts, incluidos namespaces y nombres repetidos. Las tablas de aplicabilidad son proyecciones de estas propiedades, no sustitutos. |
| `ApplicabilityValue` | `control_id`, `value_kind` (`CATEGORY`, `DIMENSION`, `LEVEL`), `value`, `source_order` | Lista ordenada separada para cada tipo; no crea parejas dimensión-nivel que no estén expresadas en OSCAL. El nivel del sistema se compara contra los valores declarados para cada dimensión aplicable. |
| `CatalogPart` | `id`, `revision_id`, `control_id?`, `group_id?`, `parent_part_id?`, `name`, `ns?`, `label?`, `prose`, `sort_order` | Árbol completo de `parts[]`, incluidos `overview`, `requisitos`, `item` y `aclaracion`; un propietario es control o grupo. Preservar saltos de línea y nesting. Renderizar los subitems de cada requisito como parte de su contenido/prose, sin permitirles respuesta independiente. |
| `Requirement` (`Requisito`) | Proyección/identidad de `CatalogPart` que satisface `name == "item"` y tiene como padre inmediato un `CatalogPart` `name == "requisitos"` | Exactamente 386 nodos respondibles. 76 items con padre item son subviñetas/texto del requisito padre; 5 con padre overview son descriptivos. Los 467 nodos `item` se conservan íntegros en `CatalogPart`; 23 items padre tienen subpartes. Ningún item contiene atributo `class`, por lo que no se usa en la clasificación. |
| `CatalogParameter` / `DisjunctiveSelectionGroup` | `parameter_id`, `control_id`, `label`, `usage`, `type_param`, `scope_kind`, `scope_value`, `selection_cardinality`, `sort_order` | Conserva params OSCAL; la proyección disyuntiva aplica a `tipo-param=seleccion-refuerzo`, con cardinalidad `"one"`. 10 grupos observados en cuatro medidas. |
| `DisjunctiveChoice` | `parameter_id`, `reinforcement_id`, `sort_order` | Una fila por ID de `select.choice[]`; validar referencia a un refuerzo existente y pertenencia al parámetro. |
| `InformationSystem` (`SistemaInformación`) | `id` UUID, `name_ciphertext`, `category_ciphertext`, `created_at`, `updated_at` | Varios sistemas independientes. UUID aleatorio como clave; los campos del usuario se guardan cifrados. |
| `SystemDimension` | `system_id`, `dimension`, `level_ciphertext` | PK compuesta (`system_id`, `dimension`); las cinco dimensiones CIDAT, cada una con nivel BAJO/MEDIO/ALTO. Las cinco filas son obligatorias al completar configuración. |
| `RequirementResponse` (`RespuestaRequisito`) | `system_id`, `requirement_id`, `maturity_ciphertext`, `note_ciphertext?`, `updated_at` | Una respuesta por `(system_id, requirement_id)`. Ausencia de fila = “sin responder”; `maturity = L0` es una respuesta explícita cifrada. Solo FK a nodos validados como respondibles y aplicables. |
| `SystemDisjunctiveSelection` | `system_id`, `parameter_id`, `selected_reinforcement_ciphertext`, `updated_at` | `UNIQUE(system_id, parameter_id)` garantiza como máximo una alternativa activa por grupo. Tras descifrar, el dominio valida que el ID aparece en `DisjunctiveChoice`; no se almacena un FK en claro que revele la evaluación. Permite cambiar mediante transacción. |

`InformeExportado` no necesita tabla: la spec permite que sea efímero y los informes reflejan una
instantánea en el momento de generación.

## Reglas de dominio

- Categoría ENS: `BASICA`, `MEDIA`, `ALTA`. Dimensión CIDAT: `CONFIDENCIALIDAD`, `INTEGRIDAD`,
  `DISPONIBILIDAD`, `AUTENTICIDAD`, `TRAZABILIDAD`. Nivel dimensional: `BAJO`, `MEDIO`, `ALTO`.
- Madurez: `L0`–`L5`. “Sin responder” se representa exclusivamente por ausencia de
  `RequirementResponse`, nunca con un nivel adicional o con L0.
- Cada respuesta y selección tiene `system_id`; cambiar de sistema no comparte ni mezcla estado.
- Solo los criterios `categoria` y `nivel-dimension` se reconocen como `aplicacion-por`. Cualquier
  valor futuro desconocido falla la validación de importación de forma explícita hasta actualizar el
  mapeador.
- Un nivel dimensional aplica cuando el nivel configurado de esa dimensión está incluido entre los
  valores `nivel` publicados para el control, que se conserva junto a la lista `dimension`; no se
  sintetizan pares que OSCAL no codifica.
- Una propiedad ausente/incompleta se conserva y comunica al dominio. Los controles afectados no se
  vuelven respondibles por un valor predeterminado silencioso.
- Solo los 386 requisitos estructurales admiten respuesta independiente. Los subitems y los items de
  overview permanecen como contenido del árbol fuente, no reciben fila `RequirementResponse`.
- Si un refuerzo no tiene criterio propio `aplicacion-por`, se evalúa heredando la aplicabilidad de
  su Medida padre. Esto es la asunción de diseño `APLICABILIDAD_HEREDADA_DE_MEDIDA`, pendiente de
  validación normativa posterior; no es una regla expresada por el catálogo.
- Contenido no sensible del catálogo permanece legible en SQLite. Cifrar campos de nombre,
  configuración, madurez, nota y elección mediante AES-GCM; nonce y versión del formato se almacenan
  junto al ciphertext. No incluir plaintext en logs ni copias Auto Backup sin estrategia de clave.

## Relaciones principales

```text
CatalogRevision 1 ── * Framework 1 ── * Family 0..1 ── * CatalogControl (MEASURE)
                                                   CatalogControl (MEASURE)
                                                             1 ── * CatalogControl (REINFORCEMENT)
CatalogControl 1 ── * CatalogPart (árbol parts recursivo)
CatalogControl 0..1 ── 1 ApplicabilityRule ── * categoría / dimensión-nivel
CatalogControl (MEASURE) 1 ── * DisjunctiveSelectionGroup 1 ── * DisjunctiveChoice
InformationSystem 1 ── 5 SystemDimension
InformationSystem 1 ── * RequirementResponse * ── 1 Requirement
InformationSystem 1 ── * SystemDisjunctiveSelection * ── 1 DisjunctiveSelectionGroup
```

## Cifrado y ciclo de vida

Keystore genera/protege la clave AES-GCM no exportable. El data layer cifra valores antes de
entregarlos a Room y descifra al construir el modelo de dominio; valida autenticidad y versión en
cada lectura. Si la clave no está disponible o un tag no valida, notifica error recuperable por UI;
no elimina ni reinicializa silenciosamente la DB. H-03 cubre los informes que abandonan SQLite.

Los borrados de sistemas deben respetar las FKs y eliminar solo sus dimensiones, respuestas y
selecciones, no los datos comunes del catálogo. Las actualizaciones de catálogo nunca borran
respuestas; los IDs normativos desaparecidos requieren revisión/mapeo para preservar historia.
