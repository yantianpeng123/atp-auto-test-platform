package com.atp.module.base.generator;

import com.atp.common.exception.BizException;
import com.atp.common.result.ResultCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成器函数注册表（白名单）。
 *
 * <p>表达式中只有本枚举登记过的函数名才会被执行，未知函数直接抛错，
 * 天然杜绝任意代码执行（本工程<strong>不使用</strong> eval / ScriptEngine / GroovyShell，解析器为手写）。
 *
 * <p>后续新增函数只需在此枚举加一个常量，前端通过 {@code /api/base/generator/functions} 自动感知。
 */
public enum GeneratorFunc {

    RANDOM_INT("randomInt", "min,max", "范围内随机整数", "${randomInt(1000,9999)}") {
        @Override
        public String apply(List<String> args) {
            long min = 0;
            long max = 9999;
            if (args.size() == 1) {
                max = num(args.get(0), "max");
            } else if (args.size() >= 2) {
                min = num(args.get(0), "min");
                max = num(args.get(1), "max");
            }
            assertRange(min, max, "randomInt");
            // 用 double 计算区间宽度：min、max 取极值时 (max - min + 1) 在 long 下会溢出成负数，
            // 导致返回值落在区间之外（例如 randomInt(0,9223372036854775807) 会返回负数）。
            double span = (double) max - (double) min + 1.0;
            if (span > Long.MAX_VALUE) {
                throw new BizException(ResultCode.BAD_REQUEST,
                        "randomInt 的取值范围过大：[" + min + "," + max + "]");
            }
            return String.valueOf(min + (long) (random().nextDouble() * span));
        }
    },

    RANDOM_FLOAT("randomFloat", "min,max[,scale]", "范围内随机浮点数", "${randomFloat(0,1,2)}") {
        @Override
        public String apply(List<String> args) {
            double min = 0;
            double max = 1;
            int scale = 2;
            if (args.size() == 1) {
                max = Double.parseDouble(args.get(0));
            } else if (args.size() >= 2) {
                min = Double.parseDouble(args.get(0));
                max = Double.parseDouble(args.get(1));
            }
            if (args.size() >= 3) {
                scale = (int) num(args.get(2), "scale");
                if (scale < 0 || scale > 10) {
                    throw new BizException(ResultCode.BAD_REQUEST, "randomFloat 的 scale 需在 0~10 之间");
                }
            }
            if (min > max) {
                throw new BizException(ResultCode.BAD_REQUEST, "randomFloat 要求 min <= max");
            }
            double value = min + random().nextDouble() * (max - min);
            return String.format(Locale.ROOT, "%." + scale + "f", value);
        }
    },

    RANDOM_STRING("randomString", "len[,charset]", "定长随机串，charset 可选 digits/alpha/alnum",
            "${randomString(8,alnum)}") {
        @Override
        public String apply(List<String> args) {
            int len = args.isEmpty() ? 8 : (int) num(args.get(0), "len");
            // 防 ${randomString(99999999)} 爆内存
            if (len <= 0 || len > MAX_STRING_LENGTH) {
                throw new BizException(ResultCode.BAD_REQUEST,
                        "randomString 的 len 需在 1~" + MAX_STRING_LENGTH + " 之间");
            }
            String charset = args.size() >= 2 ? args.get(1) : "alnum";
            String pool = switch (charset.toLowerCase(Locale.ROOT)) {
                case "digits" -> DIGITS;
                case "alpha" -> ALPHA;
                case "alnum" -> ALNUM;
                default -> throw new BizException(ResultCode.BAD_REQUEST,
                        "randomString 的 charset 只支持 digits/alpha/alnum，当前为 " + charset);
            };
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(pool.charAt(random().nextInt(pool.length())));
            }
            return sb.toString();
        }
    },

    UUID_FUNC("uuid", "", "UUID", "${uuid()}") {
        @Override
        public String apply(List<String> args) {
            return UUID.randomUUID().toString();
        }
    },

    PHONE("phone", "", "合法 11 位手机号", "${phone()}") {
        @Override
        public String apply(List<String> args) {
            // 号段表与前端 api/generator.ts 保持一致，避免阶段 2 换成真实接口后预览结果突变
            StringBuilder sb = new StringBuilder(PHONE_PREFIXES[random().nextInt(PHONE_PREFIXES.length)]);
            for (int i = 0; i < 8; i++) {
                sb.append(random().nextInt(10));
            }
            return sb.toString();
        }
    },

    ID_CARD("idCard", "[region,birth,gender]", "带 GB11643 校验位的 18 位身份证", "${idCard()}") {
        @Override
        public String apply(List<String> args) {
            String region = args.size() >= 1 && !args.get(0).isEmpty() ? args.get(0) : "110101";
            String birth = args.size() >= 2 && !args.get(1).isEmpty() ? args.get(1) : randomBirth();
            int seq = random().nextInt(1000);
            if (args.size() >= 3 && !args.get(2).isEmpty()) {
                String gender = args.get(2).toLowerCase(Locale.ROOT);
                boolean male = gender.startsWith("m") || gender.contains("男");
                seq = seq / 2 * 2 + (male ? 1 : 0);
                if (seq == 0) {
                    seq = male ? 1 : 2;
                }
            }
            String body = region + birth + String.format(Locale.ROOT, "%03d", seq);
            if (body.length() != 17) {
                throw new BizException(ResultCode.BAD_REQUEST,
                        "idCard 参数拼出的前 17 位长度必须为 17（region 6 位 + 出生日期 8 位 + 顺序码 3 位），"
                                + "当前为 " + body.length() + " 位：" + body);
            }
            // 校验位算法逐字符取 digit，若前 17 位混入非数字字符会算出错误的校验位却不报错，
            // 从而产出看似合法、实际校验不通过的号码。这里显式拦住。
            for (int i = 0; i < body.length(); i++) {
                if (!Character.isDigit(body.charAt(i))) {
                    throw new BizException(ResultCode.BAD_REQUEST,
                            "idCard 的前 17 位必须全为数字（region 6 位 + 出生日期 8 位 + 顺序码 3 位），当前为：" + body);
                }
            }
            return body + idCardCheckBit(body);
        }
    },

    NAME("name", "", "随机中文姓名", "${name()}") {
        @Override
        public String apply(List<String> args) {
            String surname = String.valueOf(SURNAMES.charAt(random().nextInt(SURNAMES.length())));
            int givenLen = random().nextInt(2) + 1;
            StringBuilder sb = new StringBuilder(surname);
            for (int i = 0; i < givenLen; i++) {
                sb.append(GIVEN.charAt(random().nextInt(GIVEN.length())));
            }
            return sb.toString();
        }
    },

    ENUM_FUNC("enum", "a,b,c...", "从候选集中随机取一个", "${enum(A,B,C)}") {
        @Override
        public String apply(List<String> args) {
            if (args.isEmpty()) {
                throw new BizException(ResultCode.BAD_REQUEST, "enum 至少需要 1 个候选值");
            }
            return args.get(random().nextInt(args.size()));
        }
    },

    TIMESTAMP("timestamp", "[format,offset]", "当前时间戳；format 默认毫秒，offset 如 +1d",
            "${timestamp(yyyyMMdd)}") {
        @Override
        public String apply(List<String> args) {
            String format = args.size() >= 1 ? args.get(0) : "";
            String offset = args.size() >= 2 ? args.get(1) : "";
            LocalDateTime time = LocalDateTime.now().plusSeconds(parseOffsetSeconds(offset));
            if (format.isEmpty()) {
                return String.valueOf(time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            try {
                return time.format(DateTimeFormatter.ofPattern(format, Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BizException(ResultCode.BAD_REQUEST, "timestamp 的 format 不合法：" + format);
            }
        }
    };

    /** 随机串长度上限，防内存爆炸 */
    public static final int MAX_STRING_LENGTH = 64;

    /**
     * 取当前线程的随机源。
     *
     * <p><strong>不要</strong>把 {@link ThreadLocalRandom#current()} 缓存成静态字段：它除了返回单例，
     * 还会顺带为<em>当前线程</em>初始化 probe 与随机 seed（{@code localInit()}）。若缓存后改由其他线程
     * 直接调用，这些线程会跳过初始化——seed 从 0 起步、probe 始终为 0。写成
     * {@code ThreadLocalRandom.current().nextX()} 才是 JDK 的推荐用法，开销只是一次线程字段读取。
     */
    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    private static final String DIGITS = "0123456789";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALNUM = DIGITS + ALPHA;
    /** 手机号号段，与前端 api/generator.ts 保持一致 */
    private static final String[] PHONE_PREFIXES = {"133", "135", "136", "137", "138", "139", "150", "151",
            "152", "158", "159", "182", "183", "186", "188", "189"};
    private static final String SURNAMES = "王李张刘陈杨黄赵吴周徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭曾肖田董袁潘于蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤";
    private static final String GIVEN = "伟芳娜秀英敏静丽强磊洋艳勇军杰娟涛明超秀霞平刚桂英文军建华志远晓梅雪蕾晨曦子轩浩然梓涵雨欣思远嘉怡宇航若曦";
    private static final int[] ID_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final String ID_CHECK_CODES = "10X98765432";

    private final String funcName;
    private final String argsDoc;
    private final String desc;
    private final String example;

    GeneratorFunc(String funcName, String argsDoc, String desc, String example) {
        this.funcName = funcName;
        this.argsDoc = argsDoc;
        this.desc = desc;
        this.example = example;
    }

    /** 表达式中使用的函数名 */
    public String funcName() {
        return funcName;
    }

    public String argsDoc() {
        return argsDoc;
    }

    public String desc() {
        return desc;
    }

    public String example() {
        return example;
    }

    /** 执行函数；args 已由解析器完成嵌套解析、trim 与去引号 */
    public abstract String apply(List<String> args);

    static long num(String raw, String field) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "参数 " + field + " 必须是数字，当前为：" + raw);
        }
    }

    private static void assertRange(long min, long max, String func) {
        if (min > max) {
            throw new BizException(ResultCode.BAD_REQUEST, func + " 要求 min <= max");
        }
    }

    private static String randomBirth() {
        // 1960~2005，与前端 api/generator.ts 保持一致
        int year = 1960 + random().nextInt(46);
        LocalDate date = LocalDate.of(year, 1, 1).plusDays(random().nextInt(365));
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT));
    }

    /** GB11643 校验位 */
    private static String idCardCheckBit(String body17) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (body17.charAt(i) - '0') * ID_WEIGHTS[i];
        }
        return String.valueOf(ID_CHECK_CODES.charAt(sum % 11));
    }

    /** 解析 +1d / -2h / +30m / +10s 之类偏移，返回秒 */
    private static long parseOffsetSeconds(String offset) {
        if (offset == null || offset.isBlank()) {
            return 0;
        }
        String raw = offset.trim();
        int sign = 1;
        if (raw.startsWith("+")) {
            raw = raw.substring(1);
        } else if (raw.startsWith("-")) {
            sign = -1;
            raw = raw.substring(1);
        }
        if (raw.isEmpty()) {
            return 0;
        }
        char unit = raw.charAt(raw.length() - 1);
        String numPart = Character.isDigit(unit) ? raw : raw.substring(0, raw.length() - 1);
        if (numPart.isEmpty()) {
            return 0;
        }
        long value = num(numPart, "offset");
        long factor = switch (Character.toLowerCase(unit)) {
            case 'd' -> 86400L;
            case 'h' -> 3600L;
            case 'm' -> 60L;
            default -> 1L;
        };
        return sign * value * factor;
    }
}
