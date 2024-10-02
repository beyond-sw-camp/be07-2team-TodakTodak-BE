package com.padaks.todaktodak.chat.cs.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CsStatus {

    INPROCESS("INPROCESS","처리중"),
    COMPLETED("COMPLETED", "처리완료");

    private final String key;
    private final String value;
}
