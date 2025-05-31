# TempRankManager

A powerful Minecraft plugin for managing temporary ranks with flexible storage options and comprehensive features.

## 📋 Features

### 🎯 Core Functionality
- **Temporary Rank Assignment** - Give players ranks for specific durations
- **Flexible Time Format** - Support for seconds, minutes, hours, days, and months
- **Time Accumulation** - Add time to existing ranks instead of replacing them
- **Automatic Expiration** - Ranks automatically expire and revert to default group
- **Whitelist Mode Support** - Automatically pauses timers when server is in whitelist mode

### 💾 Storage Options
- **SQLite Database** (Recommended) - High-performance database storage with ACID transactions
- **YAML Files** - Simple file-based storage for smaller servers
- **Automatic Migration** - Easy switching between storage types

### ⚙️ Advanced Features
- **PlaceholderAPI Integration** - Rich placeholder support for other plugins
- **Tab Completion** - Smart autocomplete for commands, players, ranks, and time formats
- **Periodic Cleanup** - Automatic removal of expired ranks
- **Configurable Settings** - Extensive customization options
- **Pause/Resume System** - Intelligent timer management during maintenance

### 🔧 Admin Tools
- **Real-time Management** - Add, remove, and list temporary ranks
- **Comprehensive Logging** - Detailed logs for all rank operations
- **Permission Integration** - Full Vault compatibility with all permission plugins

## 📦 Installation

1. **Prerequisites:**
   - Minecraft server (Paper/Spigot 1.21+)
   - [Vault](https://www.spigotmc.org/resources/vault.34315/) plugin
   - A permission plugin (LuckPerms, PermissionsEx, etc.)
   - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional)

2. **Installation:**
   - Download the latest `TempRankManager.jar`
   - Place it in your server's `plugins` folder
   - Restart your server
   - Configure the plugin (see Configuration section)

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/temprank give <player> <rank> <time>` | Give a temporary rank | `temprankmanager.admin` |
| `/temprank remove <player>` | Remove a temporary rank | `temprankmanager.admin` |
| `/temprank list` | List all active temporary ranks | `temprankmanager.admin` |

### Time Format Examples
- `30s` - 30 seconds
- `5m` - 5 minutes  
- `2h` - 2 hours
- `7d` - 7 days
- `1mo` - 1 month (30 days)
- `90m` - 90 minutes
- `12h` - 12 hours

### Usage Examples
```
/temprank give Steve vip 30s      # VIP rank for 30 seconds
/temprank give Alex premium 2h    # Premium rank for 2 hours
/temprank give Bob moderator 7d   # Moderator rank for 7 days
/temprank give Charlie admin 1mo  # Admin rank for 1 month
/temprank remove Steve            # Remove Steve's temporary rank
/temprank list                    # Show all active temporary ranks
```

## ⚙️ Configuration

### config.yml
```yaml
# TempRankManager Configuration
storage:
  type: 'sqlite'  # Options: 'sqlite', 'yaml'
  
# SQLite settings (recommended)
sqlite:
  database-file: 'tempranks.db'
  
# YAML settings (for smaller servers)  
yaml:
  data-file: 'data.yml'

settings:
  # Clean up expired ranks on startup
  cleanup-on-startup: true
  
  # Periodic cleanup interval in minutes (0 to disable)
  cleanup-interval: 60
  
  # Default group when temporary ranks expire
  default-group: 'default'
  
  # Add time to existing ranks instead of replacing
  accumulate-time: true
```

## 🏷️ PlaceholderAPI Support

When PlaceholderAPI is installed, the following placeholders are available:

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%temprank_time_raw%` | Remaining time in milliseconds | `3600000` |
| `%temprank_time_formatted%` | Human-readable time remaining | `1h 30m` |
| `%temprank_rank%` | Current temporary rank name | `vip` |
| `%temprank_expires_at%` | Expiration timestamp | `1735689600000` |
| `%temprank_is_paused%` | Whether the rank is paused | `true` |
| `%temprank_has_temprank%` | Whether player has any temp rank | `true` |

### Integration Examples
```yaml
# Scoreboard
lines:
  - "Temp Rank: %temprank_rank%"
  - "Time Left: %temprank_time_formatted%"

# Chat format
format: "[%temprank_rank%] %player_name%: %message%"
```

## 💡 Key Features Explained

### Time Accumulation
When `accumulate-time: true`, giving the same rank to a player adds time instead of replacing:
```
/temprank give Steve vip 30s    # Steve gets VIP for 30 seconds
/temprank give Steve vip 30s    # Steve now has VIP for 60 seconds (30s + 30s)
/temprank give Steve vip 1m     # Steve now has VIP for 2 minutes (60s + 60s)
```

### Whitelist Mode Intelligence
- When server enters whitelist mode, all rank timers automatically pause
- When whitelist is disabled, timers resume from where they left off
- Prevents ranks from expiring during maintenance

### Storage Comparison

#### SQLite (Recommended)
✅ **Pros:**
- Superior performance with large player counts
- ACID transactions prevent data corruption
- Efficient concurrent access
- Built-in data integrity
- Automatic cleanup operations

#### YAML
✅ **Pros:**
- Human-readable format
- Easy manual editing
- Simple backup process

❌ **Cons:**
- Slower with many players
- Risk of corruption during server crashes
- Loads entire file into memory

## 🔧 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `temprankmanager.admin` | Access to all commands | OP |

## 🐛 Troubleshooting

### Common Issues

**Plugin won't start:**
- Ensure Vault is installed and a permission plugin is present
- Check server logs for specific error messages

**Placeholders not working:**
- Install PlaceholderAPI
- Restart the server after installing PlaceholderAPI
- Check that placeholders are correctly formatted

**Ranks not expiring:**
- Verify your permission plugin supports group removal
- Check if server is in whitelist mode (timers pause automatically)
- Review the periodic cleanup settings

**Performance issues:**
- Switch from YAML to SQLite storage
- Adjust cleanup interval in config
- Monitor server logs for any errors

## 📊 Technical Details

### Requirements
- **Java:** 21+
- **Minecraft:** 1.21+
- **Dependencies:** Vault (required), PlaceholderAPI (optional)

### Storage Performance
- **SQLite:** Handles 10,000+ players efficiently
- **YAML:** Recommended for <1,000 players
- **Memory Usage:** Minimal with SQLite, moderate with YAML

## 🤝 Support

For support, bug reports, or feature requests:
- Create an issue on the project repository
- Provide server logs and configuration files
- Include Minecraft version and plugin versions

## 📄 License

This plugin is licensed under [MIT License](LICENSE).

## 🏆 Credits

**Author:** ItzRenzo  
**Version:** 1.0  
**Built with:** Java 21, Maven, Paper API