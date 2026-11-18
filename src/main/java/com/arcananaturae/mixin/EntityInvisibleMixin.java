package com.arcananaturae.mixin;

import com.arcananaturae.item.RevealingGlassesItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dark Crystal Goggles: invisible creatures render see-through for the wearer, as for a teammate. Client only. */
@Mixin(Entity.class)
public abstract class EntityInvisibleMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void arcananaturae$revealInvisible(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (RevealingGlassesItem.wearing(player, RevealingGlassesItem.REVEALS_INVISIBLE)) {
            cir.setReturnValue(false);
        }
    }
}
