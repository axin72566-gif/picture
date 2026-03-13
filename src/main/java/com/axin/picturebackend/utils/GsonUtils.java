package com.axin.picturebackend.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Gson 全局工具类（单例模式，避免重复创建 Gson 实例）
 */
public class GsonUtils {
	// 私有化构造器，禁止外部实例化
	private GsonUtils() {}

	// 全局默认 Gson 实例（基础配置：自定义日期格式、支持空值）
	private static final Gson DEFAULT_GSON = new GsonBuilder()
			.setDateFormat("yyyy-MM-dd HH:mm:ss") // 统一日期格式
			.serializeNulls() // 序列化时保留空值（接口返回更友好）
			.disableHtmlEscaping() // 禁用 HTML 转义（避免特殊字符如中文被转义）
			.create();

	// 对外提供获取默认 Gson 实例的方法
	public static Gson getDefaultGson() {
		return DEFAULT_GSON;
	}

	// -------------------------- 封装常用方法，简化调用 --------------------------
	/**
	 * 对象转 JSON 字符串
	 */
	public static String toJson(Object obj) {
		return DEFAULT_GSON.toJson(obj);
	}

	/**
	 * JSON 字符串转单个对象
	 */
	public static <T> T fromJson(String jsonStr, Class<T> clazz) {
		return DEFAULT_GSON.fromJson(jsonStr, clazz);
	}

	/**
	 * JSON 字符串转泛型集合（如 List<User>、Map<String, Object>）
	 */
	public static <T> T fromJson(String jsonStr, Type type) {
		return DEFAULT_GSON.fromJson(jsonStr, type);
	}

	// 快捷方法：转 List 集合
	public static <T> List<T> toList(String jsonStr, Class<T> clazz) {
		Type type = TypeToken.getParameterized(List.class, clazz).getType();
		return fromJson(jsonStr, type);
	}

	// 快捷方法：转 Map 集合
	public static Map<String, Object> toMap(String jsonStr) {
		Type type = new TypeToken<Map<String, Object>>() {}.getType();
		return fromJson(jsonStr, type);
	}
}
