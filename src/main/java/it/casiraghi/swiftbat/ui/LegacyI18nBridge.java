package it.casiraghi.swiftbat.ui;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Compatibility bridge for legacy Swing/Java2D renderers that still call I18n.t directly.
 * New UI code uses UiTranslations; this bridge makes the supplemental dictionaries visible
 * to the historical I18n API as well, so every renderer follows the same language state.
 */
public final class LegacyI18nBridge {
    private LegacyI18nBridge() { }

    @SuppressWarnings("unchecked")
    public static void install() {
        try {
            Map<String, String> supplementalEn = map(UiTranslations.class, "EN");
            Map<String, String> supplementalIt = map(UiTranslations.class, "IT");
            Map<String, String> legacyEn = map(I18n.class, "EN");
            Map<String, String> legacyIt = map(I18n.class, "IT");

            supplementalEn.forEach(legacyEn::putIfAbsent);
            supplementalIt.forEach(legacyIt::putIfAbsent);
            UiTranslationExtras.english().forEach(legacyEn::putIfAbsent);
            UiTranslationExtras.italian().forEach(legacyIt::putIfAbsent);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to initialize the unified UI translation dictionary", error);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> map(Class<?> owner, String fieldName) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }
}
