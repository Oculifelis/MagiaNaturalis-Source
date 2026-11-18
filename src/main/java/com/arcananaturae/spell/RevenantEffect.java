package com.arcananaturae.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.arcananaturae.MNRegistry;
import com.arcananaturae.entity.Revenant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Revenant (the original Revenant Focus): raises a baby zombie beside the target that attacks it. At most
 * {@code max_active} revenants per caster stay up at once; players are only targeted when PvP is on.
 */
public final class RevenantEffect extends AbstractEffectBehavior {
    public static final MapCodec<RevenantEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 32).optionalFieldOf("max_active", 6).forGetter(RevenantEffect::maxActive)).apply(i, RevenantEffect::new));

    private final int maxActive;

    public RevenantEffect(int maxActive) {
        this.maxActive = maxActive;
    }

    public int maxActive() {
        return maxActive;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return MNRegistry.REVENANT_BEHAVIOR.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        ServerLevel level = ctx.level();
        LivingEntity caster = ctx.caster();
        LivingEntity quarry = target.entity().filter(e -> e != caster).filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                .filter(e -> !(e instanceof Player) || level.getGameRules().get(GameRules.PVP)).orElse(null);
        Vec3 at = quarry != null ? caster.position().add(caster.getLookAngle().multiply(1, 0, 1).normalize()) : target.position();
        long active = level.getEntitiesOfClass(Revenant.class, new AABB(at, at).inflate(48), r -> r.ownedBy(caster)).size();
        if (active >= maxActive) {
            return;
        }
        Revenant revenant = MNRegistry.REVENANT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (revenant == null) {
            return;
        }
        revenant.snapTo(at.x, at.y + 0.1, at.z, caster.getYRot(), 0.0F);
        if (!level.noCollision(revenant)) {
            return;
        }
        revenant.summon(caster, quarry, power);
        if (level.addFreshEntity(revenant)) {
            level.levelEvent(LevelEvent.PARTICLES_MOBBLOCK_SPAWN, BlockPos.containing(at), 0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ZOMBIE_AMBIENT, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
    }
}
