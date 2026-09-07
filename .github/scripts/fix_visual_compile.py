from pathlib import Path
p = Path('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java')
text = p.read_text(encoding='utf-8')
old = 'private void commitExposureField(Slider slider, TextField field, boolean minimum) {'
new = 'private void commitExposureField(ScrollBar slider, TextField field, boolean minimum) {'
if old not in text:
    raise SystemExit('Firma commitExposureField non trovata')
p.write_text(text.replace(old, new, 1), encoding='utf-8')
