package com.arcananaturae.entity;

import com.arcananaturae.MNRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Evil Trunk: a tamed, walking 36-slot chest with teeth. It follows its owner, bites what the owner fights or what
 * hurts the owner, regenerates slowly, eats food to heal, and ignores fire, lava, drowning and falls. The owner opens it
 * with a plain right-click and packs it back into its item by sneaking with an empty hand (the contents stay inside).
 */
public class EvilTrunk extends TamableAnimal {
    public static final int SIZE = 36;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(EvilTrunk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(EvilTrunk.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer inventory = new SimpleContainer(SIZE);
    private final List<Player> viewers = new ArrayList<>();

    public EvilTrunk(EntityType<? extends EvilTrunk> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 75.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 8.0F, 2.0F));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(OPEN, false);
    }

    public TrunkVariant variant() {
        return TrunkVariant.byId(entityData.get(VARIANT));
    }

    public void setVariant(TrunkVariant variant) {
        entityData.set(VARIANT, variant.ordinal());
    }

    public boolean isOpen() {
        return entityData.get(OPEN);
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        viewers.removeIf(p -> p.isRemoved() || !(p.containerMenu instanceof ChestMenu menu) || menu.getContainer() != inventory);
        entityData.set(OPEN, !viewers.isEmpty());
        if (tickCount % 50 == 0 && getHealth() < getMaxHealth()) {
            heal(1.0F);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) {
                heal(food.nutrition());
                stack.consume(1, player);
                playSound(getHealth() >= getMaxHealth() ? SoundEvents.PLAYER_BURP : SoundEvents.FOX_EAT, 0.5F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() && stack.isEmpty()) {
            pack(player);
        } else if (!player.isSecondaryUseActive()) {
            viewers.add(player);
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x4, id, inv, inventory, 4), getDisplayName()));
            playSound(SoundEvents.CHEST_OPEN, 0.5F, 1.0F);
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Back into the spawner item, contents and name included. */
    private void pack(Player player) {
        ItemStack item = new ItemStack(variant().item());
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        boolean any = false;
        for (int i = 0; i < SIZE; i++) {
            items.set(i, inventory.removeItemNoUpdate(i));
            any |= !items.get(i).isEmpty();
        }
        if (any) {
            item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
        Component name = getCustomName();
        if (name != null) {
            item.set(DataComponents.CUSTOM_NAME, name);
        }
        if (!player.addItem(item)) {
            spawnAtLocation((ServerLevel) level(), item);
        }
        playSound(SoundEvents.CHEST_CLOSE, 0.5F, 1.0F);
        discard();
    }

    /** Called by the spawner item right after the trunk appears. */
    public void unpack(Player owner, ItemStack from, TrunkVariant variant) {
        setVariant(variant);
        tame(owner);
        ItemContainerContents contents = from.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            contents.copyInto(items);
            for (int i = 0; i < SIZE; i++) {
                inventory.setItem(i, items.get(i));
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) {
            return false;
        }
        return !(target instanceof Player victim && owner instanceof Player master && !master.canHarmPlayer(victim));
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel) {
            Containers.dropContents(level(), this, inventory);
        }
        super.die(source);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("trunk_variant", variant().ordinal());
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(TrunkVariant.byId(input.getIntOr("trunk_variant", 0)));
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            playSound(SoundEvents.BLAZE_HURT, 0.5F, 0.9F + random.nextFloat() * 0.1F);
        }
        return hit;
    }

    /** Spawner item, entity texture and lang name for each look. */
    public enum TrunkVariant {
        CORRUPTED, SINISTER, DEMONIC, TAINTED;

        public static TrunkVariant byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : CORRUPTED;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public net.minecraft.world.item.Item item() {
            return MNRegistry.TRUNKS.get(this).get();
        }
    }
}
