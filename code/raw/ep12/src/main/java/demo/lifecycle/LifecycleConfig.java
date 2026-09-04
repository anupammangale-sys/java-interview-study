package demo.lifecycle;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LifecycleConfig {

    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public Noisy noisy() { return new Noisy(); }
}
