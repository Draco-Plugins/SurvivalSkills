## Best practices

- **Records**: For classes primarily intended to store data (e.g., DTOs, immutable data structures), **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Declaration**: Always use the exact type declaration required (i.e. SkillCategory...)
- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing. Employ method references (e.g., `stream.map(Foo::toBar)`).
- **Null Handling**: Avoid returning or accepting `null`. Use `Optional<T>` for possibly-absent values and `Objects` utility methods like `equals()` and `requireNonNull()`.

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
