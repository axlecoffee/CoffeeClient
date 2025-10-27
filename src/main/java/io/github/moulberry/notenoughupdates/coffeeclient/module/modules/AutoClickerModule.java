package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ItemUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RandomUtil;
import io.github.moulberry.notenoughupdates.mixins.IAccessorMinecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

public class AutoClickerModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private long nextClickTime = 0L;
    private long nextBlockHitTime = 0L;

    public final IntProperty minCPS = new IntProperty("min-cps", 8, 1, 20);
    public final IntProperty maxCPS = new IntProperty("max-cps", 12, 1, 20);
    public final BooleanProperty blockHit = new BooleanProperty("block-hit", false);
    public final IntProperty blockHitChance = new IntProperty("block-hit-chance", 100, 0, 100, blockHit::getValue);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, weaponsOnly::getValue);
    public final BooleanProperty breakBlocks = new BooleanProperty("break-blocks", true);

    public AutoClickerModule() {
        super("AutoClicker", false);
    }

    private long getNextClickDelay() {
        int cps = (int) RandomUtil.nextLong(minCPS.getValue(), maxCPS.getValue() + 1);
        return 1000L / cps;
    }

    private boolean isBreakingBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private boolean canClick() {
        if (weaponsOnly.getValue()) {
            if (!ItemUtil.isHoldingSword() && !(allowTools.getValue() && ItemUtil.isHoldingTool())) {
                return false;
            }
        }

        if (breakBlocks.getValue() && isBreakingBlock()) {
            GameType gameType = mc.playerController.getCurrentGameType();
            return gameType != GameType.SURVIVAL && gameType != GameType.CREATIVE;
        }

        return true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.currentScreen != null) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            long currentTime = System.currentTimeMillis();

            if (canClick() && Mouse.isButtonDown(0)) {
                if (currentTime >= nextClickTime) {
                    ((IAccessorMinecraft) mc).invokeClickMouse();
                    nextClickTime = currentTime + getNextClickDelay();

                    if (blockHit.getValue()
                            && ItemUtil.isHoldingSword()
                            && Mouse.isButtonDown(1)
                            && Math.random() * 100 < blockHitChance.getValue()
                            && currentTime >= nextBlockHitTime) {
                        ((IAccessorMinecraft) mc).invokeRightClickMouse();
                        nextBlockHitTime = currentTime + 100;
                    }
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        nextClickTime = 0L;
        nextBlockHitTime = 0L;
    }
}
