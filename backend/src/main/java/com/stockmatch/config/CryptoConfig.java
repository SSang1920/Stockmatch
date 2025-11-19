package com.stockmatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class CryptoConfig {

    @Value("${encryption.secret-key}")
    private String secretKey;

    @Value("${encryption.salt}")
    private String salt;

    /**
     * TextEncryptor Bean을 생성 Spring 컨테이너에 등록
     * @return AES-256 을 사용한 TextEncryptor 객체
     */
    @Bean
    public TextEncryptor textEncryptor() {
        //AES-256 암호화를 위한 Encryptor 생성
        AesBytesEncryptor encryptor = new AesBytesEncryptor(secretKey, salt);

        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                // 텍스트를 암호화하여 byte[] 배열로 변환
                byte[] encryptedBytes = encryptor.encrypt(text.getBytes());
                //16진수 문자열로 변환
                return new String(Hex.encode(encryptedBytes));
            }

            @Override
            public String decrypt(String encryptedText) {
                //16진수 문자열을 byte[]배열로 변환
                byte[] decryptedBytes = Hex.decode(encryptedText);

                //원본으로 복호화
                return new String(encryptor.decrypt(decryptedBytes));
            }
        };
    }
}
