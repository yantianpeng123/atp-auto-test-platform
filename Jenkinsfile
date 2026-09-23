pipeline {
  agent any
  environment {
    ATP_BASE   = 'http://host.docker.internal:8080'      // Docker 起的 Jenkins 用 http://host.docker.internal:8080
    PROJECT_ID = '3'
    BATCH_ID   = '2'
    ATP_TOKEN  = credentials('atp-ci-token')   // 在 Jenkins Credentials 里建的 Secret text
  }
  stages {
    stage('Trigger ATP regression') {
      steps {
        script {
          def trigResp = sh(script: "curl -s -X POST ${ATP_BASE}/api/ci/trigger -H 'X-CI-Token: ${ATP_TOKEN}' -H 'Content-Type: application/json' -d '{\"projectId\":${PROJECT_ID},\"batchId\":${BATCH_ID}}'", returnStdout: true).trim()
          echo "trigger resp: ${trigResp}"
          def trig = new groovy.json.JsonSlurper().parseText(trigResp)
          def runId = trig.runId
          echo "runId=${runId}"
          def status = 'RUNNING'
          timeout(time: 30, unit: 'MINUTES') {
            while (status == 'RUNNING') {
              sleep 10
              def resResp = sh(script: "curl -s ${ATP_BASE}/api/ci/result/${runId} -H 'X-CI-Token: ${ATP_TOKEN}'", returnStdout: true).trim()
              def res = new groovy.json.JsonSlurper().parseText(resResp)
              status = res.status
              def passed = res.passed ?: 0
              def failed = res.failed ?: 0
              echo "status=${status} passed=${passed} failed=${failed}"
              if (status == 'FAILED' || (failed as int) > 0) { error("ATP 回归失败: 通过 ${passed} / 失败 ${failed}") }
            }
          }
        }
      }
    }
  }
}