# Agent Notes

- The javac deprecation/unchecked warnings reported from `build/rfg/minecraft-src/java`
  were already investigated. They come from generated/decompiled patched
  Minecraft/Forge sources, not Electrical Age source. Running
  `compilePatchedMcJava` with `-Xlint:deprecation` and `-Xlint:unchecked` shows
  details such as deprecated Forge progress APIs and raw collection usage in
  Minecraft classes. There is not a meaningful durable fix in this repository
  because `build/rfg/minecraft-src` is generated external source; suppress,
  isolate, or leave diagnostic lint opt-in rather than trying to patch those
  generated files.
