package com.atp.common.result;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页结果出参（公共层）
 *
 * <p>放在 common 而非某个 module，避免业务模块之间为了复用 VO 产生横向依赖。
 * 新建模块统一使用本类。
 */
@Data
@Builder
public class PageResult<T> {

    private List<T> records;

    private long total;

    private long page;

    private long size;
}
