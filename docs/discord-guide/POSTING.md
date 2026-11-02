# Posting the player guide on Discord

Use the numbered `.md` files in this folder as separate messages, in order.
Each file contains only the message to copy, including its section heading.
The full, continuous guide is `../PLAYER_GUIDE.md` for reading or downloading.

## How to post

1. Open a numbered file in a text editor and copy its complete contents.
2. Paste it into the intended Discord channel, thread or forum discussion.
3. Send that one message, then repeat with the next numbered file.
4. Pin the first message or share the thread so players can find the guide.

Copy the actual file text, not a rendered webpage selection that loses the
Markdown characters. Do not wrap a whole message in a code block: its headings,
bold text and lists should render normally. Command examples already have their
own complete code fences. Do not post this instruction file or the JSON manifest
as part of the player guide.

## Format and size

The export uses headings, bold text, bullet lists, inline code and closed code
blocks. It does not use Markdown tables, local file links, footnotes or internal
heading-anchor links. Each message is checked against a 1,900-character budget
with room below the standard 2,000-character limit; Nitro is not required.
See `message-sizes.json` for the measured UTF-16 count of each file.

Uploading the entire `.md` as an attachment provides a downloadable file. To make
the guide readable as formatted chat messages, paste the numbered files instead.

Discord's official references:

- [Markdown formatting](https://support.discord.com/hc/en-us/articles/210298617-Markdown-Text-101-Chat-Formatting-Bold-Italic-Underline)
- [Sending messages and message length](https://support.discord.com/hc/en-us/articles/360034632292-Sending-Messages)

## Updating the guide

Edit `docs/PLAYER_GUIDE.md`, then run `python tools/export_discord_guide.py` from
the repository root. The exporter stops if a section exceeds its size budget or
contains an unclosed code fence. Review and repost any messages you change.
