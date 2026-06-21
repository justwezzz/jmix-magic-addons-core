package org.magic.jmix.addons.core.tab.annotation;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.DefaultMainViewParent;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PresentationModeHelper {

    private PresentationModeHelper() {}

    public static Set<PresentationMode> getSupportedModes(Class<?> viewClass) {
        PresentationModes annotation = viewClass.getAnnotation(PresentationModes.class);
        if (annotation != null && annotation.value().length > 0) {
            return new LinkedHashSet<>(Arrays.asList(annotation.value()));
        }

        Route routeAnnotation = viewClass.getAnnotation(Route.class);
        if (routeAnnotation == null || routeAnnotation.layout() == com.vaadin.flow.router.RouterLayout.class) {
            return Collections.singleton(PresentationMode.PAGE);
        }
        return new LinkedHashSet<>(Arrays.asList(PresentationMode.TAB, PresentationMode.DIALOG));
    }

    public static boolean supports(Class<?> viewClass, PresentationMode mode) {
        return getSupportedModes(viewClass).contains(mode);
    }

    public static PresentationMode getDefaultMode(Class<?> viewClass) {
        return getSupportedModes(viewClass).iterator().next();
    }

    public static void validate(Class<?> viewClass, PresentationMode mode) {
        if (!supports(viewClass, mode)) {
            Set<PresentationMode> supported = getSupportedModes(viewClass);
            throw new IllegalStateException(
                    String.format("%s does not support %s mode, supported: %s",
                            viewClass.getSimpleName(), mode, supported));
        }
    }

    public static boolean isManagedByMainView(Class<?> viewClass) {
        Route routeAnnotation = viewClass.getAnnotation(Route.class);
        if (routeAnnotation == null) return false;
        Class<?> layout = routeAnnotation.layout();
        return DefaultMainViewParent.class.isAssignableFrom(layout);
    }
}
