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
