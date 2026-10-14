package coffee.axle.coffeeclient.util;

import coffee.axle.coffeeclient.CoffeeClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SocialList {

    public static final SocialList friends = new SocialList("friends.txt");
    public static final SocialList enemies = new SocialList("enemies.txt");

    private final ArrayList<String> entries = new ArrayList<>();
    private final File file;

    private SocialList(String fileName) {
        this.file = new File("./config/CoffeeClient/", fileName);
    }

    public void load() {
        if (!this.file.exists()) {
            try {
                this.file.getParentFile().mkdirs();
                this.file.createNewFile();
            } catch (Exception e) {
                CoffeeClient.LOGGER.error("Failed to create " + this.file.getName(), e);
            }
            return;
        }
        this.entries.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
            this.entries.addAll(reader.lines().map(String::trim).collect(Collectors.toList()));
        } catch (Exception e) {
            CoffeeClient.LOGGER.error("Failed to load " + this.file.getName(), e);
        }
    }

    public void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(this.file))) {
            writer.print(String.join("\n", this.entries));
        } catch (Exception e) {
            CoffeeClient.LOGGER.error("Failed to save " + this.file.getName(), e);
        }
    }

    public String add(String name) {
        if (contains(name)) {
            return null;
        }
        this.entries.add(name);
        save();
        return name;
    }

    public String remove(String name) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.entries.get(i).equalsIgnoreCase(name)) {
                String entry = this.entries.remove(i);
                save();
                return entry;
            }
        }
        return null;
    }

    public void clear() {
        this.entries.clear();
        save();
    }

    public boolean contains(String name) {
        return this.entries.stream().anyMatch(entry -> entry.equalsIgnoreCase(name));
    }

    public ArrayList<String> entries() {
        return this.entries;
    }
}
