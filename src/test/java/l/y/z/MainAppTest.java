package l.y.z;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * author: liuyazong datetime: 2018/10/29 上午10:22
 * <p>
 * <p>
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MainAppTest {
    @Test
    public void contextLoad() {
        C c = new C();
        c.test();
    }


    class A {
        public void test() {
            log.info("A: {}", this);
        }
    }

    class B extends A {
        public void test() {
            log.info("B: {}", this);
            super.test();
        }
    }

    class C extends B {
        public void test() {
            super.test();
            log.info("C: {}", this);
        }
    }
}
