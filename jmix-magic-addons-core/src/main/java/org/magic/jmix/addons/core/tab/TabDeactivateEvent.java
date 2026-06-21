package org.magic.jmix.addons.core.tab;

import com.vaadin.flow.component.ComponentEvent;
import io.jmix.flowui.view.View;

/**
 * Fired when a tab becomes inactive (user switches away from this tab).
 * Views must implement {@link TabActivationAware} to receive this event.
 *
 * <pre>{@code
 * @Subscribe
 * public void onTabDeactivate(TabDeactivateEvent event) {
 *     stopPolling();
 * }
 * }</pre>
 */
public class TabDeactivateEvent extends ComponentEvent<View<?>> {

    public TabDeactivateEvent(View<?> source) {
        super(source, false);
    }
}
