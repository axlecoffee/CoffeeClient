package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.events.LeftClickMouseEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.FloatProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.ItemUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.KeyBindUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.RandomUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Objects;

public class AutoClickerModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean clickPending = false;
    private long clickDelay = 0L;
    private boolean blockHitPending = false;
    private long blockHitDelay = 0L;

    public final IntProperty minCPS = new IntProperty("min-cps", 8, 1, 20);
    public final IntProperty maxCPS = new IntProperty("max-cps", 12, 1, 20);
    public final BooleanProperty blockHit = new BooleanProperty("block-hit", false);
    public final FloatProperty blockHitTicks = new FloatProperty("block-hit-ticks", 1.5f, 1.0f, 20.0f,
            blockHit::getValue);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, weaponsOnly::getValue);
    public final BooleanProperty breakBlocks = new BooleanProperty("break-blocks", true);
    public final BooleanProperty inventoryFill = new BooleanProperty("inventory-fill", false);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);

    public AutoClickerModule() {
        super("AutoClicker", false);
    }

    private long getNextClickDelay() {
        return 1000L / RandomUtil.nextLong(minCPS.getValue(), maxCPS.getValue());
    }

    private long getBlockHitDelay() {
        return (long) (50.0f * blockHitTicks.getValue());
    }

    private boolean isBreakingBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private boolean canClick() {
        if (!weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || (allowTools.getValue() && ItemUtil.isHoldingTool())) {
            if (breakBlocks.getValue() && isBreakingBlock()) {
                GameType gameType = mc.playerController.getCurrentGameType();
                return gameType != GameType.SURVIVAL && gameType != GameType.CREATIVE;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isAttackKeyDown() {
        return KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    private boolean isUseKeyDown() {
        return KeyBindUtil.isKeyDown(mc.gameSettings.keyBindUseItem.getKeyCode());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (clickDelay > 0L) {
                clickDelay -= 50L;
            }
            if (blockHitDelay > 0L) {
                blockHitDelay -= 50L;
            }

            if (mc.currentScreen != null && !inventoryFill.getValue()) {
                clickPending = false;
                blockHitPending = false;
            } else {
                if (clickPending) {
                    clickPending = false;
                    KeyBindUtil.updateKeyState(mc.gameSettings.keyBindAttack.getKeyCode());
                }
                if (blockHitPending) {
                    blockHitPending = false;
                    KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
                }

                if (isEnabled() && canClick()
                        && (!requirePress.getValue() || isAttackKeyDown())) {
                    if (!mc.thePlayer.isUsingItem()) {
                        while (clickDelay <= 0L) {
                            clickPending = true;
                            clickDelay = clickDelay + getNextClickDelay();
                            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                            KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindAttack.getKeyCode());
                        }
                    }

                    if (blockHit.getValue()
                            && blockHitDelay <= 0L
                            && isUseKeyDown()
                            && ItemUtil.isHoldingSword()) {
                        blockHitPending = true;
                        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                        if (!mc.thePlayer.isUsingItem()) {
                            blockHitDelay = blockHitDelay + getBlockHitDelay();
                            KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (isEnabled() && !event.isCanceled()) {
            if (!clickPending) {
                clickDelay = clickDelay + getNextClickDelay();
            }
        }
    }

    @Override
    public void onEnabled() {
        clickDelay = 0L;
        blockHitDelay = 0L;
    }

    @Override
    public void verifyValue(String mode) {
        if (minCPS.getName().equals(mode)) {
            if (minCPS.getValue() > maxCPS.getValue()) {
                maxCPS.setValue(minCPS.getValue());
            }
        } else if (maxCPS.getName().equals(mode) && minCPS.getValue() > maxCPS.getValue()) {
            minCPS.setValue(maxCPS.getValue());
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(minCPS.getValue(), maxCPS.getValue())
                ? new String[] { minCPS.getValue().toString() }
                : new String[] { String.format("%d-%d", minCPS.getValue(), maxCPS.getValue()) };
    }
}
