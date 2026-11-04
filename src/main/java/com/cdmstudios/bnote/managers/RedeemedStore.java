package com.cdmstudios.bnote.managers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class RedeemedStore {

    private final File file;
    private final Set<String> redeemed = new HashSet<>();

    public RedeemedStore(File dataFolder) throws IOException {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, "redeemed.txt");
        if (file.exists()) {
            redeemed.addAll(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            redeemed.remove("");
        }
    }

    // Returns false if the id was already redeemed. Must be called from the main thread.
    public synchronized boolean redeem(String id) throws IOException {
        if (!redeemed.add(id)) return false;
        try {
            Files.writeString(file.toPath(), id + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            redeemed.remove(id);
            throw e;
        }
        return true;
    }

    // Reverts a redeem (e.g. the economy deposit failed). Rewrites the file without the id.
    public synchronized void unredeem(String id) throws IOException {
        if (!redeemed.remove(id)) return;
        try {
            Files.write(file.toPath(), redeemed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            redeemed.add(id);
            throw e;
        }
    }

    public synchronized int size() {
        return redeemed.size();
    }
}
