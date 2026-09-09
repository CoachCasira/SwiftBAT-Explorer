from pathlib import Path


def add_import(path, marker, insertion):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if insertion.strip() not in text:
        if marker not in text:
            raise RuntimeError(f'Marker missing in {path}: {marker}')
        text = text.replace(marker, marker + insertion, 1)
        p.write_text(text, encoding='utf-8')

add_import(
    'src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java',
    'import javafx.animation.PauseTransition;\n',
    'import javafx.application.Platform;\n')
add_import(
    'src/main/java/it/casiraghi/swiftbat/ui/UiFactory.java',
    'import javafx.scene.control.Tooltip;\n',
    'import javafx.scene.control.ToggleButton;\n')

# Remove this one-shot recovery workflow/script too; the main patch already
# removes its own temporary workflow and script.
for temporary in [
    Path('scripts/apply_professor_feedback_fix.py'),
    Path('.github/workflows/apply-professor-feedback-fix.yml'),
]:
    if temporary.exists():
        temporary.unlink()

print('Import fixes applied')
