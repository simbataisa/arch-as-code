// TST-035 Toxiproxy control (Task 18).
//
// Invoked twice by plan.jmx with a different `parameters` value:
//   - setUp Thread Group ("Inject Downstream Fault"), parameters="inject"
//   - TearDown Thread Group ("Restore Downstream"), parameters="remove" --
//     the unconditional call that guarantees the fault never outlives this
//     run, however the load in between behaved.
//
// Talks directly to Toxiproxy's HTTP control API (TOXIPROXY_BASE_URL,
// default http://localhost:8474 -- matches docker-compose.yml's published
// port for the `toxiproxy` service, Task 14) with plain
// java.net.http.HttpClient. No extra library is needed on JMeter's
// classpath for this (unlike assert-degradation.groovy's use of
// qe-harness-common/Jackson): see harness/jmeter/pom.xml's testPlanLibraries
// comment -- this file adds nothing to that list.
//
// Idempotent in both directions, which is what makes the "always remove
// first" step below safe to call twice in a row (once from a genuine
// TearDown, and again -- a no-op -- if the module ever adds a second
// removal point):
//   - "remove": DELETE .../toxics/{name}; Toxiproxy answers 404 if the
//     toxic is already gone, treated the same as a successful 200.
//   - "inject": the same DELETE runs first (defensive -- a prior run whose
//     TearDown never got to execute, e.g. a hard process kill, must not
//     leave this call failing with Toxiproxy's own 409 "already exists"
//     instead of installing a fresh toxic), then POST adds it back.
//
// Fault shape: a `reset_peer` toxic on the `downstream` stream (the
// response direction, upstream -> client) with `timeout: 0`, confirmed
// empirically against this exact stack (`make up PROFILES="core
// resilience"`, direct curl against :8474/:8080) to reset the TCP
// connection immediately -- no read-timeout wait needed to observe the
// failure, the same "instant, deterministic failure" property
// BreakerBehaviourTest's own blackhole() stub has (see that test's
// Javadoc). A `timeout` toxic would work too but adds up to
// DownstreamClient's own 1000ms read-timeout of latency per call for no
// benefit here.

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

String action = (Parameters == null) ? "" : Parameters.trim()
if (!(action == "inject" || action == "remove")) {
    throw new IllegalArgumentException(
        "toxic-control.groovy: unknown action '" + action + "' (expected 'inject' or 'remove')")
}

String toxiproxyBaseUrl = System.getenv().getOrDefault("TOXIPROXY_BASE_URL", "http://localhost:8474")
// qe-harness/toxiproxy/proxies.json (Task 14): the one proxy fronting
// downstream-stub, and what reference-sut's app.downstream.base-url points
// at under the `resilience` compose profile.
String proxyName = "downstream"
String toxicName = "tst035-downstream-fault"

HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
String toxicUri = toxiproxyBaseUrl + "/proxies/" + proxyName + "/toxics/" + toxicName

HttpRequest deleteRequest = HttpRequest.newBuilder(URI.create(toxicUri))
    .timeout(Duration.ofSeconds(5))
    .DELETE()
    .build()
HttpResponse<String> deleteResponse = http.send(deleteRequest, HttpResponse.BodyHandlers.ofString())
if (deleteResponse.statusCode() != 204 && deleteResponse.statusCode() != 404) {
    throw new IllegalStateException(
        "DELETE " + toxicUri + " -> " + deleteResponse.statusCode() + " " + deleteResponse.body())
}
boolean wasPresent = deleteResponse.statusCode() == 204

String resultMessage
if (action == "inject") {
    String body = '{"name":"' + toxicName + '","type":"reset_peer","stream":"downstream",' +
        '"toxicity":1.0,"attributes":{"timeout":0}}'
    HttpRequest postRequest = HttpRequest.newBuilder(
            URI.create(toxiproxyBaseUrl + "/proxies/" + proxyName + "/toxics"))
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    HttpResponse<String> postResponse = http.send(postRequest, HttpResponse.BodyHandlers.ofString())
    if (postResponse.statusCode() != 200) {
        throw new IllegalStateException(
            "POST " + toxiproxyBaseUrl + "/proxies/" + proxyName + "/toxics -> "
                + postResponse.statusCode() + " " + postResponse.body())
    }
    resultMessage = "injected " + toxicName + " on proxy " + proxyName + " (was already present=" + wasPresent + ")"
} else {
    resultMessage = "removed " + toxicName + " from proxy " + proxyName + " (was present=" + wasPresent + ")"
}

SampleResult.setSuccessful(true)
SampleResult.setResponseData(resultMessage, "UTF-8")
SampleResult.setResponseCode("200")
