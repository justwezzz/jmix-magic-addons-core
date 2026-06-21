package org.magic.jmix.addons.core.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.querycondition.PropertyCondition;
import io.jmix.flowui.action.ObservableBaseAction;
import io.jmix.flowui.component.genericfilter.Configuration;
import io.jmix.flowui.component.genericfilter.GenericFilter;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 增强版过滤器组件，提供以下增强功能：
 * <ul>
 *   <li>添加清空按钮：一键清除所有过滤条件</li>
 *   <li>条件深拷贝：解决过滤条件被意外修改的问题</li>
 *   <li>修复 autoApply：apply() 方法仅在 autoApply=true 时执行</li>
 *   <li>修复摘要文本：单配置时显示空配置名称</li>
 *   <li>分页重置：应用过滤条件时重置 firstResult 为 0</li>
 * </ul>
 */
public class MagicGenericFilter extends GenericFilter {

    private static final Logger log = LoggerFactory.getLogger(MagicGenericFilter.class);

    private static final String CLEAR_BUTTON_TITLE_KEY = "org.magic.jmix.addons.core/genericFilter.clear";

    @Override
    protected void initControlsLayout(HorizontalLayout controlsLayout) {
        super.initControlsLayout(controlsLayout);

        JmixButton clearButton = new JmixButton();
        clearButton.setIcon(VaadinIcon.ERASER.create());
        clearButton.setTitle(messages.getMessage(CLEAR_BUTTON_TITLE_KEY));
        clearButton.addClickListener(e -> {
            Action clearValuesAction = getAction("genericFilter_clearValues");
            if (clearValuesAction != null) {
                clearValuesAction.actionPerform(this);
            }
        });
        controlsLayout.addComponentAtIndex(1, clearButton);
    }

    @Override
    protected void updateDataLoaderCondition() {
        super.updateDataLoaderCondition();
        DataLoader loader = getDataLoader();
        if (loader != null) {
            Condition live = loader.getCondition();
            loader.setCondition(deepClone(live));
        }
    }

    @Override
    protected void onApplyButtonClick(ClickEvent<MenuItem> clickEvent) {
        updateDataLoaderCondition();
        DataLoader loader = getDataLoader();
        if (loader instanceof CollectionLoader<?> cl) {
            cl.setFirstResult(0);
        }
        loader.load();
    }

    @Override
    protected Action createConfigurationAction(Configuration configuration) {
        return new ObservableBaseAction<>("genericFilter_select_" + configuration.getId())
                .withText(getConfigurationName(configuration))
                .withHandler(actionPerformedEvent -> {
                    setCurrentConfigurationInternal(configuration, true);
                    if (isAutoApply()) {
                        apply();
                    }
                });
    }

    @Override
    public void apply() {
        if (isAutoApply()) {
            super.apply();
        }
    }

    @Override
    public void loadConfigurationsAndApplyDefault() {
        super.loadConfigurationsAndApplyDefault();
        updateRootLayoutSummaryText();
    }

    @Override
    protected void updateRootLayoutSummaryText() {
        if (summaryText != null) {
            return;
        }

        if (getConfigurations().size() <= 1) {
            setSummaryTextInternal(getConfigurationName(getEmptyConfiguration()), false);
            return;
        }

        super.updateRootLayoutSummaryText();
    }

    private Condition deepClone(Condition condition) {
        if (condition == null) return null;
        if (condition instanceof LogicalCondition lc) {
            LogicalCondition clone = new LogicalCondition(lc.getType());
            for (Condition child : lc.getConditions()) {
                Condition clonedChild = deepClone(child);
                if (clonedChild != null) {
                    clone.add(clonedChild);
                }
            }
            return clone.getConditions().isEmpty() ? null : clone;
        }
        if (condition instanceof PropertyCondition pc) {
            if (pc.getParameterValue() == null) return null;
            PropertyCondition clone = PropertyCondition.create(
                    pc.getProperty(), pc.getOperation(), pc.getParameterValue());
            clone.skipNullOrEmpty();
            return clone;
        }
        return condition;
    }
}