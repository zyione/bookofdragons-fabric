from pathlib import Path
root = Path(__file__).resolve().parents[1]
count = 0
for module in ('fauna', 'bookofdragons'):
    for path in (root / module / 'src/main/java').rglob('*.java'):
        source = path.read_text(encoding='utf-8')
        updated = source.replace('javax.annotation.Nullable', 'org.jetbrains.annotations.Nullable')
        if source != updated:
            path.write_text(updated, encoding='utf-8')
            count += 1
print(f'Normalized Nullable imports in {count} files')
