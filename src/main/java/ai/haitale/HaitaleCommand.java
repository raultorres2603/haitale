package ai.haitale;

import ai.haitale.commands.InstallCommand;
import ai.haitale.commands.ListCommand;
import ai.haitale.commands.RecommendCommand;
import ai.haitale.commands.SearchCommand;
import io.micronaut.configuration.picocli.PicocliRunner;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "haitale",
         description = "AI-powered HyTale mod installer - Describe your world, get mod recommendations",
         mixinStandardHelpOptions = true,
         version = "HaiTale 0.1",
         subcommands = {
             RecommendCommand.class,
             InstallCommand.class,
             SearchCommand.class,
             ListCommand.class
         })
public class HaitaleCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    boolean verbose;

    public static void main(String[] args) {
        PicocliRunner.run(HaitaleCommand.class, args);
    }

    public void run() {
        System.out.println("HaiTale - AI-Powered HyTale Mod Installer");
        System.out.println("=========================================");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  recommend  - Get AI-powered mod recommendations");
        System.out.println("  install    - Install recommended mods");
        System.out.println("  search     - Search for mods by keyword");
        System.out.println("  list       - List installed mods");
        System.out.println();
        System.out.println("Use 'haitale <command> --help' for more information on a command.");
    }
}
