# Use a lightweight Java 17 runtime image instead of a full Maven image
FROM eclipse-temurin:17-jre-alpine

ENV TZ=Asia/Ho_Chi_Minh
# Use && to combine RUN commands for better layer caching
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

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
CMD ["java", "-jar", "./restful-api-app.jar"]
