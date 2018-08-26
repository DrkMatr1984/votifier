package com.vexsoftware.votifier;

import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.net.VoteReceiver;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.util.logging.Level;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Votifier
extends Plugin {
    private String version;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;
    private boolean debug;

    public void onEnable() {
        String listenerDirectory;
        File rsaDirectory;
        String hostAddr;
        Configuration cfg;
        block14 : {
            this.version = this.getDescription().getVersion();
            if (!this.getDataFolder().exists()) {
                this.getDataFolder().mkdir();
            }
            File config = new File(this.getDataFolder() + File.separator + "config.yml");
            boolean exist = config.exists();
            rsaDirectory = null;
            listenerDirectory = null;
            cfg = null;
            hostAddr = null;
            if (hostAddr == null || hostAddr.length() == 0) {
                hostAddr = "0.0.0.0";
            }
            try {
                if (!exist) {
                    config.createNewFile();
                }
                cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(config);
                rsaDirectory = new File(this.getDataFolder() + "/rsa");
                listenerDirectory = String.valueOf(this.getDataFolder().toString().replace("\\", "/")) + "/listeners";
                if (exist) break block14;
                try {
                    this.getLogger().info("Configuring Votifier for the first time...");
                    config.createNewFile();
                    cfg.set("host", (Object)hostAddr);
                    cfg.set("port", (Object)8192);
                    cfg.set("debug", (Object)false);
                    cfg.set("listener_folder", (Object)listenerDirectory);
                    ConfigurationProvider.getProvider(YamlConfiguration.class).save(cfg, config);
                }
                catch (Exception ex) {
                    this.getLogger().log(Level.SEVERE, "Error creating configuration file", ex);
                    this.gracefulExit();
                    return;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (!rsaDirectory.exists()) {
                rsaDirectory.mkdir();
                new File(listenerDirectory).mkdir();
                this.keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, this.keyPair);
            } else {
                this.keyPair = RSAIO.load(rsaDirectory);
            }
        }
        catch (Exception ex) {
            this.getLogger().log(Level.SEVERE, "Error reading configuration file or RSA keys", ex);
            this.gracefulExit();
            return;
        }
        String host = cfg.getString("host", hostAddr);
        int port = cfg.getInt("port", 8192);
        this.debug = cfg.getBoolean("debug", false);
        if (this.debug) {
            this.getLogger().info("DEBUG mode enabled!");
        }
        try {
            this.voteReceiver = new VoteReceiver(this, host, port);
            this.getProxy().getScheduler().runAsync((Plugin)this, (Runnable)this.voteReceiver);
            this.getLogger().info("Votifier enabled.");
        }
        catch (Exception ex) {
            this.gracefulExit();
            return;
        }
    }

    public void onDisable() {
        if (this.voteReceiver != null) {
            this.voteReceiver.shutdown();
        }
        this.getLogger().info("Votifier disabled.");
    }

    private void gracefulExit() {
        this.getLogger().log(Level.SEVERE, "Votifier did not initialize properly!");
    }

    public String getVersion() {
        return this.version;
    }

    public VoteReceiver getVoteReceiver() {
        return this.voteReceiver;
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public boolean isDebug() {
        return this.debug;
    }
}

