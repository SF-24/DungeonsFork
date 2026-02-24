package dev.bekololek.dungeons.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Party {

    private final UUID partyId;
    private UUID leaderId;
    private final Set<UUID> members;
    private final Map<UUID, Long> invitations;
    private final long createdAt;

    public Party(UUID leaderId) {
        this.partyId = UUID.randomUUID();
        this.leaderId = leaderId;
        this.members = new HashSet<>();
        this.members.add(leaderId);
        this.invitations = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Party(UUID partyId, UUID leaderId, Set<UUID> members, long createdAt) {
        this.partyId = partyId;
        this.leaderId = leaderId;
        this.members = members;
        this.invitations = new HashMap<>();
        this.createdAt = createdAt;
    }

    public UUID getPartyId() { return partyId; }
    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public boolean isMember(UUID playerId) { return members.contains(playerId); }
    public boolean isLeader(UUID playerId) { return leaderId.equals(playerId); }

    public void addMember(UUID playerId) {
        members.add(playerId);
        invitations.remove(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public int getSize() { return members.size(); }
    public boolean isEmpty() { return members.isEmpty(); }

    public void addInvitation(UUID playerId) {
        invitations.put(playerId, System.currentTimeMillis());
    }

    public void removeInvitation(UUID playerId) {
        invitations.remove(playerId);
    }

    public boolean hasInvitation(UUID playerId) {
        return invitations.containsKey(playerId);
    }

    public Map<UUID, Long> getInvitations() {
        return new HashMap<>(invitations);
    }

    public void clearExpiredInvitations(long timeout) {
        long now = System.currentTimeMillis();
        invitations.entrySet().removeIf(entry -> now - entry.getValue() > timeout * 1000);
    }

    public long getCreatedAt() { return createdAt; }

    public void promoteToLeader(UUID playerId) {
        if (isMember(playerId)) {
            this.leaderId = playerId;
        }
    }

    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Party party = (Party) o;
        return Objects.equals(partyId, party.partyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partyId);
    }

    @Override
    public String toString() {
        return "Party{partyId=" + partyId + ", leaderId=" + leaderId + ", members=" + members.size() + "}";
    }
}
