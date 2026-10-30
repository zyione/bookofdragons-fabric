from pathlib import Path
import zipfile
root = Path(__file__).resolve().parents[1]
source = next(p for p in (Path('\\\\?\\' + str(root)) / '.gradle/loom-cache/minecraftMaven').rglob('*.jar') if p.exists())
names = ['net/minecraft/client/gui/Gui', 'net/minecraft/client/renderer/LevelRenderer',
         'net/minecraft/client/renderer/GameRenderer', 'net/minecraft/client/renderer/ItemInHandRenderer',
         'net/minecraft/client/Minecraft', 'net/minecraft/world/entity/LivingEntity',
         'net/minecraft/server/packs/repository/PackRepository']
output = root / '.reference/game-input/selected.jar'
output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(source) as archive, zipfile.ZipFile(output, 'w') as target:
    for name in archive.namelist():
        if any(name == path + '.class' or name.startswith(path + '$') for path in names):
            target.writestr(name, archive.read(name))
