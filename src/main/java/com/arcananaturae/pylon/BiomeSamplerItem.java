package com.arcananaturae.pylon;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aura.BiomeAspects;
import com.leclowndu93150.thaumaturge.registry.TTDataMaps;
import com.arcananaturae.MNRegistry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;

/**
 * Biome Sampler (the original Biome Report): sneak-use on the ground to record the biome there, then use it on a Geo
 * Pylon to make that biome the pylon's target. The tooltip lists the biome's aspects, which are what the pylon will
 * ask for.
 */
public class BiomeSamplerItem extends Item {
    public BiomeSamplerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!(context.getLevel() instanceof ServerLevel level) || player == null) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            Holder<Biome> biome = level.getBiome(context.getClickedPos());
            biome.unwrapKey().ifPresent(key -> {
                stack.set(MNRegistry.SAMPLED_BIOME.get(), key);
                player.sendOverlayMessage(Component.translatable("message.arcananaturae.sampled", biomeName(key)));
                level.playSound(null, context.getClickedPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            });
            return InteractionResult.SUCCESS_SERVER;
        }
        ResourceKey<Biome> sampled = stack.get(MNRegistry.SAMPLED_BIOME.get());
        if (sampled != null && level.getBlockEntity(context.getClickedPos()) instanceof GeoPylonBlockEntity pylon) {
            pylon.setTarget(sampled);
            player.sendOverlayMessage(Component.translatable("message.arcananaturae.pylon_target", biomeName(sampled)));
            level.playSound(null, context.getClickedPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    private static Component biomeName(ResourceKey<Biome> key) {
        return Component.translatable(key.identifier().toLanguageKey("biome"));
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceKey<Biome> sampled = stack.get(MNRegistry.SAMPLED_BIOME.get());
        return sampled == null ? super.getName(stack) : Component.translatable("item.arcananaturae.biome_sampler.of", biomeName(sampled));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ResourceKey<Biome> sampled = stack.get(MNRegistry.SAMPLED_BIOME.get());
        if (sampled == null) {
            tooltip.accept(Component.translatable("tooltip.arcananaturae.sampler_empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        var registries = context.registries();
        if (registries == null) {
            return;
        }
        registries.lookup(Registries.BIOME).flatMap(r -> r.get(sampled)).ifPresent(biome -> {
            BiomeAspects aspects = biome.getData(TTDataMaps.BIOME_ASPECTS);
            if (aspects != null) {
                for (ResourceKey<IAspect> aspect : aspects.aspects()) {
                    tooltip.accept(Component.translatable("aspect." + aspect.identifier().getNamespace() + "." + aspect.identifier().getPath())
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        });
    }
}
