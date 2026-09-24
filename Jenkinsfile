pipeline {
  agent any
  environment {
    ATP_BASE   = 'http://host.docker.internal:8080'
    PROJECT_ID = '3'
    BATCH_ID   = '2'
    APP_NAME   = 'atp-app'
    APP_PORT   = '9090'
    IMAGE_TAG  = "${env.BUILD_NUMBER}"
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

            // 关键：把 runId 写回全局 env，供后续 stage 使用
            env.RUN_ID = runId.toString()

            // 2. 轮询结果
            def status = 'RUNNING'
            timeout(time: 30, unit: 'MINUTES') {
              while (status == 'RUNNING') {
                sleep 10

                def resResp = withEnv(["RUN_ID=${runId}"]) {
                  sh(
                    script: '''
                      echo ">>> GET $ATP_BASE/api/ci/result/$RUN_ID">&2
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

                if (status in ['FAILED']  || (failed as int) > 0) {
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
          dir("backend"){
          sh 'mvn clean package -DskipTests'
          }
        }
      }
      stage('Build Image') {
            steps {
              sh '''
                docker build -t ${APP_NAME}:${IMAGE_TAG} -f dockerfile .
                docker tag ${APP_NAME}:${IMAGE_TAG} ${APP_NAME}:latest
                echo "镜像构建完成: ${APP_NAME}:${IMAGE_TAG}"
              '''
            }
          }
      stage('Deploy'){
        steps{
            sh '''
            OLD_IMAGE=$(docker inspect --format='{{.Config.Image}}' ${APP_NAME} 2>/dev/null || echo "")
            echo "$OLD_IMAGE" > /tmp/${APP_NAME}_old_image
             docker stop ${APP_NAME} || true
             docker rm ${APP_NAME} || true
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

                if (!healthy) {
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
                                           ${APP_NAME}:${IMAGE_TAG}
                      echo "已回滚到: $OLD_IMAGE"
                    else
                      echo "无旧镜像可回滚"
                    fi
                  '''
                  error("健康检查失败，已回滚")
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
//测试jeknins集成CI
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