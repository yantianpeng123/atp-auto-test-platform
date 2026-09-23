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
          //def trig = new groovy.json.JsonSlurperClassic().parseText(trigResp)
          echo "trigger resp: ========222222========"
          def runId = trig.data.runId
          echo "trigger resp: ========33333======== ${runId}"
          def trig = readJSON text: trigResp
          echo "trigger resp: ==========111111======"
          def runId = trig.data.runId
          echo "runId=${runId}"
          env.RUN_ID = runId.toString()
          def status = 'RUNNING'
          timeout(time: 30, unit: 'MINUTES') {
            while (status == 'RUNNING') {
              sleep 10
              def resResp = sh(script: "curl -s ${ATP_BASE}/api/ci/result/${runId} -H 'X-CI-Token: ${ATP_TOKEN}'", returnStdout: true).trim()
              def res = new groovy.json.JsonSlurperClassic().parseText(resResp)
              status = res.status
              echo "status=${status} passed=${res.passed ?: 0} failed=${res.failed ?: 0}"
            }
          }
        }
      }
    }
    stage('Fetch JUnit report & publish') {
      steps {
        // 拉取 JUnit 格式报告（含用例级与每条步骤的断言明细），交给原生 junit 步骤渲染并判定红绿
        withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
          sh "curl -s ${ATP_BASE}/api/ci/report/${env.RUN_ID}.xml -H 'X-CI-Token: ${ATP_TOKEN}' -o atp-report.xml"
        }
        junit 'atp-report.xml'
      }
    }
  }
}