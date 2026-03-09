package com.axin.picturebackend.model.dto.picture;


import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

	private static final long serialVersionUID = -1L;

	/**
	 * 图片id
	 */
	private Long id;

	/**
	 * 图片地址
	 */
	private String fileUrl;

	/**
	 * 图片名称
	 */
	private String picName;

	/**
	 * 空间 id
	 */
	private Long spaceId;

}
