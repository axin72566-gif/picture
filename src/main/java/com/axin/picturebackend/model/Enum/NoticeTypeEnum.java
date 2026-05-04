package com.axin.picturebackend.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
public enum NoticeTypeEnum {

    LIKE("点赞通知", "like"),
    COMMENT("评论通知", "comment"),
    SYSTEM_CLEAR("系统清理通知", "system_clear"),
    SYSTEM("系统公告", "system"),
    PICTURE_UPLOAD("图片上传通知", "picture_upload"),
    FOLLOW("关注通知", "follow");

    private final String text;
    private final String value;

    NoticeTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static NoticeTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (NoticeTypeEnum noticeTypeEnum : NoticeTypeEnum.values()) {
            if (noticeTypeEnum.value.equals(value)) {
                return noticeTypeEnum;
            }
        }
        return null;
    }
}
