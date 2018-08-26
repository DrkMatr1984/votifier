package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.crypto.RSA;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.crypto.BadPaddingException;
import net.md_5.bungee.api.plugin.Event;

public class VoteReceiver
implements Runnable {
    private final Votifier plugin;
    private final String host;
    private final int port;
    private ServerSocket server;
    private boolean running = true;

    public VoteReceiver(Votifier plugin, String host, int port) throws Exception {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.initialize();
    }

    private void initialize() throws Exception {
        try {
            this.server = new ServerSocket();
            this.server.bind(new InetSocketAddress(this.host, this.port));
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Error initializing vote receiver. Please verify that the configured");
            this.plugin.getLogger().log(Level.SEVERE, "IP address and port are not already in use. This is a common problem");
            this.plugin.getLogger().log(Level.SEVERE, "with hosting services and, if so, you should check with your hosting provider.", ex);
            throw new Exception(ex);
        }
    }

    public void shutdown() {
        this.running = false;
        if (this.server == null) {
            return;
        }
        try {
            this.server.close();
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Unable to shut down vote receiver cleanly.");
        }
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                Socket socket = this.server.accept();
                socket.setSoTimeout(5000);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                InputStream in = socket.getInputStream();
                writer.write("VOTIFIER " + this.plugin.getVersion());
                writer.newLine();
                writer.flush();
                byte[] block = new byte[256];
                in.read(block, 0, block.length);
                block = RSA.decrypt(block, this.plugin.getKeyPair().getPrivate());
                int position = 0;
                String opcode = this.readString(block, position);
                position += opcode.length() + 1;
                if (!opcode.equals("VOTE")) {
                    throw new Exception("Unable to decode RSA");
                }
                String serviceName = this.readString(block, position);
                String username = this.readString(block, position += serviceName.length() + 1);
                String address = this.readString(block, position += username.length() + 1);
                String timeStamp = this.readString(block, position += address.length() + 1);
                position += timeStamp.length() + 1;
                final Vote vote = new Vote();
                vote.setServiceName(serviceName);
                vote.setUsername(username);
                vote.setAddress(address);
                vote.setTimeStamp(timeStamp);
                if (this.plugin.isDebug()) {
                    this.plugin.getLogger().info("Received vote record -> " + vote);
                }
                this.plugin.getProxy().getScheduler().schedule(this.plugin, new Runnable() {

                    @Override
                    public void run() {
                        VoteReceiver.this.plugin.getProxy().getPluginManager().callEvent((Event)new VotifierEvent(vote));
                    }
                }, 1L, TimeUnit.MILLISECONDS);
                writer.close();
                in.close();
                socket.close();
            }
            catch (SocketException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Protocol error. Ignoring packet - " + ex.getLocalizedMessage());
            }
            catch (BadPaddingException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Unable to decrypt vote record. Make sure that that your public key");
                this.plugin.getLogger().log(Level.WARNING, "matches the one you gave the server list.", ex);
            }
            catch (Exception ex) {
                this.plugin.getLogger().log(Level.WARNING, "Exception caught while receiving a vote notification", ex);
            }
        }
    }

    private String readString(byte[] data, int offset) {
        StringBuilder builder = new StringBuilder();
        int i = offset;
        while (i < data.length) {
            if (data[i] == 10) break;
            builder.append((char)data[i]);
            ++i;
        }
        return builder.toString();
    }

}

