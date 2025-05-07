# Translating the Pixelitor user interface

- The translation files are in the [resources](src/main/resources) directory. These are text files with `key=value` pairs, where the value is the translated text.
- Each language needs a file named texts_<a href="https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">lang-code</a>.properties.
- The existing files still have English text if they’re not fully translated. Feel free to fix or complete them.
- A lot of text is still hard-coded (not loaded from the translation files). If you’d like to translate some of it, open an issue and I’ll add it to the files. Alternatively, if you know Java, you can create a PR that extracts more text into the translation files.
- If you’re adding a new language, start by copying the English file (the one without a language code).
- The text for the "Tip of the Day" comes from `tips.properties` in the same directory and can be translated in the same way.
- The color chooser's translation files are in [com/bric/swing/resources](src/main/resources/com/bric/swing/resources). They also work the same way.
- Translating everything is a big task. It’s totally fine if you just add a few translations.
 
 