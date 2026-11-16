package rearth.oritech.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.client.cablesurfer.ClientZiplineHandler;
import rearth.oritech.item.tools.PortableLaserItem;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends LivingEntity> {

    

    @Inject(method = "m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    public void oritech$portableLaserPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player) || !(player.getMainHandItem().getItem() instanceof PortableLaserItem)) return;

        if (player.getMainArm() == HumanoidArm.RIGHT)
            ((HumanoidModel<?>) (Object) this).rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        else
            ((HumanoidModel<?>) (Object) this).leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }
    
    @Inject(method = "m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    public void oritech$ziplineAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        
        if (entity instanceof Player player) {
            
            if (ClientZiplineHandler.isZiplining(player)) {
                
                // Raise arms overhead
                // -3.14 (PI) is straight up. We do -2.9 to angle slightly forward
                float armX = -2.9F;
                ((HumanoidModel<?>) (Object) this).rightArm.xRot = armX;
                ((HumanoidModel<?>) (Object) this).leftArm.xRot = armX;
                
                // Bring arms inward to touch hands
                // Right arm rotates CCW (-), Left arm CW (+)
                ((HumanoidModel<?>) (Object) this).rightArm.zRot = 0.1F;
                ((HumanoidModel<?>) (Object) this).leftArm.zRot = 0.05F;
                
                // Rotate arms inward on Y axis to align the Wrench handle horizontally
                ((HumanoidModel<?>) (Object) this).rightArm.yRot = -0.3F + 2.1f;
                ((HumanoidModel<?>) (Object) this).leftArm.yRot = 0.3F;
                
                // move slightly up because the default arms are too short
                ((HumanoidModel<?>) (Object) this).rightArm.y -= 3.0F;
                ((HumanoidModel<?>) (Object) this).leftArm.y -= 3.0F;
                
                ((HumanoidModel<?>) (Object) this).rightArm.yScale = 1.15F;
                ((HumanoidModel<?>) (Object) this).leftArm.yScale = 1.15F;
                
                
                // Apply a slight swing based on momentum/time
                // We use ageInTicks to create a gentle oscillation
                float legSwing = Mth.cos(ageInTicks * 0.2F) * 0.25F;
                
                // Bend knees slightly back (positive X)
                float kneeBend = 0.4F;
                
                ((HumanoidModel<?>) (Object) this).rightLeg.xRot = kneeBend + legSwing;
                ((HumanoidModel<?>) (Object) this).leftLeg.xRot = kneeBend - legSwing;
                
                ((HumanoidModel<?>) (Object) this).rightLeg.yRot = 0.0F;
                ((HumanoidModel<?>) (Object) this).leftLeg.yRot = 0.0F;
                ((HumanoidModel<?>) (Object) this).rightLeg.zRot = 0.0F;
                ((HumanoidModel<?>) (Object) this).leftLeg.zRot = 0.0F;
                
                // Tilt body slightly forward to look dynamic
                ((HumanoidModel<?>) (Object) this).body.xRot = 0.1F;
            }
        }
    }
}
