package org.magic.jmix.addons.core.exception;

import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("magic_ConstraintMetadataScanner")
public class ConstraintMetadataScanner implements ApplicationListener<ContextRefreshedEvent> {

    private boolean initialized = false;

    private static final Logger log = LoggerFactory.getLogger(ConstraintMetadataScanner.class);

    protected final Metadata metadata;
    protected final MetadataTools metadataTools;
    protected final Map<String, ConstraintInfo> constraintCache = new HashMap<>();

    public ConstraintMetadataScanner(Metadata metadata, MetadataTools metadataTools) {
        this.metadata = metadata;
        this.metadataTools = metadataTools;
    }

    public void scan() {
        constraintCache.clear();
        for (MetaClass metaClass : metadata.getClasses()) {
            Class<?> javaClass = metaClass.getJavaClass();
            Table tableAnnotation = javaClass.getAnnotation(Table.class);
            if (tableAnnotation == null) {
                continue;
            }

            scanIndexes(metaClass, tableAnnotation);
            scanUniqueConstraints(metaClass, tableAnnotation);
        }
        log.debug("Scanned {} unique constraint entries", constraintCache.size());
    }

    protected void scanIndexes(MetaClass metaClass, Table tableAnnotation) {
        for (jakarta.persistence.Index index : tableAnnotation.indexes()) {
            if (!index.unique()) {
                continue;
            }
            String constraintName = index.name().toUpperCase();
            List<String> propertyNames = resolveColumnNames(metaClass, index.columnList());
            if (!propertyNames.isEmpty()) {
                constraintCache.put(constraintName, new ConstraintInfo(constraintName, metaClass, propertyNames));
            }
        }
    }

    protected void scanUniqueConstraints(MetaClass metaClass, Table tableAnnotation) {
        for (UniqueConstraint uc : tableAnnotation.uniqueConstraints()) {
            String constraintName = uc.name().toUpperCase();
            List<String> propertyNames = resolveColumnNames(metaClass, String.join(",", uc.columnNames()));
            if (!propertyNames.isEmpty()) {
                constraintCache.put(constraintName, new ConstraintInfo(constraintName, metaClass, propertyNames));
            }
        }
    }

    protected List<String> resolveColumnNames(MetaClass metaClass, String columnList) {
        String[] columns = columnList.split(",");
        List<String> propertyNames = new ArrayList<>();
        for (String column : columns) {
            String trimmedColumn = column.trim().toUpperCase();
            String propertyName = findPropertyNameByColumn(metaClass, trimmedColumn);
            if (propertyName != null) {
                propertyNames.add(propertyName);
            }
        }
        return propertyNames;
    }

    protected String findPropertyNameByColumn(MetaClass metaClass, String columnName) {
        for (MetaProperty property : metaClass.getProperties()) {
            String dbColumn = metadataTools.getDatabaseColumn(property);
            if (dbColumn != null && dbColumn.toUpperCase().equals(columnName)) {
                return property.getName();
            }
        }
        return null;
    }

    public Optional<ConstraintInfo> findConstraint(String constraintName) {
        return Optional.ofNullable(constraintCache.get(constraintName.toUpperCase()));
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!initialized) {
            scan();
            initialized = true;
        }
    }
}
