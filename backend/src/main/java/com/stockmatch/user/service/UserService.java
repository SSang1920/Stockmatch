package com.stockmatch.user.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.user.domain.AlphaVantageKey;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.dto.UserInfoResponse;
import com.stockmatch.user.dto.UserProfileUpdateRequest;
import com.stockmatch.user.repository.AlphaVantageKeyRepository;
import com.stockmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AlphaVantageKeyRepository alphaVantageKeyRepository;
    private final OAuthUnlinkService oAuthUnlinkService;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AlphaVantageKey apikey = alphaVantageKeyRepository.findByUserId(userId).orElse(null);

        return new UserInfoResponse(user, apikey);
    }

    /**
     * 사용자 프로필 정보를 수정
     */
    @Transactional
    public void updateUserProfile(Long userId, UserProfileUpdateRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.getName());
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        oAuthUnlinkService.unlink(user);

        user.deactivate();
    }

    /**
     * AlphaVantageKey 등록,저장
     */
    @Transactional
    public void upsertAlphaVantageKey(Long userId, String newApiKey){

        //User 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<AlphaVantageKey> existingKeyOpt = alphaVantageKeyRepository.findByUserId(userId);

        // 새로운 키
        String keyCipher = newApiKey;
        String keyLast4 = newApiKey.length() > 4 ? newApiKey.substring(newApiKey.length() - 4) : newApiKey;

        if (existingKeyOpt.isPresent()) {
            // 기존 키가 있을시 내용 수정
            AlphaVantageKey existingKey = existingKeyOpt.get();
            existingKey.updateKey(keyCipher, keyLast4);
        } else {
            // 기존 키 X 생성"
            AlphaVantageKey newKey = AlphaVantageKey.builder()
                    .user(user)
                    .keyCipher(keyCipher)
                    .keyLast4(keyLast4)
                    .build();
            alphaVantageKeyRepository.save(newKey);
        }
    }
}
