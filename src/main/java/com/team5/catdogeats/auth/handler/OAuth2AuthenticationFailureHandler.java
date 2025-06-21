package com.team5.catdogeats.auth.handler;

import com.team5.catdogeats.global.exception.WithdrawnAccountException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        /* 1) 탈퇴 계정 전용 분기 */
        if (exception instanceof WithdrawnAccountException) {
            log.info("탈퇴한 유저 로그인 시도 → /withdraw 리다이렉트");
            setDefaultFailureUrl("/withdraw?error=withdraw");
            super.onAuthenticationFailure(request, response, exception);
            return;
        }
//
//        /* 2) 기존 OAuth2 커스텀 코드 분기 유지 */
//        if (exception instanceof OAuth2AuthenticationException oauthEx) {
//            OAuth2Error error = oauthEx.getError();
//            if ("withdraw".equals(error.getErrorCode())) {
//                log.info("탈퇴한 유저(OAuth2Error) → /withdraw 리다이렉트");
//                setDefaultFailureUrl("/withdraw?error=withdraw");
//                super.onAuthenticationFailure(request, response, exception);
//                return;
//            }
//        }

        /* 3) 그 외 예외 */
        response.sendRedirect("/login?error=oauth2");
    }

}
