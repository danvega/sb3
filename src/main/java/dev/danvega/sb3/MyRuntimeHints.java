package dev.danvega.sb3;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(MyRuntimeHints.ResourcesRegistrar.class)
@Configuration
public class MyRuntimeHints {

    static class ResourcesRegistrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            try {
                hints.reflection().registerType(TypeReference.of("org.springframework.data.domain.Unpaged"),
                        builder -> builder
                                .withMembers(MemberCategory.values()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
