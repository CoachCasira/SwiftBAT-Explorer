package it.casiraghi.swiftbat.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineGrbServiceTest {
    @Test
    void dictionaryContainsCoreFieldsAndNoBlankNames() {
        var definitions = OnlineGrbService.dictionary();
        assertFalse(definitions.isEmpty());
        assertTrue(definitions.stream().anyMatch(item -> item.field().equals("FRACEXP")));
        assertTrue(definitions.stream().anyMatch(item -> item.field().equals("TRIGTIME")));
        assertTrue(definitions.stream().noneMatch(item -> item.field().isBlank()));
        assertTrue(new HashSet<>(definitions.stream().map(item -> item.field().toUpperCase()).toList()).size() > 10);
    }
}
