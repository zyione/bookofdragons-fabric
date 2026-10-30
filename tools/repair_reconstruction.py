"""Recorded, narrow Java recovery fixes; run once on the imported baseline."""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[1]

def change(relative, before, after):
    path = ROOT / 'fauna/src/main/java/net/magister/liberanimalium' / relative
    text = path.read_text(encoding='utf-8')
    if before not in text:
        raise ValueError(f'Missing expected source: {relative}: {before}')
    path.write_text(text.replace(before, after), encoding='utf-8')

for module in ('fauna', 'bookofdragons'):
    for path in (ROOT / module / 'src/main/java').rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        updated = text.replace('ResourceLocation.parse(', 'new ResourceLocation(').replace('ResourceLocation.withDefaultNamespace(', 'new ResourceLocation(')
        updated = re.sub(r'\b(\w+)\.getPersistentData\(\)', r'net.magister.liberanimalium.platform.fabric.EntityData.get(\1)', updated)
        if path.parent.name == 'mixin':
            updated = re.sub(r'\((\w+(?:<[^>]+>)?)\)this\b', r'(\1)(Object)this', updated)
        if text != updated:
            path.write_text(updated, encoding='utf-8')

change('attack/client/render/builtin/ModelBodyRenderer.java', 'AzEntityRendererConfig.builder(', 'AzEntityRendererConfig.<AttackProjectileEntity>builder(')
change('render/FaunaRendererAz.java', 'AzEntityRendererConfig.builder(', 'AzEntityRendererConfig.<T>builder(')
change('chassis/FaunaAnimator.java', 'AzKeyframeCallbacks.builder()', 'AzKeyframeCallbacks.<T>builder()')
change('body/registry/FaunaEntities.java', 'Builder.of(AttackProjectileEntity::new', 'Builder.<AttackProjectileEntity>of(AttackProjectileEntity::new')
change('body/nav/noise/FaunaNoiseListener.java', 'new Dynamic(NbtOps.INSTANCE,', 'new Dynamic<>(NbtOps.INSTANCE,')
change('render/vfx/SwimBubbles.java', 'ArrayList nostrils = new ArrayList(2);', 'ArrayList<Vec3> nostrils = new ArrayList<>(2);')
change('chassis/FaunaBase.java', '      super.onAddedToWorld();', '      // Vanilla Fabric has no superclass Forge lifecycle hook; Fabric entity-load bridge calls this method.')
change('chassis/FaunaBase.java', 'int var2 = false;', 'return; // Reference negative jump-power branch performs no takeoff.')
change('compat/v20/ClientFrameImpl.java', '.getPartialTick()', '.getFrameTime()')
change('compat/v20/EntitiesImpl.java', 'entity.getStepHeight()', 'entity.maxUpStep()')
change('platform/fabric/FabricNetwork.java', 'ResourceLocation.isValidPath(name)', 'name.matches("[a-z0-9/._-]+")')
change('platform/fabric/FabricNetwork.java', '!connection.isDisconnected()', 'connection.getConnection().isConnected()')

path = ROOT / 'fauna/src/main/java/net/magister/liberanimalium/fauna/debug/DriveAutopsy.java'
text = path.read_text(encoding='utf-8')
for case in ('enmity', 'rival', 'play'):
    pattern = rf'(case "{case}":)(.*?)(?=         case |         default:)'
    text = re.sub(pattern, lambda m: m[1]+' {'+m[2]+'         }\n', text, flags=re.S)
path.write_text(text, encoding='utf-8')
change('render/gpu/GpuSkin.java', 'case IMMEDIATE:\n', 'case IMMEDIATE: {\n')
change('render/gpu/GpuSkin.java', 'case FEEDBACK:\n', '}\n            case FEEDBACK: {\n')
change('render/gpu/GpuSkin.java', '            default:\n', '            }\n            default:\n')
