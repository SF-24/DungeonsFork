# Dungeons

A Minecraft Paper plugin for grid-based multiplayer dungeon instances with party management, quests, triggers, custom mobs, rewards, and schematic-based level design.

**Paper 1.21.4+ | Java 21**

## Features

- **Instanced Dungeons** — Grid-based system spawns isolated dungeon copies in a void world. Up to 100 concurrent instances. Schematics pasted via WorldEdit.
- **Party System** — 1–5 player parties with invite, kick, disband, friendly fire toggle, and shared rewards.
- **Quest Chains** — Per-dungeon objectives: kill mobs, kill bosses, collect items, reach locations, survive time, interact with blocks.
- **Trigger System** — Event-driven dungeon mechanics: spawn mobs, drop items, teleport players, send messages, apply effects — triggered by location, timer, kills, quest completion, or player death.
- **Custom Mobs** — Create and configure custom mobs through the in-game editor.
- **Reward Tables** — Configurable item, money, and XP rewards per dungeon with party sharing.
- **In-Game Editor** — Full dungeon creation and editing without touching config files. Manage quests, rewards, triggers, mobs, and schematics live.
- **Cooldowns & Time Limits** — Per-player cooldowns and per-dungeon time limits with admin bypass.
- **Death Handling** — Configurable respawn-in-dungeon or kick-on-death, max death limits, keep inventory toggle.
- **Economy Integration** — Entry costs and money rewards via Vault.
- **WorldGuard Protection** — Auto-created regions with configurable flags (block break/place, explosions, PvP, mob spawning, etc.).
- **Statistics & Leaderboards** — Tracks completions, playtime, deaths, quests completed, mobs killed, fastest completion.
- **PlaceholderAPI Support** — Exposes all stats as placeholders.
- **SQLite / MySQL** — Persistent storage with HikariCP connection pooling.

## Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/dungeon list` | List available dungeons | `dungeons.enter` |
| `/dungeon info <id>` | View dungeon details | `dungeons.enter` |
| `/dungeon join <id>` | Start dungeon (party leader) | `dungeons.enter` |
| `/dungeon leave` | Exit current dungeon | `dungeons.enter` |
| `/dungeon stats [player\|top]` | View statistics | `dungeons.stats` |

### Party Commands (`/dngparty` or `/p`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/p create` | Create a party | `dungeons.party` |
| `/p invite <player>` | Invite a player | `dungeons.party` |
| `/p accept` / `decline` | Respond to invitation | `dungeons.party` |
| `/p kick <player>` | Kick a member (leader) | `dungeons.party` |
| `/p leave` | Leave your party | `dungeons.party` |
| `/p disband` | Disband party (leader) | `dungeons.party` |
| `/p list` | Show party members | `dungeons.party` |

### Admin Commands (`/dngadmin` or `/da`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/da reload` | Reload all configs | `dungeons.admin.reload` |
| `/da reset <player\|dungeon>` | Reset cooldowns/data | `dungeons.admin.reset` |
| `/da list` | List active instances | `dungeons.admin` |
| `/da teleport <id>` | Spectate an instance | `dungeons.admin.teleport` |
| `/da stats` | Plugin-wide statistics | `dungeons.admin` |
| `/da cleanup` | Force cleanup all instances | `dungeons.admin` |

### Editor Commands (`/dng`)

| Command | Description |
|---------|-------------|
| `/dng create <id>` | Create a new dungeon |
| `/dng edit <id>` | Edit existing dungeon |
| `/dng save` / `cancel` | Save or discard changes |
| `/dng set <property> <value>` | Set dungeon properties |
| `/dng add quest\|reward\|trigger` | Add quest/reward/trigger |
| `/dng quest`, `/dng reward`, `/dng trigger` | Manage sub-elements |
| `/dng mobs create\|edit\|remove\|list` | Custom mob management |
| `/dng schematic paste\|confirm\|pos1\|pos2\|save` | Schematic operations |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `dungeons.enter` | Enter any dungeon | Everyone |
| `dungeons.enter.<id>` | Enter a specific dungeon | Everyone |
| `dungeons.party` | Use party commands | Everyone |
| `dungeons.editor` | In-game dungeon editor | OP |
| `dungeons.admin` | Admin commands | OP |
| `dungeons.admin.reload` | Reload configuration | OP |
| `dungeons.admin.reset` | Reset player/dungeon data | OP |
| `dungeons.admin.teleport` | Teleport to instances | OP |
| `dungeons.bypass.cooldown` | Bypass dungeon cooldowns | OP |
| `dungeons.bypass.party-size` | Bypass party size limits | OP |
| `dungeons.bypass.time-limit` | Ignore time limits | OP |
| `dungeons.stats` | View own statistics | Everyone |
| `dungeons.stats.others` | View other players' stats | OP |

## Configuration

### Grid System
```yaml
grid:
  mode: "auto"              # auto (void world) or manual (existing world)
  auto:
    world-name: "dungeon_world"
  slot-size: 256
  padding: 100
  base-y-level: 100
  max-slots: 100
```

### Instance Settings
```yaml
instance:
  max-duration: 1800         # 30 minutes
  cooldown-per-player: 3600  # 1 hour
  keep-inventory: false
  respawn-in-dungeon: true
  max-deaths: 0              # 0 = unlimited
  post-completion:
    duration: 30             # seconds before teleport out
    kill-mobs: true
```

### Party Settings
```yaml
party:
  min-size: 1
  max-size: 5
  friendly-fire: false
  share-rewards: true
  invite-timeout: 60
```

### Database
```yaml
database:
  type: sqlite               # sqlite or mysql
  sqlite:
    file: "data.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "dungeons"
    username: "root"
    password: "password"
```

## Quest Types

| Type | Description |
|------|-------------|
| `KILL_MOBS` | Kill a number of mobs |
| `KILL_BOSS` | Kill a boss mob |
| `COLLECT_ITEMS` | Collect specific items |
| `REACH_LOCATION` | Reach a target area |
| `SURVIVE_TIME` | Survive for a duration |
| `INTERACT_BLOCKS` | Interact with specific blocks |

## Trigger Types

| Trigger | Actions |
|---------|---------|
| `LOCATION` | Fired when player enters area |
| `TIMER` | Fired after elapsed time |
| `MOB_KILL` | Fired on mob death |
| `QUEST_COMPLETE` | Fired on quest completion |
| `PLAYER_DEATH` | Fired when player dies |
| `BOSS_KILL` | Fired on boss death |

Available actions: `SPAWN_MOB`, `DROP_ITEM`, `TELEPORT`, `DAMAGE_PLAYER`, `MESSAGE`, `COMMAND`, `POTION_EFFECT`.

## Dependencies

| Dependency | Required |
|------------|----------|
| [VersionAdapter](https://github.com/BekoLolek/VersionAdapter) | Yes |
| [WorldEdit](https://enginehub.org/worldedit) | No (required for schematics) |
| [WorldGuard](https://enginehub.org/worldguard) | No (required for region protection) |
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | No (required for economy) |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | No |

## Installation

1. Place `Dungeons.v1-BL.jar` in your server's `plugins/` folder.
2. Ensure `VersionAdapter.jar` is also in the `plugins/` folder.
3. (Optional) Add WorldEdit, WorldGuard, and Vault for full functionality.
4. Restart the server.
5. Use `/dng create <id>` to start building dungeons in-game.

## Part of the BekoLolek Plugin Ecosystem

Built by **Lolek** for the BekoLolek Minecraft network.
