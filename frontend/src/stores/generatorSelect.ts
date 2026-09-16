/**
 * 生成器选择返回 store。
 * 组件编辑页从「其他类型·生成随机数」卡片跳转生成器管理页（mode=select），
 * 在生成器页"选择"某生成器后，将种子写入本 store 并跳回组件列表页，
 * 由 base/component/index.vue 读取并进入编辑器、写入一条 stepType=3 步骤。
 */
import { defineStore } from 'pinia'
import type { GeneratorStepSeed } from '@/api/types'

export const useGeneratorSelectStore = defineStore('generatorSelect', {
  state: () => ({
    seed: null as GeneratorStepSeed | null
  }),
  actions: {
    setSeed(s: GeneratorStepSeed) {
      this.seed = s
    },
    clear() {
      this.seed = null
    }
  }
})
