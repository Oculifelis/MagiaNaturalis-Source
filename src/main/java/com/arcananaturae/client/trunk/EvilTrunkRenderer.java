package com.arcananaturae.client.trunk;

import com.arcananaturae.ArcanaNaturae;
import com.arcananaturae.entity.EvilTrunk;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class EvilTrunkRenderer extends MobRenderer<EvilTrunk, EvilTrunkRenderState, EvilTrunkModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ArcanaNaturae.id("evil_trunk"), "main");

    public EvilTrunkRenderer(EntityRendererProvider.Context context) {
        super(context, new EvilTrunkModel(context.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public EvilTrunkRenderState createRenderState() {
        return new EvilTrunkRenderState();
    }

    @Override
    public void extractRenderState(EvilTrunk entity, EvilTrunkRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
        state.open = entity.isOpen();
    }

    @Override
    public Identifier getTextureLocation(EvilTrunkRenderState state) {
        return ArcanaNaturae.id("textures/entity/trunk/" + state.variant.id() + ".png");
    }
}
