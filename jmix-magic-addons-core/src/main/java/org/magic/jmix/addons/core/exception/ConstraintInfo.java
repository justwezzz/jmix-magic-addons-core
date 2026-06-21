package org.magic.jmix.addons.core.exception;

import io.jmix.core.metamodel.model.MetaClass;

import java.util.List;

public record ConstraintInfo(
        String constraintName,
        MetaClass metaClass,
        List<String> propertyNames
) {
}
