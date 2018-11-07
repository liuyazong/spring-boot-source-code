package l.y.z.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * author: liuyazong
 * datetime: 2018/10/30 下午8:48
 * <p></p>
 */
@Service
//@Lazy
public class ServiceA {
    @Autowired
    private ServiceB serviceB;
}
