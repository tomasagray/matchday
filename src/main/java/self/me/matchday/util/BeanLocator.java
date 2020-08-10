package self.me.matchday.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Provides Spring managed Beans to non-Spring managed classes
 */
@Component
public class BeanLocator implements ApplicationContextAware {

  private static ApplicationContext CONTEXT;

  @Override
  public void setApplicationContext(@NotNull final ApplicationContext context)
      throws BeansException {

    // Application context injected by Spring during startup
    CONTEXT = context;
  }

  public static <T> T getBean(Class<T> beanClass) {
    return
        CONTEXT.getBean(beanClass);
  }
}
