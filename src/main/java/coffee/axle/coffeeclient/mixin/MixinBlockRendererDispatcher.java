package coffee.axle.coffeeclient.mixin;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.feature.render.BedESP;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {

    @Inject(method = "renderBlock", at = @At("HEAD"))
    private void onRenderBlock(
            IBlockState state,
            BlockPos pos,
            IBlockAccess blockAccess,
            WorldRenderer worldRenderer,
            CallbackInfoReturnable<Boolean> cir) {
        if (CoffeeClient.featureManager != null) {
            BedESP bedESP = (BedESP) CoffeeClient.featureManager.getFeature(BedESP.class);
            if (bedESP != null && bedESP.isEnabled() &&
                    state.getBlock() instanceof BlockBed &&
                    state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                bedESP.beds.add(new BlockPos(pos));
            }
        }
    }
}
