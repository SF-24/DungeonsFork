package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.Party;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PartyManager {

    private final Main plugin;
    private final Map<UUID, Party> parties;
    private final Map<UUID, UUID> playerPartyMap;
    private final Map<UUID, Map<UUID, Long>> invitations;

    private int maxPartySize;
    private int minPartySize;
    private long inviteTimeout;

    public PartyManager(Main plugin) {
        this.plugin = plugin;
        this.parties = new ConcurrentHashMap<>();
        this.playerPartyMap = new ConcurrentHashMap<>();
        this.invitations = new ConcurrentHashMap<>();

        loadConfiguration();
        loadPartiesFromDatabase();
        startInvitationCleanupTask();
    }

    private void loadConfiguration() {
        this.maxPartySize = plugin.getConfig().getInt("party.max-size", 5);
        this.minPartySize = plugin.getConfig().getInt("party.min-size", 1);
        this.inviteTimeout = plugin.getConfig().getLong("party.invite-timeout", 60) * 1000;

        plugin.getLogger().info("Party configuration loaded: " +
                "min=" + minPartySize +
                ", max=" + maxPartySize +
                ", inviteTimeout=" + (inviteTimeout / 1000) + "s");
    }

    private void loadPartiesFromDatabase() {
        List<Party> loadedParties = plugin.getDatabaseManager().loadAllParties();

        for (Party party : loadedParties) {
            parties.put(party.getPartyId(), party);

            for (UUID memberId : party.getMembers()) {
                playerPartyMap.put(memberId, party.getPartyId());
            }
        }

        plugin.getLogger().info("Loaded " + loadedParties.size() + " parties from database");
    }

    public Party createParty(Player leader) {
        UUID playerId = leader.getUniqueId();

        if (isInParty(playerId)) {
            return null;
        }

        Party party = new Party(playerId);
        parties.put(party.getPartyId(), party);
        playerPartyMap.put(playerId, party.getPartyId());

        plugin.getDatabaseManager().saveParty(party);

        plugin.getLogger().info("Party created by " + leader.getName() + " (ID: " + party.getPartyId() + ")");

        return party;
    }

    public boolean disbandParty(Party party) {
        if (party == null) return false;

        for (UUID memberId : party.getMembers()) {
            playerPartyMap.remove(memberId);
        }

        parties.remove(party.getPartyId());
        plugin.getDatabaseManager().deleteParty(party.getPartyId());

        plugin.getLogger().info("Party disbanded (ID: " + party.getPartyId() + ")");

        return true;
    }

    public boolean invitePlayer(Party party, Player inviter, Player target) {
        UUID targetId = target.getUniqueId();

        if (isInParty(targetId)) return false;
        if (party.getSize() >= maxPartySize) return false;
        if (hasInvitation(targetId, party.getPartyId())) return false;

        invitations.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>())
                .put(party.getPartyId(), System.currentTimeMillis());

        party.addInvitation(targetId);

        plugin.getLogger().info(inviter.getName() + " invited " + target.getName() + " to party " + party.getPartyId());

        return true;
    }

    public boolean acceptInvitation(Player player, UUID partyId) {
        UUID playerId = player.getUniqueId();

        if (isInParty(playerId)) return false;
        if (!hasInvitation(playerId, partyId)) return false;

        Party party = parties.get(partyId);
        if (party == null) {
            removeInvitation(playerId, partyId);
            return false;
        }

        if (party.getSize() >= maxPartySize) {
            removeInvitation(playerId, partyId);
            return false;
        }

        party.addMember(playerId);
        playerPartyMap.put(playerId, partyId);
        removeInvitation(playerId, partyId);

        plugin.getDatabaseManager().saveParty(party);

        plugin.getLogger().info(player.getName() + " joined party " + partyId);

        return true;
    }

    public boolean leaveParty(Player player) {
        UUID playerId = player.getUniqueId();
        Party party = getPlayerParty(playerId);

        if (party == null) return false;

        return removePlayerFromParty(playerId, party);
    }

    public boolean kickPlayer(Party party, UUID playerId) {
        if (party == null || !party.isMember(playerId)) return false;
        if (party.isLeader(playerId)) return false;

        return removePlayerFromParty(playerId, party);
    }

    private boolean removePlayerFromParty(UUID playerId, Party party) {
        party.removeMember(playerId);
        playerPartyMap.remove(playerId);

        if (party.isEmpty()) {
            disbandParty(party);
            return true;
        }

        if (party.isLeader(playerId) && !party.isEmpty()) {
            UUID newLeader = party.getMembers().iterator().next();
            party.promoteToLeader(newLeader);
            plugin.getLogger().info("Promoted new party leader: " + newLeader);
        }

        plugin.getDatabaseManager().saveParty(party);

        return true;
    }

    public boolean isInParty(UUID playerId) { return playerPartyMap.containsKey(playerId); }

    public Party getPlayerParty(UUID playerId) {
        UUID partyId = playerPartyMap.get(playerId);
        return partyId != null ? parties.get(partyId) : null;
    }

    public Party getParty(UUID partyId) { return parties.get(partyId); }

    public boolean hasInvitation(UUID playerId, UUID partyId) {
        Map<UUID, Long> playerInvites = invitations.get(playerId);
        if (playerInvites == null) return false;

        Long timestamp = playerInvites.get(partyId);
        if (timestamp == null) return false;

        if (System.currentTimeMillis() - timestamp > inviteTimeout) {
            playerInvites.remove(partyId);
            return false;
        }

        return true;
    }

    public void removeInvitation(UUID playerId, UUID partyId) {
        Map<UUID, Long> playerInvites = invitations.get(playerId);
        if (playerInvites != null) {
            playerInvites.remove(partyId);
            if (playerInvites.isEmpty()) {
                invitations.remove(playerId);
            }
        }
    }

    public Map<UUID, Long> getPlayerInvitations(UUID playerId) {
        Map<UUID, Long> playerInvites = invitations.get(playerId);
        return playerInvites != null ? new HashMap<>(playerInvites) : new HashMap<>();
    }

    public UUID getMostRecentInvitation(UUID playerId) {
        Map<UUID, Long> playerInvites = getPlayerInvitations(playerId);
        if (playerInvites.isEmpty()) return null;

        return playerInvites.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void startInvitationCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            AtomicInteger cleaned = new AtomicInteger();

            for (Map.Entry<UUID, Map<UUID, Long>> entry : invitations.entrySet()) {
                Map<UUID, Long> playerInvites = entry.getValue();

                playerInvites.entrySet().removeIf(invite -> {
                    if (now - invite.getValue() > inviteTimeout) {
                        cleaned.getAndIncrement();
                        return true;
                    }
                    return false;
                });

                if (playerInvites.isEmpty()) {
                    invitations.remove(entry.getKey());
                }
            }

            if (cleaned.get() > 0) {
                plugin.getLogger().fine("Cleaned up " + cleaned + " expired party invitations");
            }

        }, 20 * 60, 20 * 60);
    }

    public void saveAllParties() {
        for (Party party : parties.values()) {
            plugin.getDatabaseManager().saveParty(party);
        }
        plugin.getLogger().info("Saved " + parties.size() + " parties to database");
    }

    public Collection<Party> getAllParties() { return new ArrayList<>(parties.values()); }

    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_parties", parties.size());
        stats.put("total_players", playerPartyMap.size());
        stats.put("pending_invitations", invitations.values().stream()
                .mapToInt(Map::size)
                .sum());
        return stats;
    }

    public int getMaxPartySize() { return maxPartySize; }
    public int getMinPartySize() { return minPartySize; }
    public long getInviteTimeout() { return inviteTimeout; }
}
