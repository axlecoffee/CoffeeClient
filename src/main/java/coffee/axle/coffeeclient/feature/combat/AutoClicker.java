package coffee.axle.coffeeclient.feature.combat;

import coffee.axle.coffeeclient.events.LeftClickMouseEvent;
import coffee.axle.coffeeclient.events.PacketEvent;
import coffee.axle.coffeeclient.events.RightClickMouseEvent;
import coffee.axle.coffeeclient.feature.Feature;
import coffee.axle.coffeeclient.property.properties.BooleanProperty;
import coffee.axle.coffeeclient.property.properties.FloatProperty;
import coffee.axle.coffeeclient.property.properties.IntProperty;
import coffee.axle.coffeeclient.util.HurtTimeTracker;
import coffee.axle.coffeeclient.util.ItemUtil;
import coffee.axle.coffeeclient.util.KeyBindUtil;
import coffee.axle.coffeeclient.util.RandomUtil;
import coffee.axle.coffeeclient.mixin.AccessorGuiContainer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Objects;

public class AutoClicker extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final HurtTimeTracker hurtTimeTracker = new HurtTimeTracker();
    private boolean clickPending = false;
    private long clickDelay = 0L;
    private boolean blockHitPending = false;
    private long blockHitHoldMillis = 0L;
    private long blockHitCycleMillis = 0L;

    public final IntProperty minCPS = new IntProperty("min-cps", 8, 1, 20);
    public final IntProperty maxCPS = new IntProperty("max-cps", 12, 1, 20);
    public final BooleanProperty blockHit = new BooleanProperty("block-hit", false);
    public final FloatProperty blockHitHold = new FloatProperty("block-hit-hold", 1.5f, 1.0f, 20.0f,
            this.blockHit::getValue);
    public final FloatProperty blockHitDelay = new FloatProperty("block-hit-delay", 0.0f, 0.0f, 20.0f,
            this.blockHit::getValue);
    public final IntProperty blockHitHurtTime = new IntProperty("block-hit-hurt-time", 6, 0, 10,
            this.blockHit::getValue);
    public final BooleanProperty weaponsOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanProperty breakBlocks = new BooleanProperty("break-blocks", true);
    public final BooleanProperty inventoryFill = new BooleanProperty("inventory-fill", false);
    public final BooleanProperty requirePress = new BooleanProperty("require-press", true);

    public AutoClicker() {
        super("AutoClicker", false);
    }

    /**
     * Returns whether the AutoClicker is enabled and actively producing clicks.
     * Used by AimAssist to maintain continuous aiming while auto-clicking.
     */
    public boolean isActivelyClicking() {
        return isEnabled() && clickPending;
    }

    private long getNextClickDelay() {
        return 1000L / RandomUtil.nextLong(minCPS.getValue(), maxCPS.getValue());
    }

    private long getBlockHitHold() {
        return (long) (50.0f * blockHitHold.getValue());
    }

    private long getBlockHitDelay() {
        return (long) (50.0f * blockHitDelay.getValue());
    }

    private boolean isBreakingBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private boolean canAutoClick() {
        if (this.weaponsOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant()) {
            if (!this.allowTools.getValue() || !ItemUtil.isHoldingTool()) {
                return false;
            }
        }

        if (!this.breakBlocks.getValue() || !this.isBreakingBlock()) {
            return true;
        }

        GameType gameType = mc.playerController.getCurrentGameType();
        return gameType != GameType.SURVIVAL && gameType != GameType.CREATIVE;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (clickDelay > 0L) {
                clickDelay -= 50L;
            }
            if (blockHitHoldMillis > 0L) {
                blockHitHoldMillis -= 50L;
            }
            if (blockHitCycleMillis > 0L) {
                blockHitCycleMillis -= 50L;
            }
            this.hurtTimeTracker.tick();

            if (mc.currentScreen != null) {

                clickPending = false;
                blockHitPending = false;

                if (inventoryFill.getValue()
                        && isEnabled()
                        && Mouse.isButtonDown(0)
                        && mc.currentScreen instanceof GuiContainer) {
                    GuiContainer container = (GuiContainer) mc.currentScreen;
                    AccessorGuiContainer accessor = (AccessorGuiContainer) container;
                    int mouseX = Mouse.getX() * container.width / mc.displayWidth;
                    int mouseY = container.height - Mouse.getY() * container.height / mc.displayHeight - 1;

                    while (clickDelay <= 0L) {
                        Slot slot = accessor.doGetSlotAtPosition(mouseX, mouseY);
                        if (slot != null) {
                            mc.playerController.windowClick(
                                    container.inventorySlots.windowId,
                                    slot.slotNumber,
                                    0, 0, mc.thePlayer);
                        }
                        clickDelay += getNextClickDelay();
                    }
                }
            } else {
                if (clickPending) {
                    clickPending = false;
                    int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
                    KeyBindUtil.setKeyBindState(attackKey, KeyBindUtil.isKeyDown(attackKey));
                }
                if (blockHitPending) {
                    blockHitPending = false;
                    int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
                    KeyBindUtil.setKeyBindState(useKey, KeyBindUtil.isKeyDown(useKey));
                }

                if (!isEnabled() || !canAutoClick()
                        || (requirePress.getValue() && !mc.gameSettings.keyBindAttack.isKeyDown())) {
                    return;
                }

                if (!mc.thePlayer.isUsingItem()) {
                    while (clickDelay <= 0L) {
                        clickPending = true;
                        clickDelay = clickDelay + getNextClickDelay();
                        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                        KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindAttack.getKeyCode());
                    }
                }

                if (!this.blockHit.getValue()
                        || !mc.gameSettings.keyBindUseItem.isKeyDown()
                        || !ItemUtil.isHoldingSword()) {
                    return;
                }

                boolean hurtTimeTooHigh = this.hurtTimeTracker.remainingHurtTicks() > this.blockHitHurtTime.getValue();

                if (!(this.blockHitHoldMillis > 0L && !hurtTimeTooHigh)) {
                    this.blockHitPending = true;
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    if (this.blockHitCycleMillis > 0L || hurtTimeTooHigh) {
                        return;
                    }
                }

                if (this.blockHitHoldMillis > 0L || mc.thePlayer.isUsingItem()) {
                    return;
                }

                this.blockHitHoldMillis += this.getBlockHitHold();
                this.blockHitCycleMillis += this.getBlockHitHold() + 50L + this.getBlockHitDelay();
                KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClick(RightClickMouseEvent event) {
        if (isEnabled() && !event.isCanceled() && ItemUtil.isHoldingSword() && !this.blockHitPending) {
            this.blockHitHoldMillis += this.getBlockHitHold();
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (event.isReceive() && !event.isCanceled()) {
            this.hurtTimeTracker.handlePacket(event.getPacket());
        }
    }

    @Override
    public void onEnabled() {
        clickDelay = 0L;
        blockHitHoldMillis = 0L;
        blockHitCycleMillis = 0L;
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
