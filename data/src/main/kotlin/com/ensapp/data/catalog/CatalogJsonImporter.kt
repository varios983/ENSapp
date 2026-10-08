package com.ensapp.data.catalog

import com.ensapp.domain.catalog.ApplicabilityCriterion
import com.ensapp.domain.catalog.ApplicabilityRule
import com.ensapp.domain.catalog.Catalog
import com.ensapp.domain.catalog.CatalogControl
import com.ensapp.domain.catalog.CatalogParameter
import com.ensapp.domain.catalog.CatalogPart
import com.ensapp.domain.catalog.CatalogProperty
import com.ensapp.domain.catalog.CatalogRevision
import com.ensapp.domain.catalog.ControlKind
import com.ensapp.domain.catalog.DimensionLevel
import com.ensapp.domain.catalog.EnsCategory
import com.ensapp.domain.catalog.Family
import com.ensapp.domain.catalog.Framework
import com.ensapp.domain.catalog.SecurityDimension
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class CatalogImportException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

class CatalogJsonImporter {
    fun import(json: String): Catalog {
        if (json.isBlank()) throw CatalogImportException("El catálogo OSCAL está vacío")
        return try {
            parse(json)
        } catch (exception: CatalogImportException) {
            throw exception
        } catch (exception: Exception) {
            throw CatalogImportException("No se pudo validar el catálogo OSCAL", exception)
        }
    }

    private fun parse(source: String): Catalog {
        val root = Json.parseToJsonElement(source).asObject("raíz")
        val catalog = (root["catalog"] ?: root).asObject("catalog")
        val metadata = catalog["metadata"]?.asObject("metadata")
            ?: throw CatalogImportException("Falta metadata en el catálogo OSCAL")
        val groups = catalog.array("groups")
        if (groups.isEmpty()) throw CatalogImportException("El catálogo no contiene grupos")

        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val version = metadata.string("version")
        val revision = CatalogRevision(
            revisionId = version ?: checksum,
            catalogUuid = catalog.string("uuid")
                ?: throw CatalogImportException("Falta catalog.uuid"),
            version = version,
            oscalVersion = metadata.string("oscal-version"),
            lastModified = metadata.string("last-modified"),
            checksum = checksum,
        )

        val frameworks = mutableListOf<Framework>()
        val families = mutableListOf<Family>()
        val controls = mutableListOf<CatalogControl>()
        val parameters = mutableListOf<CatalogParameter>()
        val parts = mutableListOf<CatalogPart>()

        groups.forEachIndexed { rootOrder, rootElement ->
            val rootGroup = rootElement.asObject("grupo raíz")
            val frameworkId = rootGroup.requiredString("id", "grupo raíz")
            val frameworkProperties = rootGroup.properties("GROUP", frameworkId)
            frameworks += Framework(
                id = frameworkId,
                title = rootGroup.requiredString("title", frameworkId),
                label = frameworkProperties.firstValue("label"),
                sortOrder = rootOrder,
                properties = frameworkProperties,
            )
            rootGroup.parseParts(parts, ownerGroupId = frameworkId)
            rootGroup.parseControls(
                frameworkId = frameworkId,
                familyId = null,
                parentControlId = null,
                controls = controls,
                parameters = parameters,
                parts = parts,
            )
            rootGroup.array("groups").forEachIndexed { familyOrder, familyElement ->
                val familyGroup = familyElement.asObject("familia")
                val familyId = familyGroup.requiredString("id", "familia")
                val familyProperties = familyGroup.properties("GROUP", familyId)
                families += Family(
                    id = familyId,
                    frameworkId = frameworkId,
                    title = familyGroup.requiredString("title", familyId),
                    label = familyProperties.firstValue("label"),
                    sortOrder = familyOrder,
                    properties = familyProperties,
                )
                familyGroup.parseParts(parts, ownerGroupId = familyId)
                familyGroup.parseControls(
                    frameworkId = frameworkId,
                    familyId = familyId,
                    parentControlId = null,
                    controls = controls,
                    parameters = parameters,
                    parts = parts,
                )
                if (familyGroup.array("groups").isNotEmpty()) {
                    throw CatalogImportException(
                        "Estructura de grupos anidados no soportada bajo la familia $familyId",
                    )
                }
            }
        }

        val requirements = RequirementImportMapper().map(parts)
        return Catalog(revision, frameworks, families, controls, parameters, parts, requirements)
    }

    private fun JsonObject.parseControls(
        frameworkId: String,
        familyId: String?,
        parentControlId: String?,
        controls: MutableList<CatalogControl>,
        parameters: MutableList<CatalogParameter>,
        parts: MutableList<CatalogPart>,
    ) {
        array("controls").forEachIndexed { order, element ->
            val control = element.asObject("control")
            val id = control.requiredString("id", "control")
            val controlClass = control.string("class")
            val kind = if (controlClass == "ens-refuerzo") {
                ControlKind.REINFORCEMENT
            } else {
                ControlKind.MEASURE
            }
            val properties = control.properties("CONTROL", id)
            controls += CatalogControl(
                id = id,
                kind = kind,
                parentControlId = parentControlId,
                frameworkId = frameworkId,
                familyId = familyId,
                title = control.requiredString("title", id),
                controlClass = controlClass,
                label = properties.firstValue("label"),
                sortOrder = order,
                properties = properties,
                applicability = properties.toApplicabilityRule(id),
            )
            control.array("params").forEachIndexed { parameterOrder, parameterElement ->
                val parameter = parameterElement.asObject("parámetro de $id")
                val parameterId = parameter.requiredString("id", "parámetro de $id")
                val parameterProperties = parameter.properties("PARAMETER", parameterId)
                val select = parameter["select"]?.asObject("select de $parameterId")
                val scopeProperty = parameterProperties.firstOrNull {
                    it.namespace == ENS_NAMESPACE && it.name in setOf("categoria", "nivel")
                }
                parameters += CatalogParameter(
                    id = parameterId,
                    controlId = id,
                    label = parameter.string("label"),
                    usage = parameter.string("usage"),
                    type = parameterProperties.firstValue("tipo-param"),
                    scopeKind = scopeProperty?.name,
                    scopeValue = scopeProperty?.value,
                    selectionCardinality = select?.string("how-many"),
                    sortOrder = parameterOrder,
                    properties = parameterProperties,
                    choices = select?.array("choice")?.mapIndexed { choiceOrder, choice ->
                        choice.asString("choice[$choiceOrder] de $parameterId")
                    }.orEmpty(),
                )
            }
            control.parseParts(parts, ownerControlId = id)
            control.parseControls(
                frameworkId = frameworkId,
                familyId = familyId,
                parentControlId = id,
                controls = controls,
                parameters = parameters,
                parts = parts,
            )
        }
    }

    private fun JsonObject.parseParts(
        target: MutableList<CatalogPart>,
        ownerControlId: String? = null,
        ownerGroupId: String? = null,
        parentPartId: String? = null,
    ) {
        array("parts").forEachIndexed { order, element ->
            val sourcePart = element.asObject("part")
            val id = sourcePart.requiredString("id", "part")
            val properties = sourcePart.properties("PART", id)
            target += CatalogPart(
                id = id,
                ownerControlId = ownerControlId,
                ownerGroupId = ownerGroupId,
                parentPartId = parentPartId,
                name = sourcePart.requiredString("name", "part $id"),
                namespace = sourcePart.string("ns"),
                label = properties.firstValue("label"),
                prose = sourcePart.string("prose"),
                sortOrder = order,
                properties = properties,
            )
            sourcePart.parseParts(
                target,
                ownerControlId = ownerControlId,
                ownerGroupId = ownerGroupId,
                parentPartId = id,
            )
        }
    }

    private fun JsonObject.properties(ownerKind: String, ownerId: String): List<CatalogProperty> =
        array("props").mapIndexed { order, element ->
            val property = element.asObject("prop de $ownerId")
            CatalogProperty(
                ownerKind = ownerKind,
                ownerId = ownerId,
                namespace = property.string("ns"),
                name = property.requiredString("name", "prop de $ownerId"),
                value = property.requiredString("value", "prop de $ownerId"),
                sourceOrder = order,
            )
        }

    private fun List<CatalogProperty>.toApplicabilityRule(controlId: String): ApplicabilityRule? {
        val criterionValues = filter { it.namespace == ENS_NAMESPACE && it.name == "aplicacion-por" }
            .map { it.value }
            .distinct()
        if (criterionValues.isEmpty()) {
            return ApplicabilityRule(ApplicabilityCriterion.UNSPECIFIED)
        }
        if (criterionValues.size != 1) {
            throw CatalogImportException("Criterios aplicacion-por contradictorios en $controlId")
        }
        val criterion = when (criterionValues.single()) {
            "categoria" -> ApplicabilityCriterion.CATEGORY
            "nivel-dimension" -> ApplicabilityCriterion.DIMENSION_LEVEL
            else -> throw CatalogImportException(
                "Criterio aplicacion-por desconocido '${criterionValues.single()}' en $controlId",
            )
        }
        val ensValues = filter { it.namespace == ENS_NAMESPACE }
        return when (criterion) {
            ApplicabilityCriterion.CATEGORY -> ApplicabilityRule(
                criterion = criterion,
                categories = ensValues.filter { it.name == "categoria" }
                    .map { enumValue<EnsCategory>(it.value, controlId) }.toSet(),
            )
            ApplicabilityCriterion.DIMENSION_LEVEL -> ApplicabilityRule(
                criterion = criterion,
                dimensions = ensValues.filter { it.name == "dimension" }
                    .map { enumValue<SecurityDimension>(it.value, controlId) }.toSet(),
                levels = ensValues.filter { it.name == "nivel" }
                    .map { enumValue<DimensionLevel>(it.value, controlId) }.toSet(),
            )
            else -> error("Criterio ya validado")
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, controlId: String): T {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .uppercase(Locale.ROOT)
        return enumValues<T>().firstOrNull { it.name == normalized }
            ?: throw CatalogImportException("Valor ENS desconocido '$value' en $controlId")
    }

    private fun List<CatalogProperty>.firstValue(name: String): String? =
        firstOrNull { it.name == name }?.value

    private fun JsonObject.array(name: String): List<JsonElement> =
        when (val value = this[name]) {
            null, JsonNull -> emptyList()
            is JsonArray -> value
            else -> throw CatalogImportException("El campo '$name' debe ser un array")
        }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.requiredString(name: String, owner: String): String =
        string(name)?.takeIf { it.isNotBlank() }
            ?: throw CatalogImportException("Falta '$name' en $owner")

    private fun JsonElement.asObject(owner: String): JsonObject =
        this as? JsonObject ?: throw CatalogImportException("$owner debe ser un objeto")

    private fun JsonElement.asString(owner: String): String =
        (this as? JsonPrimitive)?.contentOrNull
            ?: throw CatalogImportException("$owner debe ser texto")

    private companion object {
        const val ENS_NAMESPACE = "urn:es:ens"
    }
}
