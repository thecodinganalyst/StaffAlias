package com.thecodinganalyst.staffalias.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class WebCorsConfigurationTest {

    @Test
    void supportsSemicolonSeparatedProductionOrigins() {
        WebCorsConfiguration configuration = new WebCorsConfiguration();

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) configuration
                .corsConfigurationSource("https://staffalias.web.app; https://staffalias.firebaseapp.com");

        assertThat(source.getCorsConfigurations().get("/**").getAllowedOrigins())
                .containsExactly("https://staffalias.web.app", "https://staffalias.firebaseapp.com");
    }

    @Test
    void rejectsEmptyAllowedOrigins() {
        WebCorsConfiguration configuration = new WebCorsConfiguration();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> configuration.corsConfigurationSource(" ; "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS allowed origin");
    }
}
