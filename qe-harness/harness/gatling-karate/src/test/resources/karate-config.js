function fn() {
  // Same SUT_BASE_URL convention every other harness module uses (see
  // ModuleRunner.java and the JMeter assert-*.groovy scripts) -- a plain
  // process environment variable read by whichever JVM is actually running
  // (this Karate JUnit runner directly, or the forked Gatling engine
  // process gatling-maven-plugin launches for Tst030Simulation), not a
  // JVM system property: the reference SUT is a separate, already-running
  // Docker container (`docker compose --profile core up`), reachable only
  // over HTTP on this host/port.
  var baseUrl = karate.properties['SUT_BASE_URL'];
  if (!baseUrl) {
    baseUrl = java.lang.System.getenv('SUT_BASE_URL');
  }
  if (!baseUrl) {
    baseUrl = 'http://localhost:8080';
  }

  var config = { baseUrl: baseUrl };
  return config;
}
