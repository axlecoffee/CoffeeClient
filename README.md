# CoffeeClient

![1779336918018](image/README/1779336918018.png)

 *A non injection-based Lunar Client QOL Client*

The way it works in Lunar is simple. Lunar allows 3rd party mod replacements in their "1.8.9 Forge" version. If you compile a 3rd party mod with your own code (shimming) Lunar still loads it since it contains the core classes.

For documentation of how this works, I suggest reading the archived copy of Lunar Client's documentation, located [here](https://web.archive.org/web/20260221030830/https://support.lunarclient.com/support/solutions/articles/60000752051-third-party-mods) or dm me on discord. `@axle.coffee`

## Community

We have a discord! [located here](https://discord.gg/4umJGqwftc)

## Recent Changes

Recently, Lunar Client removed NotEnoughUpdates (as it was EOL and SkyBlock has blocked 1.8.9 for months now) - this means I had to rewrite everything to use a different host, my choice, replaymod.

However, due to the fact it makes 0 sense to keep writing code into an already heavy codebase, CoffeeClient will now use its own mod loader, making replaymod simply a loader of CoffeeClient, rather than containing coffeeclient itself

### Mixins

As long as you compile mixins to the FQDN of whatever mod you're cosplaying as (in this case, replaymod, or formerly, NotEnoughUpdates) example:

```kotlin
    // Relocate mixin classes into replaymod's mixin package so the framework's package prefix prepend produces the correct FQCN at runtime.
    relocate("$baseGroup.mixin", "com.replaymod.replay.mixin.$modid")

    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
```

you can essentially have full mixin support! **HOWEVER** Lunar Client runs their Forge 1.8.9 instance on **Java 17** meaning if you try to use specific java 8 methods, they likely may not work. this goes for mixins and normal "mod" code

### Modding

I have a simple init system for "CoffeeMods"

```java
@CoffeeMod(name = "ExampleMod", version = "1.0.0") //basically forge info
public class ExampleMod {

    @CoffeeMod.EventHandler
// LunarClient's mixin init event, only place you can probably tamper with Lunar itself, if you wanted to do that, since even "Lunar" isn't fully loaded
    public void onMixinInit(CLMixinInitEvent event) {
        Logger.info("[ExampleMod] Mixin init ----- "
                + event.getLoadedMods() + " mod(s), "
                + event.getRegisteredMixins() + " mixin(s)");
    }

    @CoffeeMod.EventHandler
// I think this was renamed to earlyinit?
// This is the exact static hook Lunar Client calls to invoke whatever mod you've shimmed CoffeeClient's loader to - Lunar client doesnt risk loading a mod via normal forge, so we actually gain an entry point
    public void onNEUInit(CLNEUInitEvent event) {
        Logger.info("[ExampleMod] Early init");
    }

    @CoffeeMod.EventHandler
// Forge PreInit - parsed by Lunar
    public void onPreInit(CLPreInitEvent event) {
        Logger.info("[ExampleMod] Pre-init");
    }

    @CoffeeMod.EventHandler
// same as above
    public void onInit(CLInitEvent event) {
        Logger.info("[ExampleMod] Init");
        Logger.info("[ExampleMod] MC version: " + Minecraft.getMinecraft().getVersion());
    }

    @CoffeeMod.EventHandler
// same as above
    public void onPostInit(CLPostInitEvent event) {
        Logger.info("[ExampleMod] Post-init --- meowwww");
    }
}
```
