package io.github.moulberry.notenoughupdates.coffeeclient.feature.player;

import io.github.moulberry.notenoughupdates.coffeeclient.events.LivingUpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.PacketEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.events.UpdateEvent;
import io.github.moulberry.notenoughupdates.coffeeclient.feature.Feature;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.BooleanProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.IntProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.property.properties.ModeProperty;
import io.github.moulberry.notenoughupdates.coffeeclient.util.KeyBindUtil;
import io.github.moulberry.notenoughupdates.coffeeclient.util.PacketUtil;
import io.github.moulberry.notenoughupdates.mixins.IAccessorC0DPacketCloseWindow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InvWalk extends Feature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Queue<C0EPacketClickWindow> clickQueue = new ConcurrentLinkedQueue<>();
    private boolean keysPressed = false;
    private C16PacketClientStatus pendingStatus = null;
    private int delayTicks = 0;
    private int openDelayTicks = -1;
    private int closeDelayTicks = -1;

    private final Map<KeyBinding, Boolean> movementKeys = new HashMap<KeyBinding, Boolean>(8) {{
        put(mc.gameSettings.keyBindForward, false);
        put(mc.gameSettings.keyBindBack, false);
        put(mc.gameSettings.keyBindLeft, false);
        put(mc.gameSettings.keyBindRight, false);
        put(mc.gameSettings.keyBindJump, false);
        put(mc.gameSettings.keyBindSneak, false);
        put(mc.gameSettings.keyBindSprint, false);
    }};

    private boolean clicked = false;
    private long clickTime = 0;
    private long lastPingTime = System.currentTimeMillis();

    public final ModeProperty mode = new ModeProperty("mode", 0,
            new String[]{"VANILLA", "LEGIT", "HYPIXEL", "LEGIT+", "PING"});
    public final BooleanProperty guiEnabled = new BooleanProperty("click-gui", true);
    public final IntProperty openDelay = new IntProperty("open-delay", 0, 0, 20, () -> mode.getValue() == 3);
    public final IntProperty closeDelay = new IntProperty("close-delay", 4, 0, 20, () -> mode.getValue() == 3);
    public final BooleanProperty lockMoveKey = new BooleanProperty("lock-move-key", false);

    public InvWalk() {
        super("InvWalk", false);
    }

    public void pressMovementKeys(boolean skipSneak) {
        this.movementKeys.keySet().stream()
                .filter(key -> !skipSneak || key != mc.gameSettings.keyBindSneak)
                .forEach(key -> KeyBindUtil.updateKeyState(key.getKeyCode()));
        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        this.keysPressed = true;
    }

    public void resetMovementKeys() {
        this.movementKeys.replaceAll((k, v) -> false);
    }

    public boolean isSetMovementKeys() {
        return this.movementKeys.values().stream().anyMatch(Boolean::booleanValue);
    }

    public void storeMovementKeys() {
        this.movementKeys.replaceAll((k, v) -> KeyBindUtil.isKeyDown(k.getKeyCode()));
    }

    public void restoreMovementKeys() {
        for (Map.Entry<KeyBinding, Boolean> entry : movementKeys.entrySet()) {
            KeyBindUtil.setKeyBindState(entry.getKey().getKeyCode(), entry.getValue());
        }
        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        this.keysPressed = true;
    }

    public boolean canInvWalk() {
        if (!(mc.currentScreen instanceof GuiContainer)) return false;
        if (mc.currentScreen instanceof GuiContainerCreative) return false;

        switch (this.mode.getValue()) {
            case 0:
                return true;
            case 1:
                if (!(mc.currentScreen instanceof GuiInventory)) return false;
                return this.pendingStatus != null && this.clickQueue.isEmpty();
            case 2:
                return this.delayTicks == 0 && this.clickQueue.isEmpty();
            case 3:
                if (!(mc.currentScreen instanceof GuiInventory)) return false;
                return this.closeDelayTicks == -1 && this.clickQueue.isEmpty();
            case 4:
                return true;
            default:
                return false;
        }
    }

    public boolean temporaryStackIsEmpty() {
        if (mc.thePlayer.inventory.getItemStack() != null) return false;
        if (mc.thePlayer.inventoryContainer instanceof ContainerPlayer) {
            ContainerPlayer containerPlayer = (ContainerPlayer) mc.thePlayer.inventoryContainer;
            for (int i = 0; i < containerPlayer.craftMatrix.getSizeInventory(); i++) {
                ItemStack stack = containerPlayer.craftMatrix.getStackInSlot(i);
                if (stack != null) {
                    return false;
                }
            }
        }
        return true;
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled()) return;

        if (this.openDelayTicks >= 0) {
            this.openDelayTicks--;
            return;
        }

        while (!this.clickQueue.isEmpty()) {
            PacketUtil.sendPacketNoEvent(this.clickQueue.poll());
        }

        if (this.closeDelayTicks > 0) {
            if (this.temporaryStackIsEmpty()) {
                this.closeDelayTicks--;
            }
        } else if (this.closeDelayTicks == 0) {
            if (mc.currentScreen instanceof GuiInventory) {
                PacketUtil.sendPacketNoEvent(new C0DPacketCloseWindow(0));
            }
            this.closeDelayTicks = -1;
        }
    }

    @SubscribeEvent
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null || !event.isPre()) return;

        if (this.mode.getValue() == 4) {
            handlePingMode();
            return;
        }

        if (this.canInvWalk()) {
            if (this.isSetMovementKeys() && this.lockMoveKey.getValue()) {
                this.restoreMovementKeys();
            } else {
                this.pressMovementKeys(true);
            }
        } else {
            if (this.keysPressed) {
                if (mc.currentScreen != null) {
                    KeyBinding.unPressAllKeys();
                } else if (this.isSetMovementKeys()) {
                    this.resetMovementKeys();
                    this.pressMovementKeys(false);
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

    private void handlePingMode() {
        if (!(mc.currentScreen instanceof GuiContainer) || mc.currentScreen instanceof GuiContainerCreative) {
            this.clicked = false;
            if (this.keysPressed) {
                this.keysPressed = false;
            }
            return;
        }

        long now = System.currentTimeMillis();
        long sincePing = now - this.lastPingTime;

        if ((!this.clicked && sincePing < 125) || (now > this.clickTime + 325 + sincePing)) {
            
            for (KeyBinding key : movementKeys.keySet()) {
                boolean down = KeyBindUtil.isKeyDown(key.getKeyCode());
                if (down) {
                    KeyBindUtil.setKeyBindState(key.getKeyCode(), true);
                }
            }
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
            this.keysPressed = true;
        } else {
            
            for (KeyBinding key : movementKeys.keySet()) {
                KeyBindUtil.setKeyBindState(key.getKeyCode(), false);
            }
            this.keysPressed = true;
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (!isEnabled()) return;

        if (event.isReceive()) {
            if (this.mode.getValue() == 4) {
                if (event.getPacket() instanceof S00PacketKeepAlive) {
                    this.lastPingTime = System.currentTimeMillis();
                } else if (event.getPacket() instanceof S2DPacketOpenWindow) {
                    this.clicked = false;
                    for (KeyBinding key : movementKeys.keySet()) {
                        KeyBindUtil.setKeyBindState(key.getKeyCode(), KeyBindUtil.isKeyDown(key.getKeyCode()));
                    }
                }
            }
            return;
        }

        if (!event.isSend()) return;

        if (event.getPacket() instanceof C16PacketClientStatus) {
            this.storeMovementKeys();
            if (this.mode.getValue() == 1 || this.mode.getValue() == 3) {
                C16PacketClientStatus packet = (C16PacketClientStatus) event.getPacket();
                if (packet.getStatus() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    event.setCanceled(true);
                    if (this.mode.getValue() == 1) {
                        this.pendingStatus = packet;
                    }
                }
            }
            return;
        }

        if (event.getPacket() instanceof C0DPacketCloseWindow) {
            C0DPacketCloseWindow packet = (C0DPacketCloseWindow) event.getPacket();
            int windowId = ((IAccessorC0DPacketCloseWindow) packet).getWindowId();

            if (windowId == 0) {
                if (this.mode.getValue() == 3) {
                    if (!this.clickQueue.isEmpty()) this.clickQueue.clear();
                    if (this.openDelayTicks >= 0) this.openDelayTicks = -1;
                    if (this.closeDelayTicks >= 0) {
                        this.closeDelayTicks = -1;
                    } else {
                        event.setCanceled(true);
                    }
                } else if (this.pendingStatus != null) {
                    this.pendingStatus = null;
                    event.setCanceled(true);
                }
            } else {
                
                if (!this.clickQueue.isEmpty()) this.clickQueue.clear();
                if (this.openDelayTicks >= 0) this.openDelayTicks = -1;
                if (this.closeDelayTicks >= 0) this.closeDelayTicks = -1;
            }
            return;
        }

        if (event.getPacket() instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow packet = (C0EPacketClickWindow) event.getPacket();

            if (this.mode.getValue() == 4) {
                this.clicked = true;
                this.clickTime = System.currentTimeMillis();
                for (KeyBinding key : movementKeys.keySet()) {
                    KeyBindUtil.setKeyBindState(key.getKeyCode(), false);
                }
                return;
            }

            switch (this.mode.getValue()) {
                case 1:
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
                case 2:
                    if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                        event.setCanceled(true);
                    } else {
                        KeyBinding.unPressAllKeys();
                        event.setCanceled(true);
                        this.clickQueue.offer(packet);
                        this.delayTicks = 8;
                    }
                    break;
                case 3:
                    if (packet.getWindowId() == 0) {
                        if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                            event.setCanceled(true);
                            return;
                        }
                        KeyBinding.unPressAllKeys();
                        event.setCanceled(true);
                        this.clickQueue.offer(packet);
                        if (this.closeDelayTicks < 0 && this.openDelayTicks < 0) {
                            this.pendingStatus = new C16PacketClientStatus(EnumState.OPEN_INVENTORY_ACHIEVEMENT);
                            this.openDelayTicks = openDelay.getValue();
                        }
                        this.closeDelayTicks = closeDelay.getValue();
                    }
                    break;
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
        this.openDelayTicks = -1;
        this.closeDelayTicks = -1;
        this.clickQueue.clear();
        this.clicked = false;
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString().toUpperCase();
        return new String[]{modeName};
    }
}
