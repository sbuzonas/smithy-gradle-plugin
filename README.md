# Smithy Gradle Plugin

This project integrates Smithy with Gradle. This plugin can build artifacts
from Smithy models, generate JARs that contain Smithy models found in Java
projects, and generate JARs that contain filtered *projections* of Smithy
models.


## Installation

The Smithy Gradle plugin is applied using the `software.amazon.smithy` plugin.
The following example configures a project to use the Smithy Gradle plugin:

```kotlin
plugins {
    java
    id("software.amazon.smithy").version("0.0.1")
}
```


## Smithy model sources

When a JAR is generated for a project, any Smithy models found in the
following directories will be added to the JAR:

- `model/`
- `src/main/smithy`
- `src/main/resources/META-INF/smithy`

Models found in these directories are combined into a single directory
and used to validate and build the Smithy model. A Smithy manifest file
is automatically created for the detected models, and it along with the
model files, are placed in the `META-INF/smithy/` resource of the created
JAR. Any project that then depends on this created JAR will be able to find
and use the Smithy models contained in the JAR when using *model discovery*.


## Building Smithy models

This plugins operates in two different modes. If no `projection` is specified
for the `SmithyExtension`, then the plugin runs a "source" build using the
"source" projection. If a `projection` is specified for the `SmithyExtension`,
then the plugin runs in a "projection" mode.


### Building a source model

A "source" build is run when no `projection` is configured in
`SmithyExtension`. Because no projection was specified, **Smithy-build** is
executed using the `compileClasspath` plus the `buildscript` classpath. To
prevent accidentally relying on Smithy models that are only available to
build scripts, Smithy models are discovered using only the `compileClasspath`.

The following example `build.gradle.kts` will build a Smithy model using a
"source" build:

```kotlin
plugins {
    java
    id("software.amazon.smithy").version("0.0.1")
}

// The SmithyExtension is used to customize the build. This example
// doesn't set any values and can be completely omitted.
configure<software.amazon.smithy.gradle.SmithyExtension> {}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // These are just examples of dependencies. This model has a dependency on
    // a "common" model package and uses the external AWS traits.
    api("com.foo.baz:foo-model-internal-common:1.0.0")
    api("software.amazon.smithy:smithy-aws-traits:0.4.0")
}
```


### Generating a projection

A "projection" build is run when a `projection` is specified in the
`SmithyExtension`. You might want to create a projection of a model if you
need to maintain an internal version of a model that contains more information
and features than an external version of a model published to your customers.

A "projection" build is executed using only the `buildscript` classpath, and
Smithy models are discovered using only the `buildscript` classpath. This
prevents models discovered in the original model from appearing in the
projected version of the model.

The following example `build.gradle.kts` file will run a "projection"
build that uses the "external" projection.

```kotlin
plugins {
    java
    id("software.amazon.smithy").version("0.0.1")
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("software.amazon.smithy:smithy-aws-traits:0.4.1")

        // Take a dependency on the internal model package. This
        // dependency *must* be a buildscript only dependency to ensure
        // that is does not appear in the generated JAR.
        classpath("com.foo.baz:foo-internal-model:1.0.0")
    }
}

// Use the "external" projection. This projection must be found in the
// smithy-build.json file.
configure<software.amazon.smithy.gradle.SmithyExtension> {
    projection = "external"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Any dependencies that the projected model needs must be (re)declared
    // here. For example, let's assume that the smithy-aws-traits package is
    // needed in the projected model too.
    api("software.amazon.smithy:smithy-aws-traits:0.4.0")
}
```

Because the `projection` of the `SmithyExtension` was set to `external`, a
`smithy-build.json` file **must** be found that defines the `external`
projection. For example:

```json
{
    "smithy": "1.0",
    "projections": {
        "external": {
            // This projection creates a modified version of the
            // Smithy model dependencies found in the buildscript.
            "transforms": [
                {"name": "excludeShapesByTag", "args": ["internal"]},
                {"name": "excludeTraitsByTag", "args": ["internal"]},
                {"name": "excludeMetadata", "args": ["suppressions", "validators"]},
                {"name": "removeUnusedShapes", "args": []}
            ]
        }
    }
}
```


### Building artifacts from Smithy models

If a `smithy-build.json` file is found at the root of the project, then it
will be used to generate artifacts from the Smithy model.

The classpath used to find classes and Smithy models depends on if a
``projection`` was specified in the `SmithyExtension`.

- If a `projection` **is not** specified, then `smithy-build` is executed
  using the `compileClasspath` plus the `buildscript` classpath. Smithy models
  are discovered using only the `compileClasspath`.
- If a `projection` **is** specified, then `smithy-build` is executed using
  only the `buildscript` classpath, and Smithy models are discovered using
  only the `buildscript` classpath. This prevents models discovered in the
  original model from appearing in the projected version of the model.

The following example generates an OpenAPI model from a Smithy model:

```json
{
    "smithy": "1.0",
    "plugins": {
        "openapi": {
            "service": "foo.baz#MyService"
        }
    }
}
```

The above Smithy plugin also requires a `buildscript` dependency in
`build.gradle.kts`:

```kotlin
buildscript {
    // ...
    dependencies {
        // ...

        // This dependency is required in order to apply the "openapi"
        // plugin in smithy-build.json
        classpath("software.amazon.smithy:smithy-openapi:1.0.0")
    }
}
```


## Testing the plugin

The plugin is tested using integration tests that exercise example packages
in the `examples` directory. Failure cases are placed in `examples/failure-cases`.
Integration tests are executed with Gradle using the `integTest` task. Note
that this task requires publishing the Smithy Gradle plugin to your Maven
local repository.


## License

This library is licensed under the Apache 2.0 License. 