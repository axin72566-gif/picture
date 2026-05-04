package com.axin.picturebackend.model.dto.feed;

import lombok.Data;

import java.io.Serializable;

/**
 * Feed 流游标分页请求 DTO
 *
 * <p>使用时间戳游标翻页，避免传统页码翻页时因新数据插入导致的重复/遗漏问题。
 * <ul>
 *   <li>首次请求：cursor 不传（null），从最新数据开始</li>
 *   <li>后续翻页：cursor 传上次响应的 {@code nextCursor}，继续向更早数据翻页</li>
 * </ul>
 */
@Data
public class FeedQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间戳游标（毫秒）：首次为 null，后续传上次响应的 nextCursor
     */
    private Long cursor;

    /**
     * 每页返回条数，默认 20，最大 50
     */
    private Integer size = 20;
}
