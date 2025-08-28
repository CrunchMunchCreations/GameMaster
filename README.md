# GameMaster
A central Minecraft minigame management system as used in Chrunchy Christmas, for being used
in other projects without requiring to copy the entire codebase of Chrunchy's central management system.

## Why is this needed?
Contrary to most minigame events, our events are built on Fabric, typically using the latest versions of Minecraft.
This allows us a significant amount of control over our minigames without sacrificing on
performance, but this also means we require to reimplement everything by ourselves, as most utility
plugins used in Minecraft events, such as holograms, leaderboards, custom scoreboards, among other things,
are typically not available (or updated) on Fabric. We do not have to worry about NPCs, as we maintain
[a fork of Taterzens](https://github.com/CrunchMunchCreations/Taterzens) to ensure its availability for
the latest version, while also contributing upstream to ensure others are also able to use it.

## How do I use it?
At the moment, GameMaster doesn't actually *have* a stable API, and still receives frequent changes
to the codebase, as it is made a submodule within Chrunchy's central management system.
As such, it might be easier for you to do the same for now, until GameMaster is published onto a Maven
repository.