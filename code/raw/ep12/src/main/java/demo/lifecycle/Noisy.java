package demo.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * One bean that takes part in every lifecycle hook Spring offers, so the real
 * order can be read off rather than argued about.
 */
public class Noisy implements BeanNameAware, ApplicationContextAware,
                              InitializingBean, DisposableBean {

    public Noisy() { Steps.record("constructor"); }

    @Autowired
    public void setDependency(Helper helper) { Steps.record("@Autowired setter, dependency injected"); }

    @Override public void setBeanName(String name) { Steps.record("BeanNameAware.setBeanName"); }

    @Override public void setApplicationContext(ApplicationContext ctx) {
        Steps.record("ApplicationContextAware.setApplicationContext");
    }

    @PostConstruct
    public void postConstruct() { Steps.record("@PostConstruct"); }

    @Override public void afterPropertiesSet() { Steps.record("InitializingBean.afterPropertiesSet"); }

    public void customInit() { Steps.record("custom init method from @Bean(initMethod)"); }

    @PreDestroy
    public void preDestroy() { Steps.record("@PreDestroy"); }

    @Override public void destroy() { Steps.record("DisposableBean.destroy"); }

    public void customDestroy() { Steps.record("custom destroy method from @Bean(destroyMethod)"); }
}
