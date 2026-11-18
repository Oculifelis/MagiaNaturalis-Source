package com.arcananaturae;

import com.leclowndu93150.thaumaturge.content.equipment.GearWarp;
import com.leclowndu93150.thaumaturge.content.equipment.TTMaterials;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.arcananaturae.chest.ArcaneChestBlock;
import com.arcananaturae.chest.ArcaneChestBlockEntity;
import com.arcananaturae.chest.ArcaneChestMenu;
import com.arcananaturae.chest.ArcaneChestType;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.arcananaturae.item.EndericPouchItem;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import com.arcananaturae.entity.EvilTrunk;
import com.arcananaturae.entity.Revenant;
import com.arcananaturae.item.EvilTrunkItem;
import com.arcananaturae.pylon.BiomeSamplerItem;
import com.arcananaturae.pylon.GeoPylonBlock;
import com.arcananaturae.pylon.GeoPylonBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.biome.Biome;
import com.arcananaturae.item.KeyPlayers;
import com.arcananaturae.item.PrisonJarItem;
import com.arcananaturae.spell.ConstructEffect;
import com.arcananaturae.spell.RevenantEffect;
import com.arcananaturae.item.MutationStoneItem;
import com.arcananaturae.item.QuicksilverStoneItem;
import com.arcananaturae.item.ResearchLogItem;
import com.arcananaturae.item.RevealingGlassesItem;
import com.arcananaturae.item.SickleItem;
import com.arcananaturae.item.SickleReach;
import com.arcananaturae.item.ThaumiumKeyItem;
import com.arcananaturae.table.TranscribingTableBlock;
import com.arcananaturae.table.TranscribingTableBlockEntity;
import com.arcananaturae.table.TranscribingTableMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MNRegistry {
    private static final String MODID = ArcanaNaturae.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<SpellBehaviorType<?>> SPELL_BEHAVIORS = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, MODID);

    // Spell effects; the parts that use them are datapack JSON (data/arcananaturae/thaumaturge/spell_part).
    public static final Supplier<SpellBehaviorType<ConstructEffect>> CONSTRUCT_BEHAVIOR = SPELL_BEHAVIORS.register("construct",
            () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, ConstructEffect.CODEC));
    public static final Supplier<SpellBehaviorType<RevenantEffect>> REVENANT_BEHAVIOR = SPELL_BEHAVIORS.register("revenant",
            () -> new SpellBehaviorType<>(SpellPartKind.EFFECT, RevenantEffect.CODEC));
    public static final Supplier<EntityType<Revenant>> REVENANT = ENTITIES.register("revenant",
            () -> EntityType.Builder.<Revenant>of(Revenant::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ArcanaNaturae.id("revenant"))));

    /** Blocks a sickle cuts quickly and reaps in an area (plants, crops, leaves, webs). */
    public static final TagKey<Block> SICKLE_MINEABLE = TagKey.create(Registries.BLOCK, ArcanaNaturae.id("mineable/sickle"));

    /** How far a sickle's reap spreads; set per item, so a datapack or another mod can retune a stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SickleReach>> SICKLE_REACH = COMPONENTS.registerComponentType("sickle_reach",
            b -> b.persistent(SickleReach.CODEC).networkSynchronized(SickleReach.STREAM_CODEC));

    /** Forger, bound player and endorsed players of a thaumium key. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<KeyPlayers>> KEY_PLAYERS = COMPONENTS.registerComponentType("key_players",
            b -> b.persistent(KeyPlayers.CODEC).networkSynchronized(KeyPlayers.STREAM_CODEC));

    /** Research points stored in a Research Log. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AspectList>> RESEARCH_POINTS = COMPONENTS.registerComponentType("research_points",
            b -> b.persistent(AspectList.CODEC).networkSynchronized(AspectList.STREAM_CODEC));

    /** The biome a Biome Sampler recorded. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceKey<Biome>>> SAMPLED_BIOME = COMPONENTS.registerComponentType("sampled_biome",
            b -> b.persistent(ResourceKey.codec(Registries.BIOME)).networkSynchronized(ResourceKey.streamCodec(Registries.BIOME)));

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    // Original: radius 2 / 4 / 9 (BFS layers), thaumium, void and elemental tool materials.
    public static final DeferredItem<SickleItem> THAUMIUM_SICKLE = tab(ITEMS.registerItem("thaumium_sickle",
            p -> new SickleItem(p, false), p -> TTMaterials.TOOL_THAUMIUM.applyToolProperties(p, SICKLE_MINEABLE, 2.5f, -2.4f, 0f)
                    .component(SICKLE_REACH.get(), new SickleReach(2, 48)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SickleItem> VOID_SICKLE = tab(ITEMS.registerItem("void_sickle",
            p -> new SickleItem(p, true), p -> GearWarp.with(TTMaterials.TOOL_VOID.applyToolProperties(p, SICKLE_MINEABLE, 2.5f, -2.4f, 0f), 1)
                    .component(SICKLE_REACH.get(), new SickleReach(4, 128)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SickleItem> ELEMENTAL_SICKLE = tab(ITEMS.registerItem("elemental_sickle",
            p -> new SickleItem(p, false), p -> TTMaterials.TOOL_ELEMENTAL.applyToolProperties(p, SICKLE_MINEABLE, 3.0f, -2.4f, 0f)
                    .component(SICKLE_REACH.get(), new SickleReach(9, 256, true, 2)).rarity(Rarity.RARE)));
    public static final DeferredItem<MutationStoneItem> MUTATION_STONE = tab(ITEMS.registerItem("mutation_stone",
            MutationStoneItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<QuicksilverStoneItem> QUICKSILVER_STONE = tab(ITEMS.registerItem("quicksilver_stone",
            QuicksilverStoneItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Carpentry (the original's seven-metadata Arcane Wood block).
    public static final DeferredBlock<Block> GREATWOOD_PANEL = wood("greatwood_panel", Block::new, MapColor.COLOR_BROWN);
    public static final DeferredBlock<Block> CARVED_GREATWOOD = wood("carved_greatwood", Block::new, MapColor.COLOR_BROWN);
    public static final DeferredBlock<Block> SILVERWOOD_PANEL = wood("silverwood_panel", Block::new, MapColor.SAND);
    public static final DeferredBlock<Block> SILVERWOOD_TILES = wood("silverwood_tiles", Block::new, MapColor.SAND);
    public static final DeferredBlock<Block> GILDED_GREATWOOD = wood("gilded_greatwood", Block::new, MapColor.GOLD);
    public static final DeferredBlock<Block> GILDED_GREATWOOD_ROSETTE = wood("gilded_greatwood_rosette", Block::new, MapColor.GOLD);
    public static final DeferredBlock<RotatedPillarBlock> GOLD_TRIMMED_GREATWOOD = wood("gold_trimmed_greatwood", RotatedPillarBlock::new, MapColor.COLOR_BROWN);

    // Revealing head gear. Original: 350 durability, gold repair, 6% (spectacles) and 5-9% (dark goggles) discount.
    public static final ResourceKey<EquipmentAsset> SPECTACLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ArcanaNaturae.id("spectacles"));
    public static final ResourceKey<EquipmentAsset> DARK_GOGGLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ArcanaNaturae.id("dark_crystal_goggles"));
    public static final DeferredItem<RevealingGlassesItem> SPECTACLES = tab(ITEMS.registerItem("spectacles",
            p -> new RevealingGlassesItem(p, 6, false), p -> glasses(p, SPECTACLES_ASSET).rarity(Rarity.EPIC)));
    public static final DeferredItem<RevealingGlassesItem> DARK_CRYSTAL_GOGGLES = tab(ITEMS.registerItem("dark_crystal_goggles",
            p -> new RevealingGlassesItem(p, 7, true), p -> glasses(p, DARK_GOGGLES_ASSET).rarity(Rarity.RARE)));

    public static final DeferredItem<ThaumiumKeyItem> UNRAVELING_KEY = tab(ITEMS.registerItem("unraveling_key",
            p -> new ThaumiumKeyItem(p, false), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ThaumiumKeyItem> ENDORSING_KEY = tab(ITEMS.registerItem("endorsing_key",
            p -> new ThaumiumKeyItem(p, true), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Arcane chests: wood-strong for those with access, blast-proof for everyone.
    public static final DeferredBlock<ArcaneChestBlock> GREATWOOD_ARCANE_CHEST = chest("greatwood_arcane_chest", ArcaneChestType.GREATWOOD, MapColor.COLOR_BROWN);
    public static final DeferredBlock<ArcaneChestBlock> SILVERWOOD_ARCANE_CHEST = chest("silverwood_arcane_chest", ArcaneChestType.SILVERWOOD, MapColor.SAND);
    public static final Supplier<BlockEntityType<ArcaneChestBlockEntity>> ARCANE_CHEST_ENTITY = BLOCK_ENTITIES.register("arcane_chest",
            () -> new BlockEntityType<>(ArcaneChestBlockEntity::new, GREATWOOD_ARCANE_CHEST.get(), SILVERWOOD_ARCANE_CHEST.get()));
    public static final Supplier<MenuType<ArcaneChestMenu>> GREATWOOD_CHEST_MENU = MENUS.register("greatwood_arcane_chest",
            () -> IMenuTypeExtension.create((id, inv, buf) -> ArcaneChestMenu.client(ArcaneChestType.GREATWOOD, id, inv)));
    public static final Supplier<MenuType<ArcaneChestMenu>> SILVERWOOD_CHEST_MENU = MENUS.register("silverwood_arcane_chest",
            () -> IMenuTypeExtension.create((id, inv, buf) -> ArcaneChestMenu.client(ArcaneChestType.SILVERWOOD, id, inv)));

    public static final DeferredItem<PrisonJarItem> PRISON_JAR = tab(ITEMS.registerItem("prison_jar",
            PrisonJarItem::new, p -> p.stacksTo(16).rarity(Rarity.UNCOMMON)));

    // Research Log, Transcribing Table, Enderic Focus Pouch.
    public static final DeferredItem<ResearchLogItem> RESEARCH_LOG = tab(ITEMS.registerItem("research_log",
            ResearchLogItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<EndericPouchItem> ENDERIC_POUCH = tab(ITEMS.registerItem("enderic_pouch",
            EndericPouchItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredBlock<TranscribingTableBlock> TRANSCRIBING_TABLE = BLOCKS.registerBlock("transcribing_table",
            TranscribingTableBlock::new, p -> p.mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS).strength(2.5f).sound(SoundType.WOOD));
    static {
        tab(ITEMS.registerSimpleBlockItem(TRANSCRIBING_TABLE));
    }
    public static final Supplier<BlockEntityType<TranscribingTableBlockEntity>> TRANSCRIBING_TABLE_ENTITY = BLOCK_ENTITIES.register("transcribing_table",
            () -> new BlockEntityType<>(TranscribingTableBlockEntity::new, TRANSCRIBING_TABLE.get()));
    public static final Supplier<MenuType<TranscribingTableMenu>> TRANSCRIBING_TABLE_MENU = MENUS.register("transcribing_table",
            () -> IMenuTypeExtension.create((id, inv, buf) -> TranscribingTableMenu.client(id, inv)));

    // Evil Trunks, Geo Pylon, Biome Sampler.
    public static final Supplier<EntityType<EvilTrunk>> EVIL_TRUNK = ENTITIES.register("evil_trunk",
            () -> EntityType.Builder.<EvilTrunk>of(EvilTrunk::new, MobCategory.CREATURE).sized(0.9f, 0.95f).fireImmune().clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ArcanaNaturae.id("evil_trunk"))));
    public static final Map<EvilTrunk.TrunkVariant, DeferredItem<EvilTrunkItem>> TRUNKS = new EnumMap<>(EvilTrunk.TrunkVariant.class);
    static {
        for (EvilTrunk.TrunkVariant variant : EvilTrunk.TrunkVariant.values()) {
            TRUNKS.put(variant, tab(ITEMS.registerItem(variant.id() + "_trunk", p -> new EvilTrunkItem(variant, p),
                    p -> p.stacksTo(1).rarity(Rarity.RARE))));
        }
    }
    public static final DeferredItem<BiomeSamplerItem> BIOME_SAMPLER = tab(ITEMS.registerItem("biome_sampler",
            BiomeSamplerItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<GeoPylonBlock> GEO_PYLON = BLOCKS.registerBlock("geo_pylon",
            GeoPylonBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).strength(5.0f, 1200.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    static {
        tab(ITEMS.registerSimpleBlockItem(GEO_PYLON, p -> p.rarity(Rarity.RARE)));
    }
    public static final Supplier<BlockEntityType<GeoPylonBlockEntity>> GEO_PYLON_ENTITY = BLOCK_ENTITIES.register("geo_pylon",
            () -> new BlockEntityType<>(GeoPylonBlockEntity::new, GEO_PYLON.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcananaturae"))
            .icon(() -> new ItemStack(MUTATION_STONE.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private MNRegistry() {
    }

    private static <I extends Item> DeferredItem<I> tab(DeferredItem<I> item) {
        TAB_ITEMS.add(item);
        return item;
    }

    private static <B extends Block> DeferredBlock<B> wood(String name, Function<Block.Properties, B> factory, MapColor colour) {
        UnaryOperator<Block.Properties> props = p -> p.mapColor(colour).instrument(NoteBlockInstrument.BASS)
                .strength(2.0f, 5.0f).sound(SoundType.WOOD).ignitedByLava();
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props);
        tab(ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static Item.Properties glasses(Item.Properties p, ResourceKey<EquipmentAsset> asset) {
        return p.stacksTo(1).durability(350).repairable(Items.GOLD_INGOT).enchantable(25)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GOLD).setAsset(asset).build())
                .component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR, new AttributeModifier(asset.identifier(), 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD).build());
    }

    private static DeferredBlock<ArcaneChestBlock> chest(String name, ArcaneChestType type, MapColor colour) {
        DeferredBlock<ArcaneChestBlock> block = BLOCKS.registerBlock(name, p -> new ArcaneChestBlock(type, p),
                p -> p.mapColor(colour).instrument(NoteBlockInstrument.BASS).strength(2.5f, 3600000.0f).sound(SoundType.WOOD));
        tab(ITEMS.registerItem(name, p -> new BlockItem(block.get(), p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON).useBlockDescriptionPrefix()));
        return block;
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(REVENANT.get(), Zombie.createAttributes().build());
        event.put(EVIL_TRUNK.get(), EvilTrunk.createAttributes().build());
    }

    static void register(IEventBus bus) {
        bus.addListener(MNRegistry::attributes);
        ENTITIES.register(bus);
        SPELL_BEHAVIORS.register(bus);
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}
