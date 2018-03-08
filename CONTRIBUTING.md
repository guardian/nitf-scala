# Contributing to nitf-scala

:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:

Your contributions are most welcome. Please feel free to open issues or submit pull requests.

## Project Structure

The project build definition is in [project/Build.scala](project/Build.scala),
where a sub-project is dynamically generated for each version of NITF.

The build is defined to cross-compile against Scala 2.11 and 2.12.

The schema definition for each supported version of NITF is in the [schema](schema) directory.
These files are used to generate the Scala classes using ScalaXB as described in the [README](README.md#generated-sources).

There is one test class that reads an example specific to each version.
The test class is in [Tests/src/test/scala](Tests/src/test/scala).
The examples are in [Tests/src/test/resources](Tests/src/test/resources).

## Continuous Integration

The project is [configured](.travis.yml) to run on Travis CI.
The matrix is configured to build each version of NITF separately.
This helps speed up the build dramatically.

## Releasing

To release this project, run:
```bash
sbt "release with-defaults"
```

Releasing is currently [configured](project/Build.scala) to publish locally only.
