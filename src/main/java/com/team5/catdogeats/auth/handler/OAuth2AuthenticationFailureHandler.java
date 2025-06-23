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


        boolean withdrawn = exception instanceof WithdrawnAccountException
                || exception.getCause() instanceof WithdrawnAccountException;

        if (withdrawn) {
            String url = request.getContextPath() + "/withdraw?error=withdraw";
            getRedirectStrategy().sendRedirect(request, response, url);
            return;
        }

        /* 3) 그 외 예외 */
        response.sendRedirect("/login?error=oauth2");
    }

}
