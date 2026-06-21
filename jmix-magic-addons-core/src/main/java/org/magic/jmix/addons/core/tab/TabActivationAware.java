package org.magic.jmix.addons.core.tab;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.shared.Registration;

/**
 * Interface for views that need to receive tab activation/deactivation events.
 * Provides {@code addTabActivateListener} and {@code addTabDeactivateListener}
 * methods that enable {@code @Subscribe} annotation support.
 *
 * <pre>{@code
 * public class MyView extends StandardView implements TabActivationAware {
 *
 *     @Subscribe
 *     public void onTabActivate(TabActivateEvent event) {
 *         // tab activated
 *     }
 *
 *     @Subscribe
 *     public void onTabDeactivate(TabDeactivateEvent event) {
 *         // tab deactivated
 *     }
 * }
 * }</pre>
 */
public interface TabActivationAware {

    default Registration addTabActivateListener(ComponentEventListener<TabActivateEvent> listener) {
        return ComponentUtil.addListener((Component) this, TabActivateEvent.class, listener);
    }

    default Registration addTabDeactivateListener(ComponentEventListener<TabDeactivateEvent> listener) {
        return ComponentUtil.addListener((Component) this, TabDeactivateEvent.class, listener);
    }
}
