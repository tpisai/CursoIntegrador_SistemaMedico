package com.cis.citas_salud.api.seguridad;

import com.cis.citas_salud.api.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Permite declarar {@code UsuarioSesion sesion} como parámetro de un endpoint:
 * lee "Authorization: Bearer <token>" y responde 401 si falta o no es válido.
 */
@Component
public class SesionArgumentResolver implements HandlerMethodArgumentResolver {

    private final TokenService tokenService;

    public SesionArgumentResolver(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UsuarioSesion.class);
    }

    @Override
    public UsuarioSesion resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                         NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String cabecera = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String token = cabecera != null && cabecera.startsWith("Bearer ") ? cabecera.substring(7) : null;
        return tokenService.validar(token)
                .orElseThrow(() -> ApiException.noAutorizado("Tu sesión expiró. Vuelve a iniciar sesión."));
    }
}
