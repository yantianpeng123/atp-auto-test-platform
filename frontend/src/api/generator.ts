/**
 * 数据生成器 - 前端接口层（**前端先行，后端接口待补**）。
 *
 * 说明：当前后端 /api/base/generator/** 尚未实现，这里用内存 mock + 客户端生成器运行时
 * 让前端流程（列表 / 新增 / 试生成 / 选择）可独立运行。后端就绪后将下列函数替换为
 * request.get/post/put/delete 调用即可，入参/出参结构保持不变。
 */
import type {
  DataGeneratorInfo,
  DataGeneratorQuery,
  DataGeneratorSaveParams,
  GeneratorFunctionInfo,
  GeneratorPreviewParams,
  GeneratorType,
  PageResult
} from './types'

/* ============================ 客户端生成器运行时 ============================ */

const CHARSETS: Record<string, string> = {
  digits: '0123456789',
  alpha: 'abcdefghijklmnopqrstuvwxyz',
  alnum: '0123456789abcdefghijklmnopqrstuvwxyz'
}

const SURNAMES = '赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜'
const GIVEN = '伟芳娜秀英敏静丽强磊军洋勇艳杰娟涛明超霞平刚桂兰'

function randInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}
function pick<T>(arr: T[]): T {
  return arr[randInt(0, arr.length - 1)]
}
function randomString(len: number, charset: string): string {
  let s = ''
  for (let i = 0; i < len; i++) s += charset[randInt(0, charset.length - 1)]
  return s
}
function genPhone(): string {
  const prefixes = ['133', '135', '136', '137', '138', '139', '150', '151', '152', '158', '159', '182', '183', '186', '188', '189']
  return pick(prefixes) + randomString(8, CHARSETS.digits)
}
function genIdCard(): string {
  const regions = ['110101', '110105', '310115', '440305', '510104', '320106']
  const region = pick(regions)
  const year = randInt(1960, 2005)
  const month = String(randInt(1, 12)).padStart(2, '0')
  const day = String(randInt(1, 28)).padStart(2, '0')
  const seq = randomString(3, CHARSETS.digits)
  const body = `${region}${year}${month}${day}${seq}`
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checkCodes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']
  let sum = 0
  for (let i = 0; i < 17; i++) sum += Number(body[i]) * weights[i]
  return body + checkCodes[sum % 11]
}
function genName(): string {
  return pick(SURNAMES.split('')) + pick(GIVEN.split(''))
}
function genUuid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}
function genTimestamp(format?: string, offset?: string): string {
  const now = new Date()
  if (offset) {
    const m = /^([+-])(\d+)([dhm])$/.exec(offset)
    if (m) {
      const sign = m[1] === '-' ? -1 : 1
      const val = Number(m[2])
      const unit = m[3]
      const ms = unit === 'd' ? 86400000 : unit === 'h' ? 3600000 : 60000
      now.setTime(now.getTime() + sign * val * ms)
    }
  }
  if (!format || format === 'ms') return String(now.getTime())
  const p = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}${p(now.getMonth() + 1)}${p(now.getDate())} ${p(now.getHours())}:${p(now.getMinutes())}:${p(now.getSeconds())}`
}

function callFunc(name: string, rawArgs: string): string {
  const args = rawArgs
    .split(',')
    .map((a) => a.trim())
    .filter((a) => a.length > 0)
  switch (name) {
    case 'randomInt':
      return String(randInt(Number(args[0] ?? 0), Number(args[1] ?? 9999)))
    case 'randomFloat':
      return (randInt(Number(args[0] ?? 0), Number(args[1] ?? 1) * 100) / 100).toFixed(Number(args[2] ?? 2))
    case 'randomString':
      return randomString(Number(args[0] ?? 8), CHARSETS[String(args[1] ?? 'alnum')] ?? CHARSETS.alnum)
    case 'uuid':
      return genUuid()
    case 'phone':
      return genPhone()
    case 'idCard':
      return genIdCard()
    case 'name':
      return genName()
    case 'enum':
      return args.length ? pick(args) : ''
    case 'timestamp':
      return genTimestamp(args[0], args[1])
    default:
      return `\${${name}(${rawArgs})}`
  }
}

/** 从 CUSTOM 模板中提取 ${func(args)} 并逐个生成替换 */
export function genFromTemplate(template: string): string {
  return template.replace(/\$\{([a-zA-Z_]\w*)\s*\(([^}]*)\)\}/g, (_m, name, rawArgs) => callFunc(name, rawArgs))
}

/** 按生成器类型 + 参数生成一个值（供试生成 / 预览） */
export function genValue(type: GeneratorType, params: Record<string, unknown>): string {
  switch (type) {
    case 'RANDOM': {
      const len = Number(params.length ?? 8)
      const charset = CHARSETS[String(params.charset ?? 'digits')] ?? CHARSETS.digits
      const prefix = String(params.prefix ?? '')
      const suffix = String(params.suffix ?? '')
      return prefix + randomString(len, charset) + suffix
    }
    case 'PHONE':
      return genPhone()
    case 'IDCARD':
      return genIdCard()
    case 'NAME':
      return genName()
    case 'UUID':
      return genUuid()
    case 'ENUM': {
      const list = Array.isArray(params.values)
        ? (params.values as unknown[])
        : String(params.values ?? '')
            .split(/[,，]/)
            .map((s) => s.trim())
            .filter(Boolean)
      return list.length ? String(pick(list)) : ''
    }
    case 'TIMESTAMP':
      return genTimestamp(String(params.format ?? ''), String(params.offset ?? ''))
    case 'CUSTOM':
      return genFromTemplate(String(params.template ?? ''))
    default:
      return ''
  }
}

/* ============================ 内存 Mock 数据 ============================ */

let mockSeq = 100
const mockStore: DataGeneratorInfo[] = [
  {
    id: 1,
    projectId: 0,
    name: '默认手机号',
    type: 'PHONE',
    params: {},
    description: '生成合法 11 位手机号',
    createTime: '2026-09-16 09:00:00'
  },
  {
    id: 2,
    projectId: 0,
    name: '默认身份证号',
    type: 'IDCARD',
    params: {},
    description: '带 GB11643 校验位的 18 位身份证',
    createTime: '2026-09-16 09:01:00'
  },
  {
    id: 3,
    projectId: 0,
    name: '8 位纯数字订单号',
    type: 'RANDOM',
    params: { length: 8, charset: 'digits', prefix: '', suffix: '' },
    description: '固定长度 8 位随机数',
    createTime: '2026-09-16 09:02:00'
  },
  {
    id: 4,
    projectId: 0,
    name: '随机姓名',
    type: 'NAME',
    params: {},
    description: '随机中文姓名',
    createTime: '2026-09-16 09:03:00'
  }
]

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), 150))
}

/** 分页查询（按 name / type 过滤） */
export async function getGeneratorList(query: DataGeneratorQuery): Promise<PageResult<DataGeneratorInfo>> {
  const all = mockStore.filter((g) => {
    if (query.name && !g.name.includes(query.name)) return false
    if (query.type && g.type !== query.type) return false
    return true
  })
  const page = query.page ?? 1
  const size = query.size ?? 10
  const start = (page - 1) * size
  const records = all.slice(start, start + size)
  return delay({ records, total: all.length, page, size })
}

/** 新增生成器 */
export async function createGenerator(data: DataGeneratorSaveParams): Promise<DataGeneratorInfo> {
  const info: DataGeneratorInfo = {
    id: ++mockSeq,
    projectId: data.projectId,
    name: data.name,
    type: data.type,
    params: data.params,
    description: data.description ?? null,
    createTime: new Date().toLocaleString('sv-SE').replace('T', ' ')
  }
  mockStore.unshift(info)
  return delay(info)
}

/** 编辑生成器 */
export async function updateGenerator(data: DataGeneratorSaveParams): Promise<DataGeneratorInfo> {
  const info = mockStore.find((g) => g.id === data.id)
  if (info) {
    info.name = data.name
    info.type = data.type
    info.params = data.params
    info.description = data.description ?? null
  }
  return delay(info as DataGeneratorInfo)
}

/** 删除生成器（逻辑删除，前端 mock 直接移除） */
export async function deleteGenerator(id: number): Promise<void> {
  const idx = mockStore.findIndex((g) => g.id === id)
  if (idx >= 0) mockStore.splice(idx, 1)
  return delay(undefined)
}

/** 可用函数清单（供 CUSTOM 模板语法提示） */
export function getGeneratorFunctions(): Promise<GeneratorFunctionInfo[]> {
  const list: GeneratorFunctionInfo[] = [
    { name: 'randomInt', args: '(min,max)', desc: '范围内整数', example: '${randomInt(1000,9999)}' },
    { name: 'randomFloat', args: '(min,max[,scale])', desc: '浮点数', example: '${randomFloat(0,1,2)}' },
    { name: 'randomString', args: '(len,charset)', desc: '定长随机串；charset=digits/alpha/alnum', example: '${randomString(8,alnum)}' },
    { name: 'uuid', args: '()', desc: 'UUID', example: '${uuid()}' },
    { name: 'phone', args: '()', desc: '合法 11 位手机号', example: '${phone()}' },
    { name: 'idCard', args: '()', desc: '带 GB11643 校验位的 18 位身份证', example: '${idCard()}' },
    { name: 'name', args: '()', desc: '随机中文姓名', example: '${name()}' },
    { name: 'enum', args: '(a,b,c...)', desc: '从候选集随机取一个', example: '${enum(A,B,C)}' },
    { name: 'timestamp', args: '([format,offset])', desc: '当前时间；format 默认 ms；offset 如 +1d', example: '${timestamp(yyyyMMdd)}' }
  ]
  return delay(list)
}

/** 试生成（前端本地直接调用生成器运行时，无需后端） */
export function previewGenerator(payload: GeneratorPreviewParams): Promise<{ result: string }> {
  const result = genValue(payload.type, payload.params)
  return delay({ result })
}
