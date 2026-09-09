from pathlib import Path

p = Path('src/main/java/it/casiraghi/swiftbat/ui/PopulationPage.java')
text = p.read_text(encoding='utf-8')
if 'import javafx.scene.control.ToggleGroup;' not in text:
    marker = 'import javafx.scene.control.ToggleButton;\n'
    if marker not in text:
        raise RuntimeError('ToggleButton import marker missing')
    text = text.replace(marker, marker + 'import javafx.scene.control.ToggleGroup;\n', 1)
    p.write_text(text, encoding='utf-8')

for temporary in [
    Path('scripts/apply_professor_feedback_2_fix.py'),
    Path('.github/workflows/apply-professor-feedback-2-fix.yml'),
]:
    if temporary.exists():
        temporary.unlink()

print('Second-pass import fix applied')
