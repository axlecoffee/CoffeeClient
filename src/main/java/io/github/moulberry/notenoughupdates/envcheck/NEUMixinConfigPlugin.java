package io.github.moulberry.notenoughupdates.envcheck;

import io.github.moulberry.notenoughupdates.coffeeclient.hook.mixin.CoffeeMixinBootstrap;
import io.github.moulberry.notenoughupdates.miscgui.DynamicLightItemsEditor;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * NEU's mixin config plugin. Delegates CoffeeLoader mod loading
 * to {@link CoffeeMixinBootstrap}.
 */
public class NEUMixinConfigPlugin implements IMixinConfigPlugin {

	static {
		EnvironmentScan.checkEnvironmentOnce();
	}

	@Override
	public void onLoad(String mixinPackage) {
		CoffeeMixinBootstrap.onLoad(mixinPackage);
	}

	@Override
	public List<String> getMixins() {
		return CoffeeMixinBootstrap.getMixins();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if ("io.github.moulberry.notenoughupdates.mixins.MixinOFDynamicLights".equals(mixinClassName)) {
			DynamicLightItemsEditor.setDidApplyMixin(true);
		}
	}
}
