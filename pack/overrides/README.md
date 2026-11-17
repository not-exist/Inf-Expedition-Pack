# Pack overrides

This directory is copied into the root of the CurseForge pack.

- `config/` — mod configs.
- `kubejs/` — server/client scripts. Room, encounter and structure data ship in
  the `inf_expedition` jar so the mod is self-contained.
- `ftbquests/` — the 《锻途指南》 quest chapter.
- `fancymenu/` — title and loading screen assets.

Third-party jars are not stored here. `tools/build_pack.py --local` copies the cached
jars and the two local jars into a playable directory; `tools/build_mrpack.py` builds the
installable `.mrpack` (Modrinth format).
