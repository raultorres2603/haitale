package ai.haitale;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HaitaleCommandTest {

    @Test
    public void testWithCommandLineOption() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{};
            PicocliRunner.run(HaitaleCommand.class, ctx, args);

            // Check that the main help is displayed
            String output = baos.toString();
            assertTrue(output.contains("HaiTale"));
            assertTrue(output.contains("recommend"));
        }
    }

    @Test
    public void testHelpOption() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"--help"};
            PicocliRunner.run(HaitaleCommand.class, ctx, args);

            // Check that help is displayed
            String output = baos.toString();
            assertTrue(output.contains("Usage:"));
        }
    }
}
