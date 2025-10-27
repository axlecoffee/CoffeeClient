package io.github.moulberry.notenoughupdates.coffeeclient.module.modules;

import io.github.moulberry.notenoughupdates.coffeeclient.events.LivingUpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.PacketEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.UpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.module.Module;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.ModeProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.KeyBindUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.PacketUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InvWalkModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Queue<C0EPacketClickWindow> clickQueue = new ConcurrentLinkedQueue<>();
    private boolean keysPressed = false;
    private C16PacketClientStatus pendingStatus = null;
    private int delayTicks = 0;

    public final ModeProperty mode = new ModeProperty("mode", 1, new String[] { "VANILLA", "LEGIT", "HYPIXEL" });
    public final BooleanProperty guiEnabled = new BooleanProperty("ClickGUI", true);

    public InvWalkModule() {
        super("InvWalk", false);
    }

    public void pressMovementKeys() {
        KeyBinding[] movementKeys = new KeyBinding[] {
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };
        for (KeyBinding keyBinding : movementKeys) {
            KeyBindUtil.updateKeyState(keyBinding.getKeyCode());
        }
        // Check if sprint module exists (simplified approach)
        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        this.keysPressed = true;
    }

    public boolean canInvWalk() {
        if (!(mc.currentScreen instanceof GuiContainer))
            return false;
        if (mc.currentScreen instanceof GuiContainerCreative)
            return false;

        switch (this.mode.getValue()) {
            case 1: // Vanilla
                if (!(mc.currentScreen instanceof GuiInventory))
                    return false;
                return this.pendingStatus != null && this.clickQueue.isEmpty();
            case 2: // Legit
                return this.clickQueue.isEmpty();
            default: // Hypixel
                return true;
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled()) {
            return;
        }

        // Process click queue (equivalent to original's onTick)
        while (!this.clickQueue.isEmpty()) {
            PacketUtil.sendPacketNoEvent(this.clickQueue.poll());
        }
    }

    @SubscribeEvent
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.isPre()) {
            // Handle ClickGUI (simplified - no actual ClickGUI in CoffeeClient yet)
            // if (mc.currentScreen instanceof ClickGui && this.guiEnabled.getValue()) {
            // pressMovementKeys();
            // return;
            // }

            if (this.canInvWalk() && this.delayTicks == 0) {
                this.pressMovementKeys();
            } else {
                if (this.keysPressed) {
                    if (mc.currentScreen != null) {
                        KeyBinding.unPressAllKeys();
                    }
                    this.keysPressed = false;
                }
                if (this.pendingStatus != null) {
                    PacketUtil.sendPacketNoEvent(this.pendingStatus);
                    this.pendingStatus = null;
                }
                if (this.delayTicks > 0) {
                    this.delayTicks--;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || !event.isSend())
            return;

        if (event.getPacket() instanceof C16PacketClientStatus) {
            if (this.mode.getValue() == 1) {
                C16PacketClientStatus packet = (C16PacketClientStatus) event.getPacket();
                if (packet.getStatus() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    event.setCanceled(true);
                    this.pendingStatus = packet;
                }
            }
        } else if (event.getPacket() instanceof C0DPacketCloseWindow) {
            C0DPacketCloseWindow packet = (C0DPacketCloseWindow) event.getPacket();
            // Use reflection to access windowId since mixin might not be available in
            // CoffeeClient
            try {
                java.lang.reflect.Field windowIdField = packet.getClass().getDeclaredField("windowId");
                windowIdField.setAccessible(true);
                int windowId = windowIdField.getInt(packet);
                if (this.pendingStatus != null && windowId == 0) {
                    this.pendingStatus = null;
                    event.setCanceled(true);
                }
            } catch (Exception e) {
                // Fallback: just handle any close window if we have a pending status
                if (this.pendingStatus != null) {
                    this.pendingStatus = null;
                    event.setCanceled(true);
                }
            }
        } else if (event.getPacket() instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow packet = (C0EPacketClickWindow) event.getPacket();
            switch (this.mode.getValue()) {
                case 1: // Vanilla
                    if (packet.getWindowId() == 0) {
                        if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                            event.setCanceled(true);
                            return;
                        }
                        if (this.pendingStatus != null) {
                            KeyBinding.unPressAllKeys();
                            event.setCanceled(true);
                            this.clickQueue.offer(packet);
                        }
                    }
                    break;
                case 2: // Legit
                    if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                        event.setCanceled(true);
                    } else {
                        KeyBinding.unPressAllKeys();
                        event.setCanceled(true);
                        this.clickQueue.offer(packet);
                        this.delayTicks = 8;
                    }
                    break;
                // Hypixel mode doesn't cancel packets
            }
            if (this.pendingStatus != null) {
                PacketUtil.sendPacketNoEvent(this.pendingStatus);
                this.pendingStatus = null;
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.keysPressed) {
            if (mc.currentScreen != null) {
                KeyBinding.unPressAllKeys();
            }
            this.keysPressed = false;
        }
        if (this.pendingStatus != null) {
            PacketUtil.sendPacketNoEvent(this.pendingStatus);
            this.pendingStatus = null;
        }
        this.delayTicks = 0;
        this.clickQueue.clear();
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString().toUpperCase();
        return new String[] { modeName };
    }
}
