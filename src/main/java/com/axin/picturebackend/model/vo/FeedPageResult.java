package com.axin.picturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Feed 流游标分页结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本页 Feed 条目列表
     */
    private List<FeedVO> records;

    /**
     * 下一页游标（时间戳毫秒），null 表示已无更多数据
     */
    private Long nextCursor;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
}
