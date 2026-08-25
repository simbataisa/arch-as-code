# TST-035 circuit breaker capability (Task 11): a small, static downstream
# responder for docker-compose (Task 14) -- a real, network-reachable
# service, unlike BreakerBehaviourTest's in-process JDK HttpServer stub.
#
# This image always answers the same canned quote; it is never itself
# blackholed/restored. That job belongs to Toxiproxy (wired in front of this
# container in Task 14's compose file), which is what
# app.downstream.base-url actually points at in that setup -- see
# reference-sut's application.yml and DownstreamClient.
FROM nginx:1.27-alpine

COPY quote.json /usr/share/nginx/html/downstream/quotes/index.json

RUN printf 'server {\n\
    listen 8080;\n\
    location /downstream/quotes/ {\n\
        default_type application/json;\n\
        alias /usr/share/nginx/html/downstream/quotes/index.json;\n\
    }\n\
}\n' > /etc/nginx/conf.d/default.conf

EXPOSE 8080
