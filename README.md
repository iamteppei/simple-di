# simple-di

A lightweight, modular dependency injection container for Java.

[![CI](https://github.com/iamteppei/simple-di/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/iamteppei/simple-di/actions/workflows/ci-cd.yml)
[![Java](https://img.shields.io/badge/java-21-blue)](#requirements)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

## Why simple-di

- Small surface area and straightforward API.
- Modular architecture with a clear split between API and implementation.
- Designed for predictable lifecycle management and testability.

## Project Status

- Active prototype / proof-of-concept.
- API and internals may still evolve.
- Feedback and contributions are welcome.

## Project Structure

- `simple-di-api`: Public DI contracts and interfaces.
- `simple-di-core`: Container implementation, module loader, bootstrap starter, and tests.

## Requirements

- JDK 21
- Gradle Wrapper (included)

This project uses Java toolchains and can auto-download missing JDKs (configured in `gradle.properties`).

## Build and Test

Build all modules:

```bash
./gradlew build
```

Run core tests:

```bash
./gradlew :simple-di-core:test
```

Generate JaCoCo coverage report:

```bash
./gradlew :simple-di-core:jacocoTestReport
```

Coverage reports:

- `simple-di-core/build/reports/jacoco/test/html/index.html`
- `simple-di-core/build/reports/jacoco/test/jacocoTestReport.xml`

## Quick Start

Typical bootstrap flow:

```java
import io.abc.platform.container.pure.impl.ContainerRunner;
import io.abc.shared.di.AppContext;

public class Main {
    public static void main(String[] args) {
        AppContext context = ContainerRunner.start(args);
        // Resolve and use your beans from context.
    }
}
```

### Quick Start with ServiceLoader Modules

You can register modules using Java ServiceLoader so they are discovered automatically by `ContainerRunner.start(...)`.

1. Create a module implementation:

```java
package com.example;

import io.abc.shared.di.ConfigurableContext;
import io.abc.shared.di.ConfigurableModule;

public class GreetingModule implements ConfigurableModule {
    @Override
    public void configure(ConfigurableContext context) {
        context.bind(GreetingService.class, DefaultGreetingService.class, "greetingService");
    }
}
```

2. Add service registration file:

`src/main/resources/META-INF/services/io.abc.shared.di.ConfigurableModule`

with content:

```text
com.example.GreetingModule
```

3. Start the container:

```java
import io.abc.platform.container.pure.impl.ContainerRunner;
import io.abc.shared.di.AppContext;

public class Main {
    public static void main(String[] args) {
        AppContext context = ContainerRunner.start(args);
        // GreetingModule is auto-loaded from META-INF/services
    }
}
```

## Configuration and Conventions

Root Gradle and project configuration lives in:

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`

Core logging stack:

- API: `org.slf4j:slf4j-api`
- Backend: `ch.qos.logback:logback-classic`
- Config: `simple-di-core/src/main/resources/logback.xml`

## Roadmap

- Stabilize public API for a first tagged release.
- Expand usage examples and guides.
- Add compatibility matrix and benchmark notes.
- Improve publishing and release automation.

## Contributing

Contributions are welcome.

Suggested workflow:

1. Fork and create a feature branch.
2. Make focused changes with tests.
3. Run `./gradlew build` locally.
4. Open a pull request with a clear description.

When adding or changing behavior, include or update tests in `simple-di-core/src/test/java`.

## Code of Conduct

Please be respectful and constructive in all interactions.

See `CODE_OF_CONDUCT.md` for details.

## Security

If you discover a vulnerability, please report it privately to the maintainers instead of opening a public issue.

See `SECURITY.md` for reporting guidance.

## License

This project is licensed under the Apache License 2.0.

See `LICENSE` for details.
