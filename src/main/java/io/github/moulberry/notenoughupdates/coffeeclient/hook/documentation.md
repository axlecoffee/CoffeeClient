# CoffeeLoader — Mod Loading Documentation

## Overview

CoffeeLoader loads external mods at runtime on Lunar Client's Ichor platform.
Mods are discovered from the `coffeeloader/mods/` directory relative to the game
working directory.

## JAR Naming

Mod JARs must use the `.coffeeclient.jar` extension.

```
mymod-1.0.0.coffeeclient.jar
```

Files without this extension are ignored.

## @CoffeeMod Annotation

The main mod class must be annotated with `@CoffeeMod`.

```java
package com.example.mymod;

import io.github.moulberry.notenoughupdates.coffeeclient.hook.CoffeeMod;

@CoffeeMod(name = "MyMod", version = "1.0.0")
public class MyMod {
}
```

The annotation is discovered via ASM bytecode scanning. The class is not loaded
until the NEU constructor phase.

### Fallback

If no `@CoffeeMod` annotation is found, the loader checks the JAR manifest for
a `CoffeeMod-Entry` attribute pointing to the main class.

## Lifecycle Events

Register event handlers with `@CoffeeMod.EventHandler`. The method must accept
exactly one parameter of the event type.

```java
@CoffeeMod(name = "MyMod", version = "1.0.0")
public class MyMod {

    @CoffeeMod.EventHandler
    public void onMixinInit(CLMixinInitEvent event) {
        // Fired after mod instantiation. Contains loaded mod count
        // and registered mixin count.
    }

    @CoffeeMod.EventHandler
    public void onNEUInit(CLNEUInitEvent event) {
        // Fired during NEU constructor, after mixin init event.
    }

    @CoffeeMod.EventHandler
    public void onPreInit(CLPreInitEvent event) {
        // Fired during FMLPreInitializationEvent, before CoffeeClient
        // internal modules initialize.
    }

    @CoffeeMod.EventHandler
    public void onInit(CLInitEvent event) {
        // Fired during FMLInitializationEvent.
    }

    @CoffeeMod.EventHandler
    public void onPostInit(CLPostInitEvent event) {
        // Fired during FMLPostInitializationEvent.
    }
}
```

Event classes are in `io.github.moulberry.notenoughupdates.coffeeclient.hook.event`.

## Mixin Support

External mods can use Mixins. This requires shadow relocation of the mixin
package into NEU's mixin namespace.

### Mixin Config

Create a standard mixin config JSON (e.g. `mixins.mymod.json`). The `package`
field must point to the relocated package.

```json
{
  "required": true,
  "package": "io.github.moulberry.notenoughupdates.mixins.mymod",
  "compatibilityLevel": "JAVA_8",
  "client": [
    "MixinFoo"
  ]
}
```

### Shadow Relocation

In your `build.gradle.kts`, relocate your mixin package into NEU's namespace:

```kotlin
tasks.shadowJar {
    relocate("com.example.mymod.mixin", "io.github.moulberry.notenoughupdates.mixins.mymod")
}
```

The loader discovers mixin configs by checking the `MixinConfigs` manifest
attribute first, then falling back to scanning for `mixins.*.json` files at the
JAR root.

### MixinExtras

`com.llamalad7.mixinextras` is available on the Ichor classpath. Use it as
`compileOnly` in your build. Annotations like `@ModifyExpressionValue` and
`@WrapOperation` work.

## Build Setup

Mod JARs are fat JARs built with the Shadow plugin.

```kotlin
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    compileOnly("io.github.moulberry:NotEnoughUpdates:2.4.0") // or local
    compileOnly("com.llamalad7:mixinextras-common:0.3.5")
}

tasks.shadowJar {
    archiveClassifier.set("coffeeclient")
    // produces: mymod-1.0.0-coffeeclient.jar
    // rename to: mymod-1.0.0.coffeeclient.jar

    relocate("com.example.mymod.mixin",
             "io.github.moulberry.notenoughupdates.mixins.mymod")
}
```

The output JAR must end with `.coffeeclient.jar` and be placed in
`coffeeloader/mods/` in the game directory.

## Logging

CoffeeLoader logs to `coffeeloader/coffee-loader.log` and stdout.
The `DO_LOGGING` flag in `CoffeeLoader.java` toggles all output.

## Directory Structure

```
<game dir>/
  coffeeloader/
    mods/
      mymod-1.0.0.coffeeclient.jar
    coffee-loader.log
```

## Source Layout

```
coffeeclient/
  CoffeeClient.java       -- Internal client (modules, commands)
  hook/
    CoffeeMod.java         -- @CoffeeMod annotation
    CoffeeLoader.java      -- Main orchestrator
    documentation.md       -- This file
    event/
      CLMixinInitEvent.java
      CLNEUInitEvent.java
      CLPreInitEvent.java
      CLInitEvent.java
      CLPostInitEvent.java
    mixin/
      CoffeeMixinBootstrap.java  -- JAR injection, annotation scanning, mixin collection
    util/
      Logger.java                -- File + stdout logger
```
