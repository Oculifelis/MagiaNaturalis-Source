package com.arcananaturae.client.trunk;

import com.arcananaturae.entity.EvilTrunk;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class EvilTrunkRenderState extends LivingEntityRenderState {
    public EvilTrunk.TrunkVariant variant = EvilTrunk.TrunkVariant.CORRUPTED;
    public boolean open;
}
