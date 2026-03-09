package com.axin.picturebackend.model.dto.spaceuser;

import com.axin.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserQueryRequest extends PageRequest implements Serializable {

	/**
	 * ID
	 */
	private Long id;

	/**
	 * 空间 ID
	 */
	private Long spaceId;

	/**
	 * 用户 ID
	 */
	private Long userId;

	/**
	 * 空间角色：viewer/editor/admin
	 */
	private String spaceRole;

	private static final long serialVersionUID = 1L;
}

