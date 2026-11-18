package com.arcananaturae.item;

import com.arcananaturae.ArcanaNaturae;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Prison Jar: use it on a creature to seal it inside (one creature per jar), use the full jar on a block to let it out
 * again. The creature is kept in vanilla's {@code minecraft:entity_data} component, the way spawn eggs carry theirs.
 * Players, bosses ({@code c:bosses}) and anything in {@code arcananaturae:prison_jar_blacklist} cannot be caught.
 */
public class PrisonJarItem extends Item {
    public static final TagKey<EntityType<?>> BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, ArcanaNaturae.id("prison_jar_blacklist"));
    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "bosses"));

    public PrisonJarItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean canCatch(LivingEntity entity) {
        return entity instanceof PathfinderMob && entity.isAlive() && !entity.getType().builtInRegistryHolder().is(BLACKLIST) && !entity.getType().builtInRegistryHolder().is(BOSSES)
                && entity.getType().canSerialize() && !entity.isVehicle() && !entity.isPassenger();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (stack.has(DataComponents.ENTITY_DATA) || !canCatch(target)) {
            return InteractionResult.PASS;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        target.saveWithoutId(output);
        CompoundTag data = output.buildResult();
        for (String key : new String[]{"Pos", "Motion", "UUID", "Rotation", "FallDistance", "Leash"}) {
            data.remove(key);
        }
        ItemStack full = stack.copyWithCount(1);
        full.set(DataComponents.ENTITY_DATA, TypedEntityData.of(target.getType(), data));
        stack.consume(1, player);
        if (!player.getInventory().add(full)) {
            player.drop(full, false);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 0.8f, 0.8f);
        target.discard();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
        if (data == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.mayInteract(context.getPlayer(), pos)) {
            return InteractionResult.FAIL;
        }
        if (data.type().spawn(level, stack, context.getPlayer(), pos, EntitySpawnReason.BUCKET, true, false) == null) {
            return InteractionResult.FAIL;
        }
        stack.remove(DataComponents.ENTITY_DATA);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8f, 0.8f);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.ENTITY_DATA);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
        builder.accept(data == null
                ? Component.translatable("tooltip.arcananaturae.prison_jar_empty").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.arcananaturae.prison_jar_holds", data.type().getDescription()).withStyle(ChatFormatting.GOLD));
    }
}
