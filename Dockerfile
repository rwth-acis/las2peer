# las2peer node built from source (core + restmapper + webconnector incl. frontend).
FROM eclipse-temurin:17-jdk-jammy AS build
ARG NODE_VERSION=16.20.2
ARG TARGETARCH
RUN apt-get update && apt-get install -y --no-install-recommends curl xz-utils \
    && rm -rf /var/lib/apt/lists/* \
    && case "$TARGETARCH" in amd64) arch=x64 ;; arm64) arch=arm64 ;; *) echo "unsupported arch $TARGETARCH" && exit 1 ;; esac \
    && curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${arch}.tar.xz" \
       | tar -xJ -C /usr/local --strip-components=1
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle --mount=type=cache,target=/root/.npm \
    ./gradlew build -x test -x javadoc -x junitdoc --no-daemon --console=plain

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 1000 las2peer
WORKDIR /app
COPY --from=build /src/core/export/jars core/export/jars
COPY --from=build /src/restmapper/export/jars restmapper/export/jars
COPY --from=build /src/webconnector/export/jars webconnector/export/jars
COPY --from=build /src/webconnector/lib webconnector/lib
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN mkdir -p etc service node-storage log && chown -R las2peer:las2peer /app
USER las2peer
# 9011: las2peer P2P (FreePastry), 8080: WebConnector (REST API + node frontend)
EXPOSE 9011 9011/udp 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s CMD curl -sf http://localhost:8080/las2peer/version || exit 1
ENTRYPOINT ["/app/entrypoint.sh"]
