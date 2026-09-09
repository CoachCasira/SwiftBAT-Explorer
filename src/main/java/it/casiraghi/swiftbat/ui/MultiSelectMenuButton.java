package it.casiraghi.swiftbat.ui;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Dropdown multi-selezione con checkbox; se tutte le opzioni sono attive equivale a "Tutti". */
public final class MultiSelectMenuButton extends MenuButton {
    private final String allLabel;
    private final List<String> values;
    private final List<CheckMenuItem> items = new ArrayList<>();
    private Runnable changeListener = () -> {};
    private boolean internal;

    public MultiSelectMenuButton(String allLabel, List<String> values) {
        this.allLabel = allLabel;
        this.values = List.copyOf(values);
        getStyleClass().add("choice-box-modern");
        setMaxWidth(Double.MAX_VALUE);
        for (String value : values) {
            CheckMenuItem item = new CheckMenuItem(I18n.t(value));
            item.setSelected(true);
            item.setOnAction(event -> {
                if (!internal) {
                    refreshText();
                    changeListener.run();
                }
            });
            items.add(item);
            getItems().add(item);
        }
        I18n.languageProperty().addListener((obs, oldValue, newValue) -> refreshLanguage());
        refreshText();
    }

    public void setOnSelectionChanged(Runnable listener) {
        changeListener = listener == null ? () -> {} : listener;
    }

    public Set<String> selectedValues() {
        Set<String> selected = new LinkedHashSet<>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isSelected()) selected.add(values.get(i));
        }
        return Collections.unmodifiableSet(selected);
    }

    public boolean isAllSelected() {
        return items.stream().allMatch(CheckMenuItem::isSelected);
    }

    public void selectAll() {
        internal = true;
        items.forEach(item -> item.setSelected(true));
        internal = false;
        refreshText();
        changeListener.run();
    }

    private void refreshLanguage() {
        for (int i = 0; i < items.size(); i++) items.get(i).setText(I18n.t(values.get(i)));
        refreshText();
    }

    private void refreshText() {
        int count = (int) items.stream().filter(CheckMenuItem::isSelected).count();
        if (count == items.size()) {
            setText(I18n.t(allLabel));
        } else if (count == 0) {
            setText(I18n.language() == I18n.Language.IT ? "Nessuno" : "None");
        } else if (count == 1) {
            for (int i = 0; i < items.size(); i++) if (items.get(i).isSelected()) setText(I18n.t(values.get(i)));
        } else {
            setText(count + (I18n.language() == I18n.Language.IT ? " selezionati" : " selected"));
        }
    }
}
