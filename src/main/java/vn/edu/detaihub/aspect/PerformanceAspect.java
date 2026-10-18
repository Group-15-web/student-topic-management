package vn.edu.detaihub.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {
  @Around(
      "execution(public * vn.edu.detaihub.service.*.*(..)) && !within(vn.edu.detaihub.service.AuditService)")
  public Object measure(ProceedingJoinPoint jp) throws Throwable {
    long start = System.nanoTime();
    boolean success = false;
    try {
      var result = jp.proceed();
      success = true;
      return result;
    } finally {
      LoggerFactory.getLogger(getClass())
          .debug(
              "Service {} returned={} durationMs={}",
              jp.getSignature().toShortString(),
              success,
              (System.nanoTime() - start) / 1000000);
    }
  }
}
