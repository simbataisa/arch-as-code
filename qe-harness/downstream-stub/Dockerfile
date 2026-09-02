# TST-035 circuit breaker capability (Task 11): a small, static downstream
# responder for docker-compose (Task 14) -- a real, network-reachable
# service, unlike BreakerBehaviourTest's in-process JDK HttpServer stub.
#
# This image always answers the same canned quote; it is never itself
# blackholed/restored. That job belongs to Toxiproxy (wired in front of this
# container in Task 14's compose file), which is what
# app.downstream.base-url actually points at in that setup -- see
# reference-sut's application.yml and DownstreamClient.
#
# default.conf uses `return 200 '<json>'` (not `alias`/`try_files` against
# quote.json) so any path under /downstream/quotes/ -- with or without a
# trailing {id}, e.g. DownstreamClient's actual /downstream/quotes/Q1 --
# resolves to the same canned body. A first cut of this config used `alias`
# pointed straight at quote.json, which nginx treats as a literal file, not
# a directory: GET /downstream/quotes/ 500'd ("is not a directory") and
# GET /downstream/quotes/Q1 404'd (nginx tried to open a file literally
# named ".../index.jsonQ1"). Fixed and verified end-to-end (see
# qe-harness/README.md's TST-035 section).
FROM nginx:1.27-alpine

COPY quote.json /usr/share/nginx/html/quote.json
COPY default.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
