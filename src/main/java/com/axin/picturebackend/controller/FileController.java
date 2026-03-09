package com.axin.picturebackend.controller;


import com.axin.picturebackend.annotation.RoleCheck;
import com.axin.picturebackend.common.BaseResponse;
import com.axin.picturebackend.common.ResultUtils;
import com.axin.picturebackend.constant.UserConstant;
import com.axin.picturebackend.exception.BusinessException;
import com.axin.picturebackend.exception.ErrorCode;
import com.axin.picturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

	@Resource
	private CosManager cosManager;

	/**
	 * 文件上传
	 *
	 * @param multipartFile 前端上传的文件对象
	 * @return 上传后的文件路径
	 */
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	@PostMapping("/upload")
	public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
		// 4. 获取前端上传文件的原始文件名（如 "test.jpg"）
		String filename = multipartFile.getOriginalFilename();
		// 5. 拼接文件在 COS 中的存储路径（如 "/test/test.jpg"）
		String filepath = String.format("/test/%s", filename);
		File file = null;  // 声明临时文件对象，用于后续保存
		try {
			// 6. 核心行1：创建临时文件
			// - 第一个参数 filepath：临时文件的路径前缀（不是最终存储路径，仅用于标识）
			// - 第二个参数 null：临时文件的后缀（这里用null表示使用默认后缀）
			// - 作用：在系统临时目录创建一个临时文件，用于中转上传的文件内容
			file = File.createTempFile(filepath, null);

			// 7. 核心行2：将前端上传的 MultipartFile 写入临时文件
			// - transferTo：Spring 提供的方法，把内存中的文件流写入磁盘临时文件
			// - 这一步是为了获取一个可被 cosManager 读取的本地文件
			multipartFile.transferTo(file);

			// 8. 核心行3：调用 COS 管理器上传文件
			// - 第一个参数 filepath：文件在 COS 中的存储路径（如 "/test/test.jpg"）
			// - 第二个参数 file：要上传的本地临时文件
			// - 作用：把临时文件上传到腾讯云 COS 存储桶
			cosManager.putObject(filepath, file);

			// 9. 返回成功响应：将 COS 中的文件路径返回给前端
			return ResultUtils.success(filepath);
		} catch (Exception e) {
			// 10. 异常处理：记录错误日志，抛出业务异常（提示上传失败）
			log.error("file upload error, filepath = {}", filepath, e);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
		} finally {
			// 11. 核心行4：最终删除临时文件（无论上传成功/失败，都清理临时文件）
			if (file != null) {
				boolean delete = file.delete();
				// 如果删除失败，记录日志（避免临时文件堆积）
				if (!delete) {
					log.error("file delete error, filepath = {}", filepath);
				}
			}
		}
	}

	/**
	 * 文件下载
	 *
	 * @param filepath 文件路径
	 * @param response 响应对象
	 */
	@RoleCheck(mustRole = UserConstant.ADMIN_ROLE)
	@GetMapping("/download")
	public void downloadFile(String filepath, HttpServletResponse response) throws IOException {
		COSObjectInputStream cosObjectInput = null;
		ServletOutputStream outputStream = null;
		try {
			COSObject cosObject = cosManager.getObject(filepath);
			cosObjectInput = cosObject.getObjectContent();
			// 优化1：提取纯文件名（比如从 "/test/test.jpg" 提取 "test.jpg"）
			String filename = filepath.substring(filepath.lastIndexOf("/") + 1);

			// 设置响应头（优化文件名）
			response.setContentType("application/octet-stream;charset=UTF-8");
			// 优化2：处理中文文件名乱码
			String encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");

			// 优化3：流对流传输（边读边写，不占内存）
			outputStream = response.getOutputStream();
			byte[] buffer = new byte[1024 * 8]; // 8KB 缓冲区
			int len;
			// 循环读取 COS 流的内容，每次读8KB，写入响应流
			while ((len = cosObjectInput.read(buffer)) != -1) {
				outputStream.write(buffer, 0, len);
			}
			outputStream.flush();
		} catch (Exception e) {
			log.error("file download error, filepath = {}", filepath, e);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
		} finally {
			// 关闭所有资源
			if (cosObjectInput != null) {
				cosObjectInput.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}
}
