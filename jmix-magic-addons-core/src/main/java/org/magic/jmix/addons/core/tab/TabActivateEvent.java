package org.magic.jmix.addons.core.tab;

import com.vaadin.flow.component.ComponentEvent;
import io.jmix.flowui.view.View;

/**
 * Fired when a tab becomes active (user switches to this tab).
 * Views must implement {@link TabActivationAware} to receive this event.
 *
 * <pre>{@code
 * @Subscribe
 * public void onTabActivate(TabActivateEvent event) {
 *     startPolling();
 * }
 * }</pre>
 */
public class TabActivateEvent extends ComponentEvent<View<?>> {

    public TabActivateEvent(View<?> source) {
        super(source, false);
    }
}
