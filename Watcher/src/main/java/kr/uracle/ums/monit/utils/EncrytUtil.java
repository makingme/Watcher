package kr.uracle.ums.monit.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import kr.msp.util.AES128Cipher;

@Component
public class EncrytUtil {
	
	private static final Logger log = LoggerFactory.getLogger(EncrytUtil.class);
	
	private final static AES128Cipher tool =  AES128Cipher.getInstance();
	
	public static String encrypt(String plainText) {
		String encryptText = "";
		if(StringUtils.isNotEmpty(plainText)) {
			try {
				encryptText = tool.AES_Encode(plainText);
			} catch (Exception e) {
				log.error("{} 문자열 암호화중 에러 발생", plainText);
				return encryptText;
			}
		}
		return encryptText;
	}
	
	public static String decrypt(String encryptText) {
		String decryptText ="";
		if(StringUtils.isNotEmpty(encryptText)) {
			try {
				decryptText = tool.AES_Decode(encryptText);
			} catch (Exception e) {
				log.error("{} 문자열 암호화중 에러 발생", encryptText);
				return decryptText;
			}
		}
		return decryptText;
	}
	
	public static void main(String[] args) {
		if(args.length <2) {
			log.error("암/복호화할 데이터(2개)를 입력바랍니다. - 타입 암보호화문자열");
			System.exit(-1);
		}
		String type = args[0];
		if(type.equals("ENC")||type.equals("DEC")) {
			if(type.equals("ENC")) log.info("암호화 데이터 - [{}]", encrypt(args[1]));
			if(type.equals("DEC")) log.info(" 복호화 데이터 - [{}]", decrypt(args[1]));
		}else {
			log.error("첫번째({}) 타입 지정 값이 올바르지 않습니다.(ENC/DEC 지원)", type);
			System.exit(-1);
		}
	}
}
