package com.dway.dwaybackend.common.exception.partner;

import com.dway.dwaybackend.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public class PartnerNotFoundException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.PARTNER_NOT_FOUND;
    public PartnerNotFoundException() { super(ErrorCode.PARTNER_NOT_FOUND.getMessage()); }
}