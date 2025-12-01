# Use a lightweight Java 17 runtime image instead of a full Maven image
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tzdata ca-certificates

# 2. Thiết lập Timezone
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# -------------------

WORKDIR /app

# Copy the application jar
COPY ./be-src/target/restful-api-app.jar ./restful-api-app.jar

# Create a non-root user and group for Alpine
RUN addgroup -S webapp && adduser -S -G webapp rest-app

# Change ownership of the app directory
RUN chown -R rest-app:webapp /app

# Switch to the non-root user
USER rest-app

EXPOSE 9595

# Run the application
CMD ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "./restful-api-app.jar"]