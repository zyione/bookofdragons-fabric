from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'bookofdragons/src/main/java/net/magister/bookofdragons'
replacements = {
    'client/gui/book/data/RiderManual.java': [('mapping.getKey()', '((net.magister.liberanimalium.mixin.KeyMappingAccessor)mapping).liberanimalium$key()')],
    'client/gui/config/ConfigList.java': [('(Enum)value.get()', 'value.get()'), ('(Enum)value.getDefault()', 'value.getDefault()')],
    'client/render/misc/PlushRenderer.java': [('AzEntityRendererConfig.builder(', 'AzEntityRendererConfig.<PlushEntity>builder(')],
    'entity/ModEntities.java': [('Builder.of(DragonEggEntity::new', 'Builder.<DragonEggEntity>of(DragonEggEntity::new'), ('Builder.of(SpikeProjectile::new', 'Builder.<SpikeProjectile>of(SpikeProjectile::new')],
    'item/ModCreativeModeTabs.java': [('CreativeModeTab.builder()', 'net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()')],
    'particle/ModParticles.java': [('new SimpleParticleType(', 'net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(')],
}
for relative, changes in replacements.items():
    path = ROOT / relative
    text = path.read_text(encoding='utf-8')
    for before, after in changes:
        assert before in text, (path, before)
        text = text.replace(before, after)
    path.write_text(text, encoding='utf-8')
