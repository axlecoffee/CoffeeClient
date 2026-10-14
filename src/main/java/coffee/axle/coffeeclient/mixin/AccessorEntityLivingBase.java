package coffee.axle.coffeeclient.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface AccessorEntityLivingBase {
    @Accessor("jumpTicks")
    int getJumpTicks();

    @Accessor("jumpTicks")
    void setJumpTicks(int jumpTicks);
}
