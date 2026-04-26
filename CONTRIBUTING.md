# Contributing

This is an open source project. We appreciate any help from the community to improve the mod.

## Bugs or ideas for new items

Did you found a bug or do you have an idea how to improve the mod? We are happy to hear from you.

- Report issues or propose features through the [GitHub issue tracker](https://github.com/age-series/ElectricalAge/issues).
- Chat with the team in the [Discord server](https://discord.gg/YjK2JAD) if you want help reproducing or validating an idea before opening a pull request.
- Pull requests are reviewed via GitHub—please include links to any relevant issues or design discussions.

## Building from source and adding new features

If you would like to test changes locally, you can build and run the mod with Gradle:

```bash
git clone https://github.com/age-series/ElectricalAge.git
cd ElectricalAge
./gradlew setupDecompWorkspace
./gradlew runClient
```

`setupDecompWorkspace` only needs to be executed the first time (or after updating Forge/Minecraft dependencies). After that you can iterate using `./gradlew runClient` or other Gradle tasks as needed.

## Translations

Is the mod not available in your language or are some translations missing? You can change that by editing the localization files under [`src/main/resources/assets/eln/lang`](src/main/resources/assets/eln/lang) or by following any language-specific notes on the [GitHub wiki](https://github.com/age-series/ElectricalAge/wiki/Translations).

Some translation strings might contain placeholders for runtime arguments in order to include numbers or other runtime objects into the sentence. These are identified by **%N$** whereas *N* is the number of the argument in the argument list (at runtime). A translation string should include these placeholders too at the appropriate position in the text.

### i18n Guidelines for Contributors

When adding new items or user-facing strings, follow these rules to keep translations working:

1. **Wrap strings with `tr()` or `TR_NAME()`** — Never hardcode user-visible text directly. Use the project's `tr()` (for translatable strings) or `TR_NAME()` (for item/block names) wrappers so the string goes through the localization system.

2. **Avoid dynamically building strings** — Keep translatable strings as complete sentences or phrases rather than concatenating parts at runtime. This makes them easier to translate and avoids word-order issues across languages.

3. **Use positional parameters (`%N$`)** — When a string needs runtime values, use the `%1$`, `%2$`, … form (e.g. `"Temperature: %1$.1f °C"`) instead of embedding variables directly with `+` or string interpolation. This allows translators to reorder arguments as needed for their language.

4. **Run `./gradlew generateLangFiles` after changes** — If your contribution adds new items or new translatable strings, execute this task once you are done to regenerate the language files. Do **not** manually edit the language files unless there is a specific reason (e.g. fixing an existing incorrect translation).
