![1761602550045](image/README/1761602550045.png)

# A fully(not) working shim/impl of OpenMyau Client (OpenMyau-based sometime october commit) on NotEnoughUpdates 2.6.0

## I do not condone cracking Myau, however basically the entire codebase was pure-java & no external packages so it was REALLY easy to paste + better then a random Raven version xD

The way it works in lunar is simple
Lunar allows 3rd party mod replacements in their 1.8.9 "forge" version see [here](https://support.lunarclient.com/support/solutions/articles/60000752051-third-party-mods) - if you shim in your own code (in this case what I called CoffeeClient) lunar will still load it since it contains the main NotEnoughUpdates classes

Regarding mixins - as long as you put them in NEU's mixin's dirs and not your own - they hypothetically load? or at least did in my case

For docs on how to use refer to https://support.lunarclient.com/support/solutions/articles/60000752051-third-party-mods
or dm me on discord @axle.coffee
please dont ask me to add x feature - I may add some of the popular Utils but otherwise - you want it you contribute it


This is a direct continuation on both learning java 💀 (I am a go main sorry) and continuing to do bad things with notenoughupdates (my first attempt with docs https://github.com/axlecoffee/notenoughodin )

Credits to https://github.com/sakthic01/OpenMyau for the "cheats" - cba to port more features from it or the full mixin set :shrug:

# I do NOT own/contribute to NEU - all NEU code is still licensed to them don't like misuse it or whatever this is a proof of concept their readme is at -README.md in the root of this project go support & donate to them [patreon/moulberry](https://patreon.com/Moulberry) they do AMAZING work and without NEU being open source - none of this would've been possible!!
