from pathlib import Path

p = Path('src/main/java/it/casiraghi/swiftbat/ui/SkyMapPage.java')
text = p.read_text(encoding='utf-8')
old = '''        Label scientificNote = UiFactory.wrappedLabel(
                "La soglia a 2 s è mostrata soltanto come riferimento descrittivo tradizionale. La mappa non assegna da sola una classificazione scientifica definitiva.

"
                        + "Seleziona un punto per leggere coordinate, T90, classe descrittiva e redshift. Le viste Mollweide 2D e Sfera 3D rappresentano lo stesso campione: cambia soltanto il modo in cui la distribuzione celeste viene esplorata.",
                "sky-science-note");'''
new = '''        Label scientificNote = UiFactory.wrappedLabel(
                "La soglia a 2 s è mostrata soltanto come riferimento descrittivo tradizionale. La mappa non assegna da sola una classificazione scientifica definitiva. "
                        + "Seleziona un punto per leggere coordinate, T90, classe descrittiva e redshift. Le viste Mollweide 2D e Sfera 3D rappresentano lo stesso campione: cambia soltanto il modo in cui la distribuzione celeste viene esplorata.",
                "sky-science-note");'''
if old not in text:
    raise SystemExit('Testo nota scientifica da correggere non trovato')
p.write_text(text.replace(old, new, 1), encoding='utf-8')
print('Nota scientifica corretta.')
