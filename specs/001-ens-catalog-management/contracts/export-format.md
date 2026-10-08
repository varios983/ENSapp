# Contrato de exportación PDF/CSV

Las exportaciones son generadas localmente, bajo demanda y sin red. Reflejan el estado del sistema
seleccionado en el instante de exportación. Este contrato define la forma interoperable requerida por
US6/US7; el archivo se escribe mediante el mecanismo de creación/selección de documento de Android.

## CSV

- Codificación UTF-8; formato CSV compatible con RFC 4180: campos con comas, comillas o saltos de
  línea entrecomillados y comillas interiores duplicadas.
- Cabecera estable, una fila por cada uno de los 386 nodos `Requisito` identificados mediante
  `name == "item"` y padre inmediato `name == "requisitos"`, incluidos los que no aplican, con
  `aplicabilidad=NO_APLICA` y madurez vacía. Los 76 subitems y 5 items descriptivos se conservan como
  texto jerárquico, pero no generan filas de respuesta independientes. No filtrar silenciosamente
  los no aplicables.
- Columnas mínimas:

| Columna | Contenido |
|---|---|
| `sistema_id` | UUID del Sistema de Información exportado |
| `sistema_nombre` | Nombre del sistema |
| `catalog_revision` | Revisión/versión OSCAL utilizada |
| `marco_id`, `marco_nombre` | Marco propietario |
| `familia_id`, `familia_nombre` | Familia; vacíos si el control cuelga directamente del marco |
| `medida_id`, `medida_titulo` | Medida propietaria |
| `refuerzo_id`, `refuerzo_titulo` | Refuerzo propietario o vacío |
| `requisito_id`, `requisito_label`, `requisito_texto` | ID OSCAL de part, etiqueta normativa y prose |
| `aplicabilidad` | `APLICA`, `NO_APLICA` o `REVISAR`; se añade texto de motivo/dimensión si aplica |
| `madurez` | Vacío si no existe respuesta; `L0`–`L5` para una respuesta. Vacío **no** significa L0 |
| `nota` | Evidencia en texto libre o vacío |
| `actualizado_en` | Fecha/hora ISO-8601 de la respuesta, o vacío |

- El orden de columnas y el delimitador coma son estables; cada fila conserva el orden del catálogo.
- No escribir respuestas de un sistema distinto al seleccionado. Incluir el estado de aplicabilidad
  para distinguir no aplicables de respondibles. Una regla pendiente/no resuelta se marca `REVISAR`,
  no como `APLICA` ni `NO_APLICA`.

## PDF

- Generado con `android.graphics.pdf.PdfDocument`, sin librerías de terceros.
- Debe contener configuración del sistema (categoría y los cinco niveles CIDAT), resumen del estado y
  detalle jerárquico de medidas/refuerzos/requisitos, respuesta y nota.
- Debe ser legible con evaluaciones parciales y distinguir explícitamente “sin responder” de L0.
- El diseño puede definir paginación/estilos, pero no omitir elementos requeridos ni alterar sus IDs.

## Seguridad y límites

**Limitación aceptada**: ENSapp no cifra el PDF/CSV una vez que se ha escrito en el destino final,
fuera del almacenamiento interno cifrado de SQLite/Keystore de la app. Desde el momento de la
exportación, la responsabilidad de proteger esa copia corresponde al usuario/organización, que debe
elegir y custodiar un destino adecuado.

Esta limitación solo afecta a la copia exportada fuera del perímetro de la app. El cifrado en reposo
de la información sensible que permanece dentro de ENSapp y la persistencia SQLite (Principios I/VI)
se mantienen sin cambios. La app debe solicitar explícitamente un destino, evitar copias temporales
innecesarias en claro y mostrar la advertencia de responsabilidad antes de exportar; no debe prometer
que el proveedor de destino cifra el documento.
