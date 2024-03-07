# Contributing to Pixelitor

Thank you for considering contributing to Pixelitor!

Before you start contributing, please make sure to read and understand the guidelines below.

If you have a new feature, bugfix, or performance optimization in mind, please start by opening an issue in the [issue tracker](https://github.com/lbalazscs/Pixelitor/issues) and discussing your idea. 
This will help to ensure that your contribution aligns with the project goals, avoids duplicated work, and maintains the code quality. 
If you have any questions about the project or how to contribute, feel free to ask in the issue tracker. 
Alternatively, you can also send an email or discuss your idea on [Discord](https://discord.gg/SXaxYnBSTv).

## Focused Changes

Each pull request should focus on addressing a single issue or implementing a specific feature. Avoid making a lot of unrelated changes. Provide a clear and concise description of the problem you're solving or the feature you're implementing.

## Not Recommended: Refactorings, Stylistic Changes, and Unit Tests

If this is your first contribution to Pixelitor, you should probably focus on new features, bugfixes or performance optimizations. 
Refactorings and unit tests are needed, but if you're unfamiliar with the codebase, it's best to avoid them initially.

If you are considering refactorings, it's especially important to discuss them first. 
Just because your IDE can propose many changes, I won't automatically agree with them. 
Also, note that all code outside the "pixelitor" package is essentially library code that, for some reason, can't be pulled from Maven Central.

Don't even think about purely stylistic changes (such as removing "final" from fields or adding "final" to local variables).

If you can add a test without truly understanding the code ("call a setter, then a getter"), then it probably isn't useful. 
By all means, add or update tests if you change the functionality of the code, but think twice before adding low-effort new tests.

## Code Formatting

If you're contributing code, I don't really mind how you format the new code, but please don't change the formatting of the existing code, because this would make it more difficult to see the differences.
If you have a habit of automatically formatting code while you write, you can find the Java formatting rules I use in the "stuff/intellij_formatting_config.xml" file.
You can import this file into IntelliJ Idea to make sure you're using the same formatting rules.

## Rebasing

Before submitting a pull request, always ensure that your changes are based on the newest master version, and rebase them if they aren't.
If your fork says "This branch is x commits ahead of, y commits behind", then it's not properly rebased. It should be ahead, but not behind.



