package com.arcananaturae.client.trunk;

import com.arcananaturae.entity.EvilTrunk.TrunkVariant;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A chest-sized jaw and a skull lid hinged at the back, with teeth along both front edges. Each look adds its own
 * crown on the lid: stacked horns (corrupted), a ridge of spikes (sinister), big swept horns (demonic) or nubs (tainted).
 */
public final class EvilTrunkModel extends EntityModel<EvilTrunkRenderState> {
    private final ModelPart skull;
    private final ModelPart[] crowns = new ModelPart[TrunkVariant.values().length];

    public EvilTrunkModel(ModelPart root) {
        super(root);
        skull = root.getChild("skull");
        for (TrunkVariant variant : TrunkVariant.values()) {
            crowns[variant.ordinal()] = skull.getChild(variant.id());
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose hinge = PartPose.offset(0.0F, 19.0F, 7.0F);
        PartDefinition jaw = root.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, 0.0F, -14.0F, 14.0F, 5.0F, 14.0F), hinge);
        PartDefinition skull = root.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 19).addBox(-7.0F, -10.0F, -14.0F, 14.0F, 10.0F, 14.0F), hinge);
        CubeListBuilder upper = CubeListBuilder.create();
        CubeListBuilder lower = CubeListBuilder.create();
        for (float x : new float[]{-6.0F, -2.5F, 0.5F, 4.0F}) {
            upper.texOffs(0, 43).addBox(x, 0.0F, -14.5F, 2.0F, 2.0F, 1.0F);
            lower.texOffs(0, 43).addBox(x + 1.0F, -2.0F, -14.5F, 2.0F, 2.0F, 1.0F);
        }
        skull.addOrReplaceChild("upper_teeth", upper, PartPose.ZERO);
        jaw.addOrReplaceChild("lower_teeth", lower, PartPose.ZERO);

        // Corrupted: two stacked pairs of short horns.
        skull.addOrReplaceChild("corrupted", CubeListBuilder.create()
                .texOffs(8, 43).addBox(-6.0F, -12.0F, -12.0F, 2.0F, 2.0F, 2.0F).texOffs(8, 43).addBox(4.0F, -12.0F, -12.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(8, 43).addBox(-5.0F, -14.0F, -12.0F, 2.0F, 2.0F, 2.0F).texOffs(8, 43).addBox(3.0F, -14.0F, -12.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(16, 43).addBox(-4.5F, -15.0F, -11.5F, 1.0F, 1.0F, 1.0F).texOffs(16, 43).addBox(3.5F, -15.0F, -11.5F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        // Sinister: a ridge of spikes from front to back.
        CubeListBuilder spikes = CubeListBuilder.create();
        for (float z : new float[]{-12.0F, -8.5F, -5.0F, -1.5F}) {
            spikes.texOffs(24, 43).addBox(-0.5F, -12.0F, z, 1.0F, 2.0F, 1.0F);
            spikes.texOffs(24, 43).addBox(-5.5F, -11.0F, z, 1.0F, 1.0F, 1.0F).texOffs(24, 43).addBox(4.5F, -11.0F, z, 1.0F, 1.0F, 1.0F);
        }
        skull.addOrReplaceChild("sinister", spikes, PartPose.ZERO);
        // Demonic: long horns swept outward and back.
        PartDefinition demonic = skull.addOrReplaceChild("demonic", CubeListBuilder.create(), PartPose.ZERO);
        demonic.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(32, 43).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                .texOffs(16, 43).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, -10.0F, -10.0F, -0.45F, 0.0F, -0.55F));
        demonic.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(32, 43).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                .texOffs(16, 43).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(5.0F, -10.0F, -10.0F, -0.45F, 0.0F, 0.55F));
        // Tainted: fleshy nubs bursting out of the lid.
        skull.addOrReplaceChild("tainted", CubeListBuilder.create()
                .texOffs(44, 43).addBox(-5.0F, -12.0F, -11.0F, 2.0F, 3.0F, 2.0F).texOffs(44, 43).addBox(1.0F, -13.0F, -7.0F, 2.0F, 3.0F, 2.0F)
                .texOffs(44, 43).addBox(-2.0F, -11.0F, -4.0F, 2.0F, 2.0F, 2.0F).texOffs(44, 43).addBox(-8.0F, -6.0F, -9.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(44, 43).addBox(6.0F, -7.0F, -5.0F, 2.0F, 3.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EvilTrunkRenderState state) {
        super.setupAnim(state);
        for (TrunkVariant variant : TrunkVariant.values()) {
            crowns[variant.ordinal()].visible = variant == state.variant;
        }
        float lid;
        if (state.open) {
            lid = 0.55F + Mth.sin(state.ageInTicks * 0.15F) * 0.04F;
        } else {
            // Snaps while it walks, wider the faster it goes.
            lid = Math.abs(Mth.sin(state.walkAnimationPos * 0.6F)) * 0.25F * Math.min(1.0F, state.walkAnimationSpeed);
        }
        skull.xRot = -lid;
    }
}
