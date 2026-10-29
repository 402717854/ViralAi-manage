# syntax=docker/dockerfile:1

###############################################################################
# 构建阶段：Node 编译 Vite 产物
###############################################################################
# 构建阶段用 glibc 基础镜像：项目依赖 sass-embedded（原生二进制），alpine(musl) 易出现绑定问题
FROM node:22-bookworm-slim AS build

WORKDIR /app

# 可选：切换 npm 镜像源（默认使用 npmmirror 加速国内构建）
ARG NPM_REGISTRY=https://registry.npmmirror.com
RUN npm config set registry "$NPM_REGISTRY" \
 && npm config set fund false \
 && npm config set audit false

# 仓库未提交 lock 文件，只能 install；若已生成 package-lock.json 可换成 npm ci
COPY package.json ./
RUN npm install

COPY . .
RUN npm run build:prod

###############################################################################
# 运行阶段：Nginx 托管静态资源 + 反向代理后端
###############################################################################
FROM nginx:1.27-alpine

# BACKEND_UPSTREAM：后端地址，compose 内为服务名 server:8080；独立部署时用 -e 覆盖
ENV TZ=Asia/Shanghai \
    BACKEND_UPSTREAM=server:8080

RUN apk add --no-cache tzdata \
 && cp /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone

COPY --from=build /app/dist /usr/share/nginx/html

# 官方镜像启动时会用 envsubst 把 /etc/nginx/templates/*.template 渲染到 /etc/nginx/conf.d/
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1
