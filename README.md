# 🪨 AnarhyStones

A high-performance, block-based land protection plugin for Minecraft servers. Built to work seamlessly with WorldGuard, it allows players to protect their builds simply by placing a specific block.

---

## ✨ Features

- 🧱 **Custom Protection Blocks:**  
  Create unlimited tiers of stones via `.toml` configuration files.

- 👁️ **Visual Holograms:**  
  Automatic armor-stand holograms displaying owner names and region radius.

- 🛡️ **WorldGuard Integration:**  
  Automatically applies pre-configured flags (PVP, TNT, etc.) to newly created regions.

- 💥 **Anti-Grief Physics:**  
  Protection blocks are immune to piston movement and explosion damage.

- 🏠 **Teleportation System:**  
  Includes `/as home` and `/as sethome` with configurable warm-up delays.

- 📈 **Dynamic Limits:**  
  Set region limits per player or rank using permission nodes like:
  ```text
  anarhystones.limit.5
  ```

- 🌐 **Multilingual:**  
  Native support for English, Ukrainian, and Russian.

---

## 📋 Commands

| Command | Description |
|---|---|
| `/as get` | Obtain a protection block |
| `/as toggle` | Enable or disable region creation on block placement |
| `/as info` | View details of the region you are standing in |
| `/as home` | Teleport to your region's home |
| `/as sethome` | Set a teleport point for the region |
| `/as add` | Add a member to your region |
| `/as remove` | Remove a member from your region |
| `/as count` | List all your owned regions |
| `/as reload` | Reload plugin configurations *(Admin)* |

---

## 🛠 Installation

### 1. Install Dependencies

Make sure your server already has:

- WorldEdit
- WorldGuard

---

### 2. Install the Plugin

Drop `AnarhyStones.jar` into your server's `plugins` folder.

---

### 3. Restart the Server

Restart or reload your Minecraft server.

---

### 4. Configure Your Blocks

Edit your custom protection blocks inside:

```text
/plugins/AnarhyStones/blocks/
```

---

## ⚙️ Permissions

| Permission | Description |
|---|---|
| `anarhystones.limit.X` | Allows a player to own up to X regions |
| `anarhystones.admin` | Access to admin commands |

---

## 🌍 Supported Languages

- 🇺🇸 English
- 🇺🇦 Ukrainian
- 🇷🇺 Russian

---

## 📄 License

Distributed under the MIT License.

See the `LICENSE` file for more information.
