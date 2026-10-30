
### 部署

    # 1. 先构建镜像（只有镜像变更时执行，--no-build 跳过这一步）
    docker compose -f docker-compose-prod.yml --env-file .env build

    docker save -o viralai-prod.tar viralai:prod

    docker load -i viralai-prod.tar.gz
    
    # 2. 再后台启动（你这条命令）
    docker compose -f docker-compose-prod.yml --env-file .env up -d --no-build
    
    # 3. 查看启动日志
    docker compose -f docker-compose-prod.yml --env-file .env.prod logs -f
    
    # 4. 只看某个服务日志（例如 viralai）
    docker compose -f docker-compose-prod.yml --env-file .env.prod logs -f viralai

     
    