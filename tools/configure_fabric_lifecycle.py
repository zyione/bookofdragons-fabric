"""Move client setup out of common initialization and replace Forge mixin config."""
from pathlib import Path
import json
root = Path(__file__).resolve().parents[1]
for module, main_class in [('fauna', 'LiberAnimalium'), ('bookofdragons', 'BookOfDragons')]:
    package = 'liberanimalium' if module == 'fauna' else module
    path = root / module / f'src/main/java/net/magister/{package}/{main_class}.java'
    text = path.read_text(encoding='utf-8')
    start = text.index('      if (Services.platform().isClient()) {')
    end = start + len('      if (Services.platform().isClient()) {')
    depth = 1
    while depth:
        if text[end] == '{': depth += 1
        elif text[end] == '}': depth -= 1
        end += 1
    body = text[start + len('      if (Services.platform().isClient()) {'):end-1]
    if module == 'fauna':
        body = '      boolean dev = !Services.platform().isProduction();\n' + body
    text = text[:start] + text[end:]
    insert = text.index('   public static void setup()')
    text = text[:insert] + '   public static void clientInit() {' + body + '\n   }\n\n' + text[insert:]
    path.write_text(text, encoding='utf-8')

resources = root / 'fauna/src/main/resources'
original = json.loads((resources / 'liberanimalium.forge.mixins.json').read_text(encoding='utf-8'))
original.pop('refmap', None)
original['mixins'] += ['PersistentEntityDataMixin', 'ServerPartLookupMixin', 'CommonEntityEventsMixin',
    'CommonLivingEventsMixin', 'TamingEventsMixin', 'AdvancementEventsMixin', 'ServerPackSourcesMixin',
    'BlockPlacedEventsMixin', 'BlockBrokenEventsMixin', 'ExplosionEventsMixin']
original['client'] += ['BulkVertexUploadMixin', 'ArmPoseAccessor', 'CustomArmPoseMixin', 'KeyMappingAccessor',
    'ClientPartLookupMixin', 'ClientInputEventsMixin', 'KeyboardEventsMixin', 'MouseEventsMixin', 'HudEventsMixin',
    'RenderStageEventsMixin', 'HandEventsMixin', 'ItemArmPoseMixin', 'ClientWorldUnloadMixin', 'ClientPackSourcesMixin']
(resources / 'liberanimalium.fabric.mixins.json').write_text(json.dumps(original, indent=2)+'\n', encoding='utf-8')
# These are Forge-generated mapping artifacts, not gameplay assets.
(resources / 'liberanimalium.forge.mixins.json').unlink()
(resources / 'liberanimalium.mixins.refmap.json').unlink()
path = resources / 'fabric.mod.json'
metadata = json.loads(path.read_text(encoding='utf-8'))
metadata['entrypoints']['client'] = ['net.magister.liberanimalium.platform.fabric.FabricClientBridge']
metadata['mixins'] = ['liberanimalium.fabric.mixins.json']
path.write_text(json.dumps(metadata, indent=2)+'\n', encoding='utf-8')
path = root / 'bookofdragons/src/main/resources/fabric.mod.json'
metadata = json.loads(path.read_text(encoding='utf-8'))
metadata['entrypoints']['liberanimalium:client'] = ['net.magister.bookofdragons.platform.fabric.BookOfDragonsClient']
path.write_text(json.dumps(metadata, indent=2)+'\n', encoding='utf-8')
path = root / 'bookofdragons/src/main/resources/bookofdragons.fabric.mixins.json'
metadata = json.loads(path.read_text(encoding='utf-8'))
metadata['mixins'] += ['ParalysisLivingMixin', 'ParalysisSpawnMixin']
metadata['client'] = ['ParalysisInputMixin']
path.write_text(json.dumps(metadata, indent=2)+'\n', encoding='utf-8')
