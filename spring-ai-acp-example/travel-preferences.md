# Travel preferences

This file is an example resource for the Travel ACP agent. It does **not** get
picked up automatically — you attach it per prompt from your ACP client (in
Zed: `@travel-preferences.md` in the chat input, or drag & drop the file).
When you attach it, the agent reads it via ACP's `fs/read_text_file` and uses
the contents as personalisation context for that prompt.

Edit this file, ship your own, or reuse the same pattern for any other context
(packing lists, saved itineraries, hotel confirmations, etc.).

- **Home base**: Berlin, Germany
- **Budget**: EUR 150 per person per day (mid-range)
- **Diet**: vegetarian
- **Interests**: architecture, hiking, coffee culture, jazz
- **Transport**: prefer trains over flights when travel time is under 8 hours
- **Accommodation**: mid-range boutique hotels, avoid large chains
- **Pace**: 2-3 activities per day with generous free time in between
- **Languages spoken**: English, German
