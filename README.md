# TetherThere Plugin

TetherThere is a Minecraft plugin that allows players to bind and unbind each other using a rope-like mechanic. With various configuration options, you can tweak the behavior of the plugin to suit your server's needs.

## Features:

- **Bind Players**: Bind two players together with a rope, allowing them to be tethered.
- **Unbind Players**: Unbind the rope between two players, releasing them from each other.
- **Cooldown Management**: Set cooldowns between binding and unbinding attempts to prevent spamming.
- **Debuffs**: Apply debuffs (blindness, slowness, weakness) to the tethered player, making the experience more challenging.
- **Tethering Range**: Control the distance within which players can be tethered.
- **Teleportation**: When tethered, the bound player will be pulled toward the tethering player at a defined distance and speed.

## Configuration:

Here are the key configuration options available:

- **tether_chance**: The chance of successfully tethering a player.
- **tether_cooldown_minutes**: Time between attempts to tether another player.
- **tether_max_distance**: The maximum distance for attempting to tether a player.
- **tether_duration_seconds**: Time it takes to bind a player.
- **tether_effect_duration_seconds**: Time the player remains tethered (excluding effects).
- **tether_pull_distance**: Distance the player will be pulled towards the tethering player.
- **tether_pull_speed**: Speed at which the player is pulled.
- **unbind_duration_seconds**: Time it takes to unbind a player.
- **binding_max_distance**: Maximum distance a tethered player can move before the tether is broken.
- **unbind_chance**: Chance of successfully unbinding a player.
- **debuffs**: List of debuffs applied to the tethered player (blindness, slowness, weakness).

## Commands:

The following commands are available in the plugin:

- `/tether bind <player1> <player2>`: Binds two players together with a rope.
- `/tether unbind <player1> <player2>`: Unbinds the rope between two players.
- `/tether resetbindcooldown`: Resets the cooldown, allowing players to attempt tethering again.
- `/tether reload`: Reloads the plugin's configuration.

## Installation:

1. Download the plugin .jar file.
2. Place it in your server's `plugins` folder.
3. Restart the server.
4. Configure the plugin settings in the `config.yml` file.
