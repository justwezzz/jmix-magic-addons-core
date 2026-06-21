package org.magic.jmix.addons.core.view.base;

import com.vaadin.flow.component.ComponentEventListener;
import io.jmix.core.EntityStates;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.flowui.view.StandardDetailView;
import org.magic.jmix.addons.core.tab.TabActivationAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;

public abstract class BaseDetailView<T> extends StandardDetailView<T> implements TabActivationAware {

    private static final Logger log = LoggerFactory.getLogger(BaseDetailView.class);

    private static final String MSG_PREFIX = "org.magic.jmix.addons.core/";

    private boolean dataSaved = false;
    private boolean newEntity = false;

    @Autowired
    private EntityStates entityStates;
    @Autowired
    private Messages messages;
    @Autowired
    private Metadata metadata;
    @Autowired
    private io.jmix.core.MetadataTools metadataTools;

    public BaseDetailView() {
        addAfterSaveListener((ComponentEventListener<AfterSaveEvent>) event -> {
            dataSaved = true;
        });

        addBeforeSaveListener((ComponentEventListener<StandardDetailView.BeforeSaveEvent>) event -> {
            newEntity = entityStates.isNew(getEditedEntity());
        });

        addReadyListener((ComponentEventListener<ReadyEvent>) event -> {
            updateAutoTitle();
        });
    }

    protected void updateAutoTitle() {
        setAutoTitle();
    }

    protected boolean isAutoTitleEnabled() {
        return true;
    }

    protected void setAutoTitle() {
        if (!isAutoTitleEnabled()) {
            return;
        }

        String currentTitle = getPageTitle();
        if (currentTitle != null && !currentTitle.isEmpty()) {
            return;
        }

        T entity;
        try {
            entity = getEditedEntity();
        } catch (IllegalStateException e) {
            return;
        }

        if (entity == null) {
            return;
        }

        io.jmix.core.metamodel.model.MetaClass metaClass = metadata.getClass(entity);
        Class<?> entityClass = metaClass.getJavaClass();
        String entityName = messages.getMessage(entityClass.getPackageName(), entityClass.getSimpleName());

        String title;
        if (entityStates.isNew(entity)) {
            title = messages.getMessage(MSG_PREFIX + "detailView.title.new") + ": " + entityName;
        } else {
            String instanceName = metadataTools.getInstanceName(entity);
            if (instanceName == null || instanceName.isEmpty()) {
                instanceName = entityName;
            }

            if (isReadOnly()) {
                title = messages.getMessage(MSG_PREFIX + "detailView.title.view") + ": " + entityName + ": " + instanceName;
            } else {
                title = messages.getMessage(MSG_PREFIX + "detailView.title.edit") + ": " + entityName + ": " + instanceName;
            }
        }

        setPageTitle(title);
    }

    public boolean isDataSaved() {
        return dataSaved;
    }

    public boolean isNewEntity() {
        return newEntity;
    }

    @Nullable
    public T getSavedEntity() {
        if (dataSaved) {
            return getEditedEntity();
        }
        return null;
    }

    public boolean isSavedNewEntity() {
        return dataSaved && newEntity;
    }
}
