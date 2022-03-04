/*
 * Copyright (c) 2022.
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

package self.me.matchday.plugin.datasource.parsing.fabric;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FabricLogger {

  @Pointcut(value = "execution(* Fabric.*(..))")
  private void logBlogger() {}

  @Before("logBlogger()")
  public void beforeLog() {
    System.out.println("Loggin' a method...");
  }

  @Around("@annotation(self.me.matchday.plugin.datasource.parsing.fabric.FabricLogger.Fabrique)")
  public Object logFabric(@NotNull ProceedingJoinPoint joinPoint) throws Throwable {

    System.out.println("Now we are loggin'!");
    final Object result = joinPoint.proceed();
    System.out.println("Now we are DONE loggin'!");
    return result;
  }

  public @interface Fabrique {}
}
