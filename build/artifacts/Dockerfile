FROM simplifierag/simplifier-assembly:graal25_2.0.9 AS build

# guuess, that this is not used at all!

RUN echo "Not used!"

WORKDIR /build
COPY . /build/
RUN sbt assembly

FROM debian:bookworm-slim AS contentrepo_plugin_builder

RUN apt-get update && \
    apt-get upgrade -f -y
RUN mkdir -p /opt/plugin

#  provide plugin jar & config files
COPY --from=this assets /opt/plugin/assets
COPY --from=this setup.sh /opt/plugin/setup.sh
COPY --from=build /build/target/scala-2.12/contentRepoPlugin.jar /opt/plugin/assets

FROM scratch
COPY --from=contentrepo_plugin_builder /opt/plugin /opt/plugin

