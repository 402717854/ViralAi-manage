# syntax=docker/dockerfile:1

###############################################################################
# 构建阶段：Maven 编译打包（多模块，产出 ruoyi-admin.jar）
###############################################################################
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# 可选：切换 Maven 镜像源加速构建（默认走官方中央仓库）
# 用法：docker build --build-arg MAVEN_MIRROR=https://maven.aliyun.com/repository/public .
ARG MAVEN_MIRROR=""
RUN if [ -n "$MAVEN_MIRROR" ]; then \
        mkdir -p /root/.m2 && \
        printf '<settings><mirrors><mirror><id>custom</id><mirrorOf>central</mirrorOf><name>custom</name><url>%s</url></mirror></mirrors></settings>' "$MAVEN_MIRROR" > /root/.m2/settings.xml; \
    fi

# 先只拷贝各模块 pom，让「依赖下载」独立成层：改代码不会重新下载依赖
COPY pom.xml ./
COPY ruoyi-admin/pom.xml ruoyi-admin/
COPY ruoyi-common/pom.xml ruoyi-common/
COPY ruoyi-framework/pom.xml ruoyi-framework/
COPY ruoyi-generator/pom.xml ruoyi-generator/
COPY ruoyi-quartz/pom.xml ruoyi-quartz/
COPY ruoyi-system/pom.xml ruoyi-system/
COPY lion-crm/pom.xml lion-crm/
RUN mvn -B -ntp dependency:go-offline -DskipTests || true

# 拷贝源码并打包（测试在 CI 阶段执行，镜像构建跳过）
COPY . .
RUN mvn -B -ntp -DskipTests clean package

###############################################################################
# 运行阶段：仅 JRE，非 root 用户运行
###############################################################################
FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms512m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

# curl 用于容器健康检查；tzdata 用于时区
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
 && groupadd -g 1000 viralai \
 && useradd -u 1000 -g viralai -m -s /bin/bash viralai \
 && mkdir -p /home/viralai/logs /home/viralai/uploadPath \
 && chown -R viralai:viralai /home/viralai

WORKDIR /home/viralai

# 只拷贝可执行 jar（ruoyi-admin.jar.original 不进入镜像）
COPY --from=build --chown=viralai:viralai /build/ruoyi-admin/target/ruoyi-admin.jar /home/viralai/app.jar

USER viralai
EXPOSE 8080

# /captchaImage 为免登录接口，能正常返回即代表 Spring 上下文已就绪
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=6 \
    CMD curl -fsS http://127.0.0.1:8080/captchaImage > /dev/null || exit 1

# exec 让 java 成为 PID 1，docker stop 时能收到 SIGTERM 优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /home/viralai/app.jar"]
