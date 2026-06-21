package org.magic.jmix.addons.core.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import io.jmix.flowui.component.pagination.SimplePagination;
import io.jmix.flowui.model.CollectionChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 增强版分页组件，修复 Jmix SimplePagination 的 firstResult 被意外重置问题。
 * <p>
 * 问题场景：当 Grid 数据刷新（如过滤、排序）时，标准 SimplePagination 会重置
 * loader.firstResult 为 0，导致用户翻页状态丢失。
 * <p>
 * 解决方案：在 onRefreshItems 中记录 committedFirstResult，在翻页操作时恢复。
 */
public class MagicSimplePagination extends SimplePagination {

    private static final Logger log = LoggerFactory.getLogger(MagicSimplePagination.class);

    private int committedFirstResult = 0;

    @Override
    protected void onRefreshItems(CollectionChangeType changeType) {
        if (changeType == CollectionChangeType.REFRESH) {
            committedFirstResult = loader.getFirstResult();
        } else {
            restoreFirstResultIfNeeded();
        }
        super.onRefreshItems(changeType);
    }

    @Override
    protected void onPreviousClick(ClickEvent<Button> event) {
        restoreFirstResultIfNeeded();
        super.onPreviousClick(event);
    }

    @Override
    protected void onNextClick(ClickEvent<Button> event) {
        restoreFirstResultIfNeeded();
        super.onNextClick(event);
    }

    private void restoreFirstResultIfNeeded() {
        int current = loader.getFirstResult();
        if (current != committedFirstResult) {
            log.debug("MagicSimplePagination: restoring firstResult {} -> {}",
                    current, committedFirstResult);
            loader.setFirstResult(committedFirstResult);
        }
    }
}