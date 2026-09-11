package com.atp.common.exception;

import com.atp.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：可预期的错误，由全局异常处理器转为友好提示
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }
}
