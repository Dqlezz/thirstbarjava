package com.haroldstudios.thirstbar.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.haroldstudios.thirstbar.ThirstBar;

public class PacketListener {

    ThirstBar plugin;

    public PacketListener(ThirstBar thirstBar) {
        this.plugin = thirstBar;
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin, PacketType.Play.Server.UPDATE_HEALTH) {
            public void onPacketSending(PacketEvent event) {
                if (!thirstBar.canDisableHealthPacket()) return;
                event.setCancelled(true);
            }
        });
    }

}
