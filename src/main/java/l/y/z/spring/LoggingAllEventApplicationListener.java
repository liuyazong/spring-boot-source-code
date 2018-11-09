package l.y.z.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

@Slf4j
public class LoggingAllEventApplicationListener implements ApplicationListener, Ordered {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        log.info("event: {}", event);
    }

    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER+1;
    }
}
