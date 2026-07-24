package com.iot.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )
                .authorizeExchange(exchanges -> exchanges
                        // 0. BẮT BUỘC: Cho phép tất cả request OPTIONS (CORS Preflight) đi qua không cần Token
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 1. Dành cho Register: ADMIN và MANAGER có quyền tạo Staff
                        .pathMatchers("/api/v1/auth/register").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                        // 2. Endpoint Filter Sensor cho phép cả 3 role (Staff, Manager, Admin) dùng để Giám sát
                        .pathMatchers(HttpMethod.POST, "/api/v1/sensors/filter").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF")

                        // 3. Quyền XEM (GET): Cả STAFF, MANAGER, ADMIN đều xem/giám sát được
                        .pathMatchers(HttpMethod.GET, "/api/v1/stations/**", "/api/v1/sensors/**", "/api/v1/sensor-types/**", "/api/v1/gateways/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF")

                        // 4. Quyền THÊM/SỬA/XÓA (POST, PUT, DELETE): Chỉ ADMIN và MANAGER mới có quyền (STAFF KHÔNG CÓ QUYỀN)
                        .pathMatchers(HttpMethod.POST, "/api/v1/stations/**", "/api/v1/sensors/**", "/api/v1/sensor-types/**", "/api/v1/gateways/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/stations/**", "/api/v1/sensors/**", "/api/v1/sensor-types/**", "/api/v1/gateways/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/stations/**", "/api/v1/sensors/**", "/api/v1/sensor-types/**", "/api/v1/gateways/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                        // 5. Public APIs & User Service APIs
                        .pathMatchers("/eureka/**", "/actuator/**", "/api/v1/auth/**", "/ws/telemetry/**").permitAll()
                        .pathMatchers("/api/v1/user/**", "/api/v1/users/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF")

                        // 6. Chốt chặn cuối
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @SuppressWarnings("unchecked")
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();

            // 1. Lấy vai trò từ realm_access
            Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List) {
                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                for (String r : realmRoles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()));
                }
            }

            // 2. Lấy vai trò từ resource_access (mọi client)
            Map<String, Object> resourcesAccess = (Map<String, Object>) jwt.getClaims().get("resource_access");
            if (resourcesAccess != null) {
                for (Object clientObj : resourcesAccess.values()) {
                    if (clientObj instanceof Map) {
                        Map<String, Object> clientMap = (Map<String, Object>) clientObj;
                        if (clientMap.get("roles") instanceof List) {
                            List<String> clientRoles = (List<String>) clientMap.get("roles");
                            for (String r : clientRoles) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()));
                            }
                        }
                    }
                }
            }

            return authorities;
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}