FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

# 3. Build code
COPY src ./src
RUN mvn clean package -DskipTests

# 4. Chuyển sang Giai đoạn Run (Vẫn bắt buộc phải là Java 17)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Nhìn vào dòng 12 và 13 của pom.xml (artifactId: auth-service, version: 0.0.1-SNAPSHOT)
# File build ra chắc chắn sẽ tên là auth-service-0.0.1-SNAPSHOT.jar
# Nhưng dùng *.jar sẽ tự động bắt lấy file này
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8480
ENTRYPOINT ["java", "-jar", "app.jar"]
