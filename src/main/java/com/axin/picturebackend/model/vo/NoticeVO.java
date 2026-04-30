package com.axin.picturebackend.model.vo;

import com.axin.picturebackend.model.entity.SysNotice;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知响应 VO
 */
@Data
public class NoticeVO implements Serializable {

    /**
     * 通知ID
     */
    private Long id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 关联业务ID（如图片ID、评论ID）
     */
    private Long relatedId;

    /**
     * 阅读状态：0-未读 1-已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 通知创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;

    /**
     * 将 SysNotice 转换为 NoticeVO
     */
    public static NoticeVO of(SysNotice sysNotice) {
        if (sysNotice == null) {
            return null;
        }
        NoticeVO noticeVO = new NoticeVO();
        BeanUtils.copyProperties(sysNotice, noticeVO);
        return noticeVO;
    }
}
