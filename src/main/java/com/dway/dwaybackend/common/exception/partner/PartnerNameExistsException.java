package com.dway.dwaybackend.common.exception.partner;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PartnerNameExistsException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.PARTNER_NAME_EXISTS;
    public PartnerNameExistsException() { super(ErrorCode.PARTNER_NAME_EXISTS.getMessage()); }
}