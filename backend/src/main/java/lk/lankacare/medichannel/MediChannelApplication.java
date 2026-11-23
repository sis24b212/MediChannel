package lk.lankacare.medichannel;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class MediChannelApplication {

    @PostConstruct
    public void initTimezone() {
        // BR-11: All hospital session and booking times use Asia/Colombo
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MediChannelApplication.class, args);
    }
}
