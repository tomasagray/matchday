/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

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
