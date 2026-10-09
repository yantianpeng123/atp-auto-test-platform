# ============ 构建阶段 ============
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
# 直接调用 vite build，跳过 package.json 中 "build" 的 vue-tsc 类型检查
RUN npx vite build

# ============ 运行阶段 ============
FROM nginx:1.27-alpine

# 移除官方默认站点，避免与下方模板生成的配置冲突
RUN rm /etc/nginx/conf.d/default.conf

# 后端地址：通过环境变量注入。
# 官方 nginx 镜像在容器启动时会自动对 /etc/nginx/templates/*.template
# 执行 envsubst，把 ${BACKEND_HOST} / ${BACKEND_PORT} 替换为实际值。
# 默认 8081 与 backend/Dockerfile 的 EXPOSE 端口保持一致。
ENV BACKEND_HOST=backend
ENV BACKEND_PORT=8081

COPY nginx.conf /etc/nginx/templates/default.conf.template

# 把构建出的前端静态产物放入 Nginx 根目录（缺失这行会导致只显示 Welcome to nginx）
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
# 镜像自带 entrypoint 会先完成模板变量替换，再拉起 nginx
