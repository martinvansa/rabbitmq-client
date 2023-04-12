# Pull gradle to build the project for S2I
ARG ISSP_ENV
ARG BUILD_BASE_IMAGE
ARG BASE_IMAGE

FROM registry.cirrus.ibm.com/issp-${ISSP_ENV}/k-issp-build-base:${BUILD_BASE_IMAGE} as build
COPY . /src
WORKDIR /src

#ARG ISSP_ENV
ARG TAASUSERNAME
ARG TAASPASSWORD
#ARG APPSCAN_ID
#ARG APPSCAN_KEY
#ARG APPSCAN_SECRET
#ARG cirrus_build_id
#ARG cirrus_git_commit
 
RUN ./gradlew build --no-daemon -PtaasUsername=${TAASUSERNAME} -PtaasPassword=${TAASPASSWORD} -Pfull=no

#RUN export SERVICE_VERSION=$(cat settings.gradle | grep name | sed "s/.*'\(.*\)'/\1/") && if [ "${ISSP_ENV}" = "development" ]; then ./gradlew appscan-analyze -Pfull=no -DappId=${APPSCAN_ID} -DirxName="${SERVICE_VERSION}" -DappscanKey=${APPSCAN_KEY} -DappscanSecret=${APPSCAN_SECRET}; else echo "Not building ${ISSP_ENV}"; fi

##########################
# Throw it in the base image and run it.
FROM registry.cirrus.ibm.com/issp-${ISSP_ENV}/k-issp-ms-base:${BASE_IMAGE}

COPY --from=build /src/build/libs/* /deployments/

# certificate - required for rabbitmq, mongodb, so2o
# so2o - to run quote requests to the hub
# rabbitmq -
# isspdb - retrieve data related to case/quote
# mongodb -
# ENV JAVA_OPTIONS "-Dspring.profiles.active=certificate,so2o,rabbitmq,isspdb,mongodb"
ENV JAVA_APP_JAR app.jar

ENTRYPOINT ["/deployments/run-java.sh"]