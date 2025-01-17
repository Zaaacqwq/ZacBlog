package com.zaaac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author zaaac
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Zac Blog Started   ლ(´ڡ`ლ)ﾞ  \n" +
                " ________  ________  ________     \n" +
                "|\\_____  \\|\\   __  \\|\\   ____\\    \n" +
                " \\|___/  /\\ \\  \\|\\  \\ \\  \\___|    \n" +
                "     /  / /\\ \\   __  \\ \\  \\       \n" +
                "    /  /_/__\\ \\  \\ \\  \\ \\  \\____  \n" +
                "   |\\________\\ \\__\\ \\__\\ \\_______\\\n" +
                "    \\|_______|\\|__|\\|__|\\|_______|\n");
    }
}
