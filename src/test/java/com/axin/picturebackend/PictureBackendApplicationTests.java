package com.axin.picturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

@SpringBootTest
class PictureBackendApplicationTests {

	/** 密码加密盐值 */
	private static final String SALT = "axin";

	/** 管理员创建用户时的默认密码 */
	private static final String DEFAULT_PASSWORD = "12345678";

	@Test
	void password() {
		System.out.println(DigestUtils.md5DigestAsHex((SALT + DEFAULT_PASSWORD).getBytes()));
	}

}
