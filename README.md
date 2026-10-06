# Player Monitor

A Fabric server administration and anti-cheat monitoring mod for Minecraft 1.21.1.

Player Monitor collects a diagnostic snapshot from every connecting client and lets an
authorized administrator review it and run controlled remote actions through a GUI.

## Modules

The project ships two jars:

| Jar | Mod id | Where to install | Purpose |
| --- | --- | --- | --- |
| `playermonitor-<version>.jar` | `playermonitor` | Server and every client | Data collection, networking, commands, remote actions |
| `playermonitor-<version>-GUI.jar` | `playermonitor-gui` | Administrator client only | Review GUI, requires the core mod |

The core mod is mandatory on the server and on each client. The GUI mod is a separate
client-only companion that administrators install in addition to the core mod.

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16 or newer
- Fabric API
- Cloth Config
- Java 21

## Features

### Client data collection

The client uploads a chunked report on join and on request. Collection is guarded
per section; restricted platforms report an error entry instead of failing.

- Installed and loaded mods (id, name, version)
- Enabled resource packs
- Launcher brand and version
- JVM version, JVM arguments and the full process command line
- Operating system, architecture, timezone and locale
- CPU model and load, GPU renderer and vendor, memory usage
- Game settings: render distance, FOV, gamma, GUI scale, graphics mode, volume, language
- Shader stack detection (Iris, OptiFine, Oculus) and the active shader pack when available
- Account username, UUID and skin URL
- Real-time FPS, framebuffer and window size
- Key bindings
- Cheat client signature matching against known mod ids and names
- Running process list (optional)
- Clipboard contents (optional)

### Server-side remote actions

Client actions, delivered over `s2c` and executed locally:

- Close Minecraft
- Shut down the system
- Disconnect to title
- F3 debug overlay, F3+B hitboxes, F3+G chunk borders
- Simulate a key press
- Show a message or title
- Send a chat message or command as the player
- Reload resources
- Toggle fullscreen
- Set render distance
- Take a screenshot and upload it back to the server
- Force a client crash

Server actions, executed directly by the server:

- Teleport to coordinates
- Set health, food level, XP level
- Set the game mode

### Administrator GUI

- The server opens the GUI on a successful `/playermonitor verify`
- Player list with a detail view per player
- Flattened report viewer
- Live screenshot preview
- Buttons for every remote action, with an argument prompt when required
- Cloth Config settings screen

## Commands

| Command | Access | Description |
| --- | --- | --- |
| `/playermonitor setpassword <password>` | Console only | Store the SHA-256 hash of the verification password |
| `/playermonitor verify <password>` | Player with permission level 4 | Verify and open the GUI on this client |
| `/playermonitor <player> info` | Console only | Print the latest report for a player to the server log |
| `/pmonitor ...` | Alias | Short alias of `/playermonitor` |

## Installation

Server:

1. Install Fabric Loader for Minecraft 1.21.1.
2. Put Fabric API, Cloth Config and `playermonitor-<version>.jar` in the `mods` folder.
3. Start the server once, then set the password from the console:

```text
/playermonitor setpassword <your-password>
```

Client:

1. Put Fabric API, Cloth Config and `playermonitor-<version>.jar` in the `mods` folder.
2. The server rejects clients that do not have the core mod unless `requireClientMod` is disabled.

Administrator client, in addition:

1. Put `playermonitor-<version>-GUI.jar` in the `mods` folder.
2. Join the server, then run `/playermonitor verify <password>` to open the monitor GUI.

## Configuration

The server writes `config/playermonitor.json`.

| Key | Default | Description |
| --- | --- | --- |
| `passwordHash` | `""` | SHA-256 hex digest of the verification password |
| `requireAuthForActions` | `true` | Reject remote actions from unverified administrators |
| `requireClientMod` | `true` | Disconnect clients that do not have the mod |
| `missingModMessage` | see file | Message shown to a rejected client |
| `reportOnJoin` | `true` | Request a report automatically when a player joins |
| `collectProcesses` | `true` | Include the running process list |
| `collectClipboard` | `true` | Include the clipboard contents |
| `maxProcessCount` | `200` | Maximum number of processes to report |
| `maskPersonalData` | `false` | Replace the home directory inside report strings |

The administrator GUI writes `config/playermonitor-gui.json` and can also be edited
in game through its Cloth Config screen.

## Building

The GitHub Actions pipeline builds both jars. Local builds use the Gradle wrapper-less
setup and require JDK 21:

```bash
# Build both modules with an explicit version
gradle build -Pmod_version=26.1-Beta
```

Outputs:

- `core/build/libs/playermonitor-<version>.jar`
- `gui/build/libs/playermonitor-<version>-GUI.jar`

## Versioning and CI

The version scheme is `<YY>.<build>-<Beta|Release>`, for example `26.1-Beta` or
`26.1-Release`. The build number counts releases published in the current year.

Workflows:

- `Test` runs automatically on every push to a branch and publishes a `Beta` pre-release.
- `Release and Update` runs manually from the Actions tab and publishes a `Release`.

## Disclaimer

Player Monitor collects system information and can perform remote actions on connected
clients. Use it only on servers that you own or administer and where players have been
informed. You are responsible for complying with the laws of your jurisdiction.
