![1761602550045](image/README/1761602550045.png)

# CoffeeClient - A non-injection-based Lunar Client QOL Client

The way it works in lunar is simple
Lunar allows 3rd party mod replacements in their 1.8.9 "forge" version see [here](https://support.lunarclient.com/support/solutions/articles/60000752051-third-party-mods) - if you shim in your own code (in this case what I called CoffeeClient) lunar will still load it since it contains the main NotEnoughUpdates classes

Regarding mixins - as long as you put them in NEU's mixin's dirs and not your own - they hypothetically load? or at least did in my case

For docs on how to use refer to https://support.lunarclient.com/support/solutions/articles/60000752051-third-party-mods
or dm me on discord @axle.coffee

## Features

### Combat
- AimAssist
- AntiFireball
- AutoClicker
- KillAura
- NoHitDelay
- Velocity
- WTap

### Player
- AutoTool
- Eagle
- FastPlace
- InvWalk
- NoJumpDelay

### Render
- AntiDebuff
- AntiObfuscate
- BedESP
- Bedplates
- Chams
- ESP
- FullBright
- HUD
- Indicators
- ItemESP
- NameTags
- Trajectories

### World
- BedTracker

### Misc
- Test
- The worst visuals you have ever seen

## Modding API

Currently, the modding API is in a very rough implimentation - the best example is a [Session Login Mod](https://github.com/axlecoffee/coffeeclient-coffeeauth) that I made with it - this is full forge meaning essentially with barely any modification you can load any**** mod

*some features don't exist in the JVM so be prepared to fight claude*

## Support + Beta Releases + Bugs + Suggestions

We have a discord! [click here](https://discord.gg/4umJGqwftc) to join and suggest features and report bugs!

# I do NOT own/contribute to NEU - all NEU code is still licensed to them don't like misuse it or whatever this is a proof of concept their readme is at -README.md in the root of this project go support & donate to them [patreon/moulberry](https://patreon.com/Moulberry) they do AMAZING work and without NEU being open source - none of this would've been possible!!
