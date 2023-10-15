# Translating the Pixelitor user interface

- The translation files are in the [resources](src/main/resources) directory. They are text files with key=value pairs, where the value is the translated text.
- For each language there has to be a translation file named
  texts_<a href="https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">lang-code</a>.properties.
- The files might contain English text if they are not fully translated - feel free to correct them.
- A lot of text is still hard-coded (doesn't come from the translation files), let me know if you want to translate some
  of it (open an issue), and I will add it to the translation files
- If you create a new file for a new language, start by making a copy of the English one (the one without a language code).
- The text for the "Tip of the Day" is coming from tips.properties in the same directory, it could be translated
  similarly.
- The color chooser's translation files are in [com/bric/swing/resources](src/main/resources/com/bric/swing/resources),
  they have the same structure.
- Translating every text in the user interface is a big task. It's OK if you contribute only a few translations.
 
 