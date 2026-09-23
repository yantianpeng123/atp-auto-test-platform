pipeline {
  agent any
  environment {
    ATP_BASE   = 'http://host.docker.internal:8080'
    PROJECT_ID = '3'
    BATCH_ID   = '2'
  }
  stages {
    stage('Trigger ATP regression') {
      steps {
        withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
          script {
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

            def status = 'RUNNING'
            timeout(time: 30, unit: 'MINUTES') {
              while (status == 'RUNNING') {
                sleep 10
                def resResp =  withEnv(["RUN_ID=${runId}"]){
                sh(
                  script: '''
                    curl -s "$ATP_BASE/api/ci/result/$RUN_ID" \
                      -H "X-CI-Token: $ATP_TOKEN"
                  ''',
                  returnStdout: true,
                  env: [RUN_ID: "${runId}"]
                ).trim()
                }
                def res = parseJson(resResp)

                status = res.data?.status ?: res.status
                def passed = res.data?.passed ?: 0
                def failed = res.data?.failed ?: 0
                echo "status=${status} passed=${passed} failed=${failed}"

                if (status == 'FAILED' || (failed as int) > 0) {
                  error("ATP 回归失败: 通过 ${passed} / 失败 ${failed}")
                }
              }
            }
          }
        }
         stage('Fetch JUnit report & publish') {
         steps{
            withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
                      sh "curl -s ${ATP_BASE}/api/ci/report/${env.RUN_ID}.xml -H 'X-CI-Token: ${ATP_TOKEN}' -o atp-report.xml"
         }
          junit 'atp-report.xml'
         }
      }
    }
  }
}

// 在 @NonCPS 方法里解析 JSON，并递归转换为可序列化的 HashMap/ArrayList
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