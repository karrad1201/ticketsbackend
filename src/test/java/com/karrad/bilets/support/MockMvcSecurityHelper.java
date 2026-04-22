package com.karrad.bilets.support;

import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

/**
 * Kotlin 2.x конфликтует с именем `apply` при вызове Java generic-метода
 * DefaultMockMvcBuilder.apply(MockMvcConfigurer). Этот хелпер решает проблему,
 * инкапсулируя вызов в Java где конфликта нет.
 */
public class MockMvcSecurityHelper {
    public static DefaultMockMvcBuilder withSpringSecurity(DefaultMockMvcBuilder builder) {
        return builder.apply(SecurityMockMvcConfigurers.springSecurity());
    }
}
