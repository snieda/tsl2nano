sourceType=${1:-jsonschema}
targetPackage=${2:-org.anonymous.project}
. mvnw org.jsonschema2pojo:jsonschema2pojo-maven-plugin:generate \
    -Djsonschema2pojo.sourceDirectory="generated-src" \
    -Djsonschema2pojo.serializable="true" \
    -Djsonschema2pojo.includeRequiredPropertiesConstructor="true" \
    -Djsonschema2pojo.includeJsr303Annotations="true" \
    -Djsonschema2pojo.includeJsr305Annotations="true" \
    -Djsonschema2pojo.useJakartaValidation="true" \
    -Djsonschema2pojo.targetPackage="$targetPackage" \
    -Djsonschema2pojo.sourceType="$sourceType" \
    -Djsonschema2pojo.formatDates="true" \
    -Djsonschema2pojo.formatTimes="true" \
    -Djsonschema2pojo.formatDateTimes="true"

. mvnw -f pom-openapi.xml dependency:copy-dependencies -DoutputDirectory=./
. compilejar.cmd

