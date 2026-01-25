FROM ${docker.java.base} as builder
# First stage : Extract the layers
WORKDIR application
COPY ${project.build.finalName}.jar ${project.build.finalName}.jar
RUN java -Djarmode=layertools -jar ${project.build.finalName}.jar extract

FROM tranvanhoa2810/base-image:latest as runtime
# Second stage : Copy the extracted layers
COPY --from=builder application/application/ ./
CMD ["java", "-javaagent:elastic-apm-agent-${apm.agent.version}.jar","org.springframework.boot.loader.launch.JarLauncher"]
