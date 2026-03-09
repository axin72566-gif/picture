package com.axin.picturebackend.model.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {

	private List<String> tagList;

	private List<String> categoryList;
}
