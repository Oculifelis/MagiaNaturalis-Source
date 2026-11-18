package com.arcananaturae.compat;

import com.arcananaturae.MNRegistry;
import com.arcananaturae.ArcanaNaturae;
import com.arcananaturae.morph.MorphCycles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * JEI: a Phenomorphous Stone category with every step of the datapack mutation cycles (block in, the look it turns
 * into), and information pages for the items that work in the world, reusing their research text. Arcane, infusion
 * and crafting recipes are shown by Thaumaturge and JEI. Loaded only with JEI.
 */
@JeiPlugin
public final class MNJeiPlugin implements IModPlugin {
    record Morph(Item from, Item to) {
    }

    static final IRecipeType<Morph> MUTATION = IRecipeType.create(ArcanaNaturae.id("mutation"), Morph.class);

    /** Item -> the research entry whose second page explains it. */
    private static final Map<String, String> INFO = Map.ofEntries(
            Map.entry("mutation_stone", "mutation_stone"), Map.entry("geo_pylon", "geo_occultism"),
            Map.entry("biome_sampler", "geo_occultism"), Map.entry("prison_jar", "prison_jar"),
            Map.entry("quicksilver_stone", "quicksilver_stone"), Map.entry("research_log", "research_log"),
            Map.entry("transcribing_table", "transcribing_table"), Map.entry("enderic_pouch", "ender_pouch"),
            Map.entry("unraveling_key", "arcane_keys"), Map.entry("endorsing_key", "arcane_keys"),
            Map.entry("corrupted_trunk", "evil_trunk"), Map.entry("demonic_trunk", "evil_trunk"),
            Map.entry("sinister_trunk", "evil_trunk"), Map.entry("tainted_trunk", "evil_trunk"),
            Map.entry("greatwood_arcane_chest", "arcane_chest"), Map.entry("silverwood_arcane_chest", "arcane_chest"),
            Map.entry("thaumium_sickle", "sickles"), Map.entry("void_sickle", "void_sickle"),
            Map.entry("elemental_sickle", "sickle_of_abundance"));

    @Override
    public Identifier getPluginUid() {
        return ArcanaNaturae.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MutationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(MUTATION, MNRegistry.MUTATION_STONE.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<Morph> morphs = new ArrayList<>();
        MorphCycles.all().forEach((from, to) -> {
            Item a = from.asItem();
            Item b = to.asItem();
            if (a != Items.AIR && b != Items.AIR && a != b) {
                morphs.add(new Morph(a, b));
            }
        });
        morphs.sort((x, y) -> BuiltInRegistries.ITEM.getKey(x.from()).compareTo(BuiltInRegistries.ITEM.getKey(y.from())));
        registration.addRecipes(MUTATION, morphs);
        INFO.forEach((name, research) -> BuiltInRegistries.ITEM.getOptional(ArcanaNaturae.id(name)).ifPresent(item ->
                registration.addIngredientInfo(item, Component.translatable("research.arcananaturae." + research + ".stage_1"))));
    }

    private static final class MutationCategory extends AbstractRecipeCategory<Morph> {
        MutationCategory(IGuiHelper gui) {
            super(MUTATION, Component.translatable("jei.arcananaturae.mutation"), gui.createDrawableItemLike(MNRegistry.MUTATION_STONE.get()), 82, 26);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Morph recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 4).setStandardSlotBackground().add(new ItemStack(recipe.from()));
            builder.addOutputSlot(60, 4).setOutputSlotBackground().add(new ItemStack(recipe.to()));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Morph recipe, IFocusGroup focuses) {
            builder.addRecipeArrowWidget().setPosition(28, 4);
        }
    }
}
