package coffee.axle.coffeeclient.command.commands;

import coffee.axle.coffeeclient.CoffeeClient;
import coffee.axle.coffeeclient.command.Command;
import coffee.axle.coffeeclient.util.ChatUtil;
import coffee.axle.coffeeclient.util.SocialList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class EnemyCommand extends Command {

    public EnemyCommand() {
        super(new ArrayList<>(Arrays.asList("enemy", "e", "target")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            showUsage(args.get(0));
            return;
        }
        switch (args.get(1).toLowerCase(Locale.ROOT)) {
            case "add":
                if (args.size() < 3) {
                    showNameUsage(args.get(0), "add");
                    return;
                }
                for (String name : args.subList(2, args.size())) {
                    String added = SocialList.enemies.add(name);
                    if (added == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is already in your enemy list&r",
                                CoffeeClient.CLIENT_NAME, name));
                    } else {
                        ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your enemy list&r",
                                CoffeeClient.CLIENT_NAME, added));
                    }
                }
                return;
            case "remove":
                if (args.size() < 3) {
                    showNameUsage(args.get(0), "remove");
                    return;
                }
                for (String name : args.subList(2, args.size())) {
                    String removed = SocialList.enemies.remove(name);
                    if (removed == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is not in your enemy list&r",
                                CoffeeClient.CLIENT_NAME, name));
                    } else {
                        ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your enemy list&r",
                                CoffeeClient.CLIENT_NAME, removed));
                    }
                }
                return;
            case "list":
                ArrayList<String> entries = SocialList.enemies.entries();
                if (entries.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%sNo enemies&r", CoffeeClient.CLIENT_NAME));
                    return;
                }
                ChatUtil.sendFormatted(String.format("%sEnemys:&r", CoffeeClient.CLIENT_NAME));
                for (String entry : entries) {
                    ChatUtil.sendFormatted(String.format("   &o%s&r", entry));
                }
                return;
            case "clear":
                SocialList.enemies.clear();
                ChatUtil.sendFormatted(String.format("%sCleared your enemy list&r", CoffeeClient.CLIENT_NAME));
                return;
            default:
                showUsage(args.get(0));
        }
    }

    private void showUsage(String alias) {
        ChatUtil.sendFormatted(String.format(
                "%sUsage: .%s <&oadd&r/&oremove&r/&olist&r/&oclear&r>&r",
                CoffeeClient.CLIENT_NAME, alias.toLowerCase(Locale.ROOT)));
    }

    private void showNameUsage(String alias, String action) {
        ChatUtil.sendFormatted(String.format(
                "%sUsage: .%s %s <&oname&r> [&oname&r]...&r",
                CoffeeClient.CLIENT_NAME, alias.toLowerCase(Locale.ROOT), action));
    }
}
