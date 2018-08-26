package com.vexsoftware.votifier.model;

import net.md_5.bungee.api.plugin.Event;

public class VotifierEvent
extends Event {
    private Vote vote;

    public VotifierEvent(Vote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return this.vote;
    }
}

