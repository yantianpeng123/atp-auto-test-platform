pipeline {
    agent any
    parameters {
        string(name: 'ATP_BASE_URL', defaultValue: 'http://localhost:5173/api', description: 'ATP 平台 API 基址，如 http://192.168.1.10:8080/api')
        string(name: 'PROJECT_ID', defaultValue: '3', description: 'ATP 项目 ID')
        string(name: 'BATCH_ID', defaultValue: '2', description: '批次 ID（留空则使用平台默认批次）')
        string(name: 'ENV_ID', defaultValue: '', description: '环境 ID（留空则使用平台默认环境）')
    }
    stages {
        stage('Trigger ATP Test') {
            steps {
                withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
                    script {
                        def body = [ projectId: params.PROJECT_ID, batchId: params.BATCH_ID ?: null, envId: params.ENV_ID ?: null ]
                        def resp = httpRequest(
                            url: "${params.ATP_BASE_URL}/ci/trigger",
                            httpMode: 'POST',
                            contentType: 'APPLICATION_JSON',
                            customHeaders: [[name: 'X-CI-Token', value: env.ATP_TOKEN]],
                            body: groovy.json.JsonOutput.toJson(body)
                        )
                        def json = new groovy.json.JsonSlurperClassic().parseText(resp.content)
                        env.RUN_ID = json.data.runId.toString()
                        echo "已触发运行 runId=${env.RUN_ID}，轮询地址：${params.ATP_BASE_URL}${json.data.statusUrl}"
                    }
                }
            }
        }
        stage('Wait & Report') {
            steps {
                withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
                    script {
                        def status = 'RUNNING'
                        def maxRetry = 180
                        for (int i = 0; i < maxRetry && status == 'RUNNING'; i++) {
                            sleep(time: 10, unit: 'SECONDS')
                            def content = httpRequest(
                                url: "${params.ATP_BASE_URL}/ci/result/${env.RUN_ID}",
                                httpMode: 'GET',
                                customHeaders: [[name: 'X-CI-Token', value: env.ATP_TOKEN]]
                            ).content
                            def json = new groovy.json.JsonSlurperClassic().parseText(content)
                            status = json.data.status
                            echo "轮询 #${i + 1}: status=${status} passed=${json.data.passed} failed=${json.data.failed}"
                        }
                        if (status == 'RUNNING') {
                            error("CI 执行超时未完成，请到平台查看 runId=${env.RUN_ID}")
                        }
                        // 拉取 JUnit 报告并交给 Jenkins 原生 junit 步骤渲染（含用例级与步骤断言明细）
                        httpRequest(
                            url: "${params.ATP_BASE_URL}/ci/report/${env.RUN_ID}.xml",
                            httpMode: 'GET',
                            customHeaders: [[name: 'X-CI-Token', value: env.ATP_TOKEN]],
                            outputFile: 'atp-report.xml'
                        )
                    }
                }
                // 原生 junit 步骤：自动渲染测试报告、判定红绿（无需手写轮询判定）
                junit 'atp-report.xml'
            }
        }
    }
}