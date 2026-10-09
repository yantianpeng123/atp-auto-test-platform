pipeline {
  agent any
  environment {
    ATP_BASE   = 'http://host.docker.internal:8081'
    PROJECT_ID = '3'
    BATCH_ID   = '2'
    APP_NAME   = 'atp-app'
    APP_PORT   = '9090'
    IMAGE_TAG  = "${env.BUILD_NUMBER}"

    FRONTEND_APP_NAME = 'atp-frontend'
    FRONTEND_PORT     = '8080'
  }
  stages {
    stage('Trigger ATP regression') {
      steps {
        withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
          script {
            // 1. 触发回归
            def trigResp = sh(
              script: '''
                curl -s -X POST "$ATP_BASE/api/ci/trigger" \
                  -H "X-CI-Token: $ATP_TOKEN" \
                  -H "Content-Type: application/json" \
                  -d "{\\"projectId\\":$PROJECT_ID,\\"batchId\\":$BATCH_ID}"
              ''',
              returnStdout: true
            ).trim()
            echo "trigger resp: ${trigResp}"

            def trig = parseJson(trigResp)
            def runId = trig.data?.runId
            echo "runId=${runId}"

            if (!runId) {
              error("触发失败，未获取到 runId。响应: ${trigResp}")
            }

            // 把 runId 写回全局 env，供 post 块使用
            env.RUN_ID = runId.toString()

            // 2. 轮询结果
            def status = 'RUNNING'
            timeout(time: 30, unit: 'MINUTES') {
              while (status == 'RUNNING') {
                sleep 10

                def resResp = withEnv(["RUN_ID=${runId}"]) {
                  sh(
                    script: '''
                      echo ">>> GET $ATP_BASE/api/ci/result/$RUN_ID" >&2
                      curl -s "$ATP_BASE/api/ci/result/$RUN_ID" \
                        -H "X-CI-Token: $ATP_TOKEN"
                    ''',
                    returnStdout: true
                  ).trim()
                }
                echo "result resp: ${resResp}"

                def res = parseJson(resResp)
                status = res.data?.status ?: res.status
                def passed = res.data?.passed ?: 0
                def failed = res.data?.failed ?: 0
                echo "status=${status} passed=${passed} failed=${failed}"

                if (status in ['FAILED', 'PARTIAL_FAILED'] || (failed as int) > 0) {
                  error("ATP 回归失败: 通过 ${passed} / 失败 ${failed}")
                }
              }
            }
          }
        }
      }
    }

    stage('Maven Build') {
      steps {
        dir('backend') {
          sh 'mvn clean package -DskipTests'
        }
      }
    }

    stage('Build Image') {
      steps {
        dir('backend') {
          sh '''
            docker build -t ${APP_NAME}:${IMAGE_TAG} -f Dockerfile .
            docker tag ${APP_NAME}:${IMAGE_TAG} ${APP_NAME}:latest
            echo "镜像构建完成: ${APP_NAME}:${IMAGE_TAG}"
          '''
        }
      }
    }

    // 前端镜像：Dockerfile 内只跑 vite build（esbuild 转译，低内存）。
    // 注意：CI 未单独跑 vue-tsc 类型检查，类型问题需开发者本地把关。
    stage('Frontend Build Image') {
      steps {
        dir('frontend') {
          sh '''
            docker build -t ${FRONTEND_APP_NAME}:${IMAGE_TAG} -f Dockerfile .
            docker tag ${FRONTEND_APP_NAME}:${IMAGE_TAG} ${FRONTEND_APP_NAME}:latest
            echo "前端镜像构建完成: ${FRONTEND_APP_NAME}:${IMAGE_TAG}"
          '''
        }
      }
    }

    stage('Deploy') {
      steps {
        sh '''
          # 保存旧镜像，用于回滚
          OLD_CONTAINER_EXISTS=$(docker ps -a --filter "name=^${APP_NAME}$" --format '{{.Names}}')
          if [ -n "$OLD_CONTAINER_EXISTS" ]; then
            OLD_IMAGE=$(docker inspect --format='{{.Config.Image}}' ${APP_NAME} 2>/dev/null || echo "")
            echo "$OLD_IMAGE" > /tmp/${APP_NAME}_old_image
            echo "旧镜像: $OLD_IMAGE"
          else
            echo "" > /tmp/${APP_NAME}_old_image
            echo "首次部署，无旧镜像"
          fi

          # 停旧容器
          docker stop ${APP_NAME} || true
          docker rm ${APP_NAME} || true

          # 起新容器
          docker run -d --name ${APP_NAME} \
            --add-host host.docker.internal:host-gateway \
            -p ${APP_PORT}:8081 \
            --restart always \
            -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/atp?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
            -e SPRING_DATASOURCE_USERNAME="root" \
            -e SPRING_DATASOURCE_PASSWORD="root" \
            -e SPRING_DATA_REDIS_HOST=host.docker.internal \
            -e SPRING_DATA_REDIS_PORT=6379 \
            -e SPRING_DATA_REDIS_PASSWORD= \
            ${APP_NAME}:${IMAGE_TAG}

          echo "部署完成: ${APP_NAME}:${IMAGE_TAG}"
        '''
      }
    }

    stage('Frontend Deploy') {
      steps {
        sh '''
          docker stop ${FRONTEND_APP_NAME} || true
          docker rm ${FRONTEND_APP_NAME} || true
          docker run -d --name ${FRONTEND_APP_NAME} \
            --add-host host.docker.internal:host-gateway \
            -p ${FRONTEND_PORT}:80 \
            --restart always \
            -e BACKEND_HOST=host.docker.internal \
            -e BACKEND_PORT=${APP_PORT} \
            ${FRONTEND_APP_NAME}:${IMAGE_TAG}
          echo "前端部署完成: ${FRONTEND_APP_NAME}:${IMAGE_TAG} (端口 ${FRONTEND_PORT})"
        '''
      }
    }

    stage('Health Check') {
      steps {
        script {
          def healthy = sh(
            script: '''
              for i in $(seq 1 30); do
                if curl -sf http://host.docker.internal:${APP_PORT}/actuator/health; then
                  echo "健康检查通过"
                  exit 0
                fi
                echo "等待应用启动... ($i/30)"
                sleep 5
              done
              echo "健康检查失败"
              exit 1
            ''',
            returnStatus: true
          ) == 0

          if (healthy) {
            // 部署成功：清理旧镜像
            sh '''
              OLD_IMAGE=$(cat /tmp/${APP_NAME}_old_image)
              if [ -n "$OLD_IMAGE" ] && [ "$OLD_IMAGE" != "${APP_NAME}:${IMAGE_TAG}" ]; then
                echo "清理旧镜像: $OLD_IMAGE"
                docker rmi "$OLD_IMAGE" || true
              fi
              docker image prune -f --filter "until=24h" || true
            '''
          } else {
            // 部署失败：用旧镜像回滚
            sh '''
              OLD_IMAGE=$(cat /tmp/${APP_NAME}_old_image)
              docker stop ${APP_NAME} || true
              docker rm ${APP_NAME} || true

              if [ -n "$OLD_IMAGE" ]; then
                docker run -d --name ${APP_NAME} \
                  --add-host host.docker.internal:host-gateway \
                  -p ${APP_PORT}:8081 \
                  --restart always \
                  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/atp?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true" \
                  -e SPRING_DATASOURCE_USERNAME="root" \
                  -e SPRING_DATASOURCE_PASSWORD="root" \
                  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
                  -e SPRING_DATA_REDIS_PORT=6379 \
                  -e SPRING_DATA_REDIS_PASSWORD= \
                  $OLD_IMAGE
                echo "已回滚到: $OLD_IMAGE"
              else
                echo "无旧镜像可回滚"
              fi

              # 删除本次失败的新镜像
              docker rmi ${APP_NAME}:${IMAGE_TAG} || true
            '''
            error("健康检查失败，已回滚")
          }
        }
      }
    }

    stage('Frontend Health Check') {
      steps {
        script {
          def ok = sh(
            script: '''
              for i in $(seq 1 20); do
                if curl -sf http://host.docker.internal:${FRONTEND_PORT}/ -o /dev/null; then
                  echo "前端健康检查通过"
                  exit 0
                fi
                echo "等待前端启动... ($i/20)"
                sleep 3
              done
              echo "前端健康检查失败"
              exit 1
            ''',
            returnStatus: true
          ) == 0
          if (!ok) {
            error("前端健康检查失败")
          }
        }
      }
    }
  }

  post {
    always {
      withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
        sh '''
          curl -sf "$ATP_BASE/api/ci/report/$RUN_ID.xml" \
            -H "X-CI-Token: $ATP_TOKEN" \
            -o atp-report.xml || true
        '''
      }
      junit testResults: 'atp-report.xml', allowEmptyResults: true
    }
  }
}

// @NonCPS 方法定义在 pipeline 块外部
@NonCPS
def parseJson(String text) {
  def obj = new groovy.json.JsonSlurper().parseText(text)
  return toSerializable(obj)
}

@NonCPS
def toSerializable(obj) {
  if (obj instanceof Map) {
    def m = new HashMap()
    obj.each { k, v -> m.put(k.toString(), toSerializable(v)) }
    return m
  } else if (obj instanceof List) {
    return obj.collect { toSerializable(it) }
  }
  return obj
}