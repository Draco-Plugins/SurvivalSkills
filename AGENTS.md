## Best practices

- **Records**: For classes primarily intended to store data (e.g., DTOs, immutable data structures), **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Declaration**: Always use the exact type declaration required (i.e. SkillCategory...)
- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing. Employ method references (e.g., `stream.map(Foo::toBar)`) only when they do not produce null-type-safety warnings.
- **Null Handling**: Avoid returning or accepting `null`. Use `Optional<T>` for possibly-absent values and `Objects` utility methods like `equals()` and `requireNonNull()`.
- **Null-Safe Functional Expressions**: Do not use an unbound instance method reference such as `Type::method` when Eclipse null analysis requires its receiver to be `@Nonnull` but the functional interface parameter has unspecified nullness. Use an explicitly typed lambda instead (e.g., `(TrophyType trophyType) -> trophyType.getName()` or `(TrophyEffects trophyEffects) -> trophyEffects.checkForPlayers()`). Apply this rule to stream collectors, `Optional.ifPresent`, and other generic functional APIs whenever the method reference would require an unchecked null conversion.
- **Reading Files**: Always read the full file to avoid missing important context or making tool calls several times to read the same file

### Naming Conventions

- `UpperCamelCase` for class and interface names.
- `lowerCamelCase` for method and variable names.
- `UPPER_SNAKE_CASE` for constants.
- `lowercase` for package names.
- Use nouns for classes (`UserService`) and verbs for methods (`getUserById`).
- Avoid abbreviations and Hungarian notation.

### Logging

- Logs should only be used when things go wrong. Success paths should not have log messages
- For all Minecraft plugin development use the format `Bukkit.getLogger().log(Level.<LEVEL>, "[SurvivalSkills] <message>", <exception>)`.
- Use appropriate log levels (`INFO`, `WARNING`, `SEVERE`, etc.) and avoid logging sensitive information.
- If messages contain variables use String.format() for better readability and performance.

### Bug Patterns

| Description                                                 | Example / Notes                                                        |
| ----------------------------------------------------------- | ---------------------------------------------------------------------- |
| Resources should be closed                                  | Use try-with-resources when working with streams, files, sockets, etc. |
| Objects should be compared with `.equals()` instead of `==` | Especially important for Strings and boxed primitives.                 |
| Redundant casts should be removed                           | Clean up unnecessary or unsafe casts.                                  |
| Conditions should not always evaluate to true or false      | Watch for infinite loops or if-conditions that never change.           |
| Unreachable code should be removed                          | Code after `return`, `throw`, etc., must be cleaned up.                |

## Code Smells

| Description                                     | Example / Notes                                                |
| ----------------------------------------------- | -------------------------------------------------------------- |
| Methods should not have too many parameters     | Refactor into helper classes or use builder pattern.           |
| Duplicated blocks of code should be removed     | Consolidate logic into shared methods.                         |
| Methods should not be too long                  | Break complex logic into smaller, testable units.              |
| Cognitive complexity should be reduced          | Simplify nested logic, extract methods, avoid deep `if` trees. |
| String literals should not be duplicated        | Replace with constants or enums.                               |
| Unused assignments should be removed            | Avoid dead variables—remove or refactor.                       |
| Magic numbers should be replaced with constants | Improves readability and maintainability.                      |
| Catch blocks should not be empty                | Always log or handle exceptions meaningfully.                  |

## Repo Level Conventions

1. When adding a new skill reward, be sure to add it at the appropriate level to config.yml, then ensure that the version number is incremented in pom.xml and FileUtils.java so that the config is picked up by servers running the plugin
2. When creating a new item, it is important that the item itself is added appropriately. Whether this is a custom runnable class, being added to a list of other items, or Bukkit Listener logic. Then the item should be added to the SurvivalSkillsGetCommand.java and the TabCompleter.java classes.
