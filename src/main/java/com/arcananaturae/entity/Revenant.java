package com.arcananaturae.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A baby zombie raised by the Revenant spell. It fights for its caster: it never targets the caster or the caster's
 * other revenants, does not burn in daylight, drops nothing and crumbles after a minute (the original's raised
 * zombies stayed forever and turned on their owner once their quarry died).
 */
public class Revenant extends Zombie {
    private static final int LIFESPAN = 1200;

    private @Nullable UUID owner;
    private int age;

    public Revenant(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        setBaby(true);
        xpReward = 0;
    }

    public void summon(LivingEntity caster, @Nullable LivingEntity quarry, float power) {
        owner = caster.getUUID();
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(2.0 + 0.5 * Math.max(0, power - 1));
        }
        if (quarry != null && canAttack(quarry)) {
            setTarget(quarry);
        }
    }

    public boolean ownedBy(Entity entity) {
        return owner != null && owner.equals(entity.getUUID());
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (ownedBy(target) || target instanceof Revenant other && owner != null && owner.equals(other.owner)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    protected boolean considersEntityAsAlly(Entity entity) {
        return ownedBy(entity) || super.considersEntityAsAlly(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && ++age >= LIFESPAN) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 12, 0.2, 0.3, 0.2, 0.01);
            discard();
        }
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("revenant_age", age);
        if (owner != null) {
            output.putString("revenant_owner", owner.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("revenant_age", 0);
        owner = input.getString("revenant_owner").map(UUID::fromString).orElse(null);
    }
}
