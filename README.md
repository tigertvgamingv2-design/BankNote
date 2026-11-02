# 💰 Bank Notes

A Spigot/Paper plugin that turns in-game money into physical, redeemable bank notes and money pouches.

## ✨ Features

- 💰 **Physical banknote items** - `/withdraw <amount>` converts money into a note showing who issued it
- 🖥️ **In-game banknote editor** - create, edit and delete note designs from a GUI
- 🖱️ **Right-click redemption** - each note can be redeemed exactly once (unique IDs stop duplication exploits)
- 👝 **Money pouches (T1-T4)** - right-click for a random payout in a configurable range
- 📊 **Live balance viewer** - `/bnbalance` menu that updates in real time
- 🔊 **Sound & title feedback** - configurable for withdraw, redeem, pouch and error events
- 📦 **Automatic config saving** - new default options are added to your config on startup
- 🔗 **Vault & EssentialsX compatible**
- 📈 **PlaceholderAPI support**
- 🎨 **Colorful console logging**

## 📋 Requirements

- Java 25
- Spigot or Paper 1.21+
- [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin (e.g. EssentialsX)
- A valid license key
- Optional: PlaceholderAPI

## 🚀 Installation

1. Drop `BankNotes-<version>.jar` into your server's `plugins/` folder.
2. Start the server once to generate `plugins/BankNotes/config.yml`.
3. Set `license.key` in `config.yml`, then restart.

## ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/withdraw <amount>` | Withdraw money into a bank note (min $1) | `banknotes.withdraw` (default: everyone) |
| `/bnbalance` (`/notebalance`) | Open the live balance menu | `banknotes.balance` (default: everyone) |
| `/bnadmin create <amount> [player] [template]` | Create a nameless note (no issuer, no cost) | `banknotes.admin` (default: op) |
| `/bnadmin give <player> <amount>` | Give a note issued under a player's name | `banknotes.admin` |
| `/bnadmin pouch <tier> [player] [count]` | Give money pouches | `banknotes.admin` |
| `/bnadmin editor` | Open the banknote editor | `banknotes.admin` |
| `/bnadmin check` | Inspect the note in your hand | `banknotes.admin` |
| `/bnadmin reload` | Reload the config | `banknotes.admin` |

## 👝 Money Pouches

| Tier | Item | Payout |
|---|---|---|
| T1 | Leather | $10 - $100 |
| T2 | Rabbit hide | $500 - $10,000 |
| T3 | Heart of the sea | $25,000 - $100,000 |
| T4 | Nether star | $200,000 - $1,000,000 |

Tiers are defined under `pouches:` in `config.yml` - edit the ranges or add your own.

## 🖥️ Banknote Editor

Run `/bnadmin editor` to manage note designs. You can change the name, lore, material and glow of each design, create new ones, take a test note, or shift-click to delete.
Names and lore support `&` color codes, `#RRGGBB` hex colors, and the placeholders `{amount}`, `{player}` and `{date}`.
Designs are saved to `plugins/BankNotes/templates.yml`.

## 📈 PlaceholderAPI

| Placeholder | Description |
|---|---|
| `%banknotes_balance%` | Player's formatted balance |
| `%banknotes_balance_raw%` | Player's raw balance |
| `%banknotes_redeemed_total%` | Total notes/pouches redeemed |
| `%banknotes_templates%` | Number of banknote designs |

## 🔒 Duplication Protection

Every note and pouch carries a unique ID. When one is redeemed its ID is saved to `plugins/BankNotes/redeemed.txt`, so a duplicated copy is worthless and persists across restarts.

## 🛠️ Building

```
mvn clean package
```

The jar is written to `target/`.

## 📄 License

&copy; CDM Studios. All rights reserved.
