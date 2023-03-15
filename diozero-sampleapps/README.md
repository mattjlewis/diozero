# Sample Applications
Contains various tests, samples, examples, and general applications to exercise certain portions fo the library suite(s).

The "default build" will construct a "shaded jar" that contains the dependencies listed and uses the _Default_ provider. To run one of the applications using this jar, simply invoke

```shell
java -cp diozero-sampleapps-<version>.jar <full reference to main class>
```
`
Example:
```shell
`java -cp diozero-sampleapps-1.4.0.jar com.diozero.sampleapps.AnimationTest
```

If you wish to use one of the other providers, un-comment that dependency and re-build. Note that you **cannot** use multiple providers at the same time in this JAR due to the SPI limitations.
