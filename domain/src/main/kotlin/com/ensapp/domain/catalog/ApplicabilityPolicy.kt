package com.ensapp.domain.catalog

/**
 * ASUNCION DE DISENO pendiente de validacion normativa posterior: un refuerzo sin regla propia
 * hereda el criterio de categoria y/o dimension de su Medida padre. El catalogo OSCAL no afirma
 * esta regla.
 */
internal const val APLICABILIDAD_HEREDADA_DE_MEDIDA: Boolean = true

internal object ApplicabilityPolicy {
    fun shouldInherit(control: CatalogControl, rule: ApplicabilityRule?): Boolean =
        APLICABILIDAD_HEREDADA_DE_MEDIDA &&
            control.kind == ControlKind.REINFORCEMENT &&
            (rule == null || rule.criterion == ApplicabilityCriterion.UNSPECIFIED)
}
